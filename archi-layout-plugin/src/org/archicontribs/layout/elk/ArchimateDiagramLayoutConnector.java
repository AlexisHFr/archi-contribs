package org.archicontribs.layout.elk;

import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Handler;

import org.eclipse.elk.core.LayoutConfigurator;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.service.DiagramLayoutEngine;
import org.eclipse.elk.core.service.IDiagramLayoutConnector;
import org.eclipse.elk.core.service.LayoutConfigurationManager;
import org.eclipse.elk.core.service.LayoutMapping;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.properties.IPropertyHolder;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

import com.archimatetool.editor.diagram.ArchimateDiagramEditor;
import com.archimatetool.editor.diagram.IArchimateDiagramEditor;
import com.archimatetool.editor.diagram.editparts.AbstractBaseEditPart;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.google.common.collect.BiMap;
import com.google.inject.Inject;

public class ArchimateDiagramLayoutConnector implements IDiagramLayoutConnector {
	// inspiration avec la version GMF :
	// https://github.com/eclipse-elk/elk/blob/master/plugins/org.eclipse.elk.conn.gmf/src/org/eclipse/elk/conn/gmf/GmfDiagramLayoutConnector.java

	@Inject
	private LayoutConfigurationManager configurationManager;

	@Override
	public LayoutMapping buildLayoutGraph(IWorkbenchPart workbenchPart, Object diagramPart) {

		ArchimateDiagramEditor diagramEditor = (ArchimateDiagramEditor) workbenchPart;
		IArchimateDiagramModel model = diagramEditor.getModel();

		// Get or create a LayoutMapping for this diagram
		LayoutMapping previousLayoutMapping = (LayoutMapping) model.getAdapter(LayoutMapping.class);

		// 1st run initiaite the graph, user set options, then a new run is triggered ..
		// => existing graphNode should be kept
		LayoutMapping layoutMapping = new LayoutMapping(workbenchPart);
		// attach a reference to the mapping/graph in the model itself, as an Adapter :
		model.setAdapter(LayoutMapping.class, layoutMapping);
		layoutMapping.setParentElement(diagramEditor);
		ElkNode rootElkNode = ElkGraphUtil.createGraph();
		rootElkNode.setDimensions(1000, 1000); // FIXME: fetch Archimate Diagram max widht/height from constants..
		layoutMapping.setLayoutGraph(rootElkNode);
		layoutMapping.getGraphMap().put(rootElkNode, model); // TODO: not sure for this one..
		

		// 1st create all 'nodes'
		EList<IDiagramModelObject> children = model.getChildren();
		for (Iterator<IDiagramModelObject> iter = children.iterator(); iter.hasNext();) {
			IDiagramModelObject childDmo = iter.next();
			populateLayoutGraphRecursively(layoutMapping, rootElkNode, childDmo, previousLayoutMapping);
		}

		//TODO : then add 'edges' :
		
		return layoutMapping;
	}

	/**
	 * Translate an ArchimateDiagramEditor to a Graph of {@link ElkNode} within a
	 * {@link LayoutMapping}.
	 * 
	 * <br>
	 * <a href=
	 * "https://eclipse.dev/elk/documentation/tooldevelopers/graphdatastructure.html}">ELK
	 * Graph Data Structure</a>
	 * 
	 * Mapping (Archi<=> ELK> :
	 * <li>IDiagramModel => Root Node
	 * <li>IDiagramModelComponent => Simple Node (si pas Container && getChildren()
	 * > 0 )
	 * <li>IDiagramModelArchimateObject => Hierarchical Node (si getChildren() == 0
	 * )
	 * <li>IDiagramModelContainer => Hierarchical Node
	 * <li>IDiagramModelGroup => Hierarchical Node
	 * <li>IDiagramModelConnection => edge
	 * <li>IDiagramModelBendpoint ?=> ElkEdgeSection <br>
	 * 
	 * @param diagramEditor
	 * @param diagModelCpn
	 * @param previousLayoutMapping 
	 * @return
	 */
	public void populateLayoutGraphRecursively(LayoutMapping mapping, ElkNode parentElkNode,
			IDiagramModelComponent diagModelCpn, LayoutMapping previousLayoutMapping) {

		ElkNode childElkNode = null;
		if (previousLayoutMapping != null) {
			childElkNode = (ElkNode) previousLayoutMapping.getGraphMap().inverse().get(diagModelCpn);
		}
		
		if (childElkNode == null) {
			childElkNode =  ElkGraphUtil.createNode(parentElkNode);
			diagModelCpn.setAdapter(LayoutMapping.class, mapping); // usage ?!
		}
		

		if (diagModelCpn instanceof IDiagramModelArchimateObject) {
			IDiagramModelArchimateObject dmo = (IDiagramModelArchimateObject) diagModelCpn;
			IBounds bounds = dmo.getBounds();
			childElkNode.setDimensions(bounds.getWidth(), bounds.getHeight());
			childElkNode.setLocation(bounds.getX(), bounds.getY());

			parentElkNode.getChildren().add(childElkNode);
			mapping.getGraphMap().put(childElkNode, dmo);
		}

		if (diagModelCpn instanceof IDiagramModelContainer) {
			IDiagramModelContainer dmc = (IDiagramModelContainer) diagModelCpn;

			EList<IDiagramModelObject> children = dmc.getChildren();
			for (Iterator<IDiagramModelObject> iter = children.iterator(); iter.hasNext();) {
				IDiagramModelObject siblingDiagObj = iter.next();
				//
				populateLayoutGraphRecursively(mapping, childElkNode, siblingDiagObj, previousLayoutMapping);
			}
		}
	}

	@Override
	public void applyLayout(LayoutMapping mapping, IPropertyHolder settings) {
		ElkNode layoutGraph = mapping.getLayoutGraph();
		for (Entry<ElkGraphElement, Object> entry : mapping.getGraphMap().entrySet()) {
			Object value = entry.getValue();
			if (value instanceof IDiagramModelArchimateObject) {
				applyLayoutForElement((ElkNode) entry.getKey(), (IDiagramModelArchimateObject) value);

			} else if (value instanceof ArchimateDiagramEditor) {

			}

		}
	}

	private void applyLayoutForElement(ElkNode node, IDiagramModelArchimateObject admObject) {
		// 1st try : it works !!
		// FIXME : undo management !!
		admObject.setBounds((int) node.getX(), (int) node.getY(), (int) node.getWidth(), (int) node.getHeight());
	}

}
