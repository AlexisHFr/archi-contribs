package org.archicontribs.layout.elk;

import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.elk.core.service.IDiagramLayoutConnector;
import org.eclipse.elk.core.service.LayoutMapping;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.properties.IPropertyHolder;
import org.eclipse.elk.graph.util.ElkGraphUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.ui.IWorkbenchPart;

import com.archimatetool.editor.diagram.ArchimateDiagramEditor;
import com.archimatetool.editor.diagram.commands.CreateBendpointCommand;
import com.archimatetool.editor.model.DiagramModelUtils;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelBendpoint;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;

public class ArchimateDiagramLayoutConnector implements IDiagramLayoutConnector {
	// inspiration avec la version GMF :
	// https://github.com/eclipse-elk/elk/blob/master/plugins/org.eclipse.elk.conn.gmf/src/org/eclipse/elk/conn/gmf/GmfDiagramLayoutConnector.java

	@Override
	public LayoutMapping buildLayoutGraph(IWorkbenchPart workbenchPart, Object diagramPart) {

		ArchimateDiagramEditor diagramEditor = (ArchimateDiagramEditor) workbenchPart;

		IArchimateDiagramModel model = diagramEditor.getModel();
		// get the previous LayoutMapping instance, if any :
		LayoutMapping previousLayoutMapping = (LayoutMapping) model.getAdapter(LayoutMapping.class);

		// Create a fresh/new LayoutMapping for this pass :
		LayoutMapping layoutMapping = new LayoutMapping(workbenchPart);

		// attach it to the model, as an Adapter :
		model.setAdapter(LayoutMapping.class, layoutMapping);
		layoutMapping.setParentElement(diagramEditor);

		// (re-)create the ELK-Graph
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

		// Then add 'edges' :
		for (Iterator<IDiagramModelObject> iter1 = children.iterator(); iter1.hasNext();) {
			IDiagramModelObject childDmo = iter1.next();
			EList<IDiagramModelConnection> sourceConnections = childDmo.getSourceConnections();
			for (Iterator<IDiagramModelConnection> iter2 = sourceConnections.iterator(); iter2.hasNext();) {
				IDiagramModelConnection dmc =  iter2.next();
				createEdge(layoutMapping, dmc);
			}
		}
		
		// Ports  <= ConnectionAnchor ? 
		return layoutMapping;
	}

	
	private void createEdge(LayoutMapping mapping, IDiagramModelConnection diagModelCpn) {
		IDiagramModelConnection modelConnection = (IDiagramModelConnection) diagModelCpn;
		ElkNode elkSourcNode = (ElkNode) mapping.getGraphMap().inverse().get(modelConnection.getSource());
		ElkNode elkTargetNode = (ElkNode) mapping.getGraphMap().inverse().get(modelConnection.getTarget());
		// TODO: manage edgeSection <== bendPoints
		ElkEdge elkEdge = ElkGraphUtil.createSimpleEdge(elkSourcNode, elkTargetNode);
		mapping.getGraphMap().put(elkEdge, modelConnection);
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
		
		//FIXME : why this ? why here ?
		diagModelCpn.setAdapter(LayoutMapping.class, mapping); // usage ?!

		if (diagModelCpn instanceof IDiagramModelArchimateObject) {
			childElkNode = ElkGraphUtil.createNode(parentElkNode);
			IDiagramModelArchimateObject dmo = (IDiagramModelArchimateObject) diagModelCpn;
			IBounds bounds = dmo.getBounds();
			childElkNode.setDimensions(bounds.getWidth(), bounds.getHeight());
			childElkNode.setLocation(bounds.getX(), bounds.getY());

			parentElkNode.getChildren().add(childElkNode);
			mapping.getGraphMap().put(childElkNode, dmo);

		}
		// ArchimateObject, Groups, Canvas, .. can have other nodes embedded :
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
		
		for (Entry<ElkGraphElement, Object> entry : mapping.getGraphMap().entrySet()) {
			Object value = entry.getValue();
			if (value instanceof IDiagramModelArchimateObject) {
				applyLayoutForElement((ElkNode) entry.getKey(), (IDiagramModelArchimateObject) value);

			} else if (value instanceof IDiagramModelArchimateConnection) {
				applyLayoutForConnection((ElkEdge) entry.getKey(), (IDiagramModelArchimateConnection) value);
			}

		}
	}


	private void applyLayoutForElement(ElkNode node, IDiagramModelArchimateObject dmo) {
		// FIXME : undo management needed =>  use 'Commands' ?!
		dmo.setBounds((int) node.getX(), (int) node.getY(), (int) node.getWidth(), (int) node.getHeight());
	}


	private void applyLayoutForConnection(ElkEdge edge, IDiagramModelArchimateConnection dmc) {
		
		dmc.getBendpoints().clear();
		
		for (ElkEdgeSection edgeSection : edge.getSections()) {
			
			for (int i = 0; i < edgeSection.getBendPoints().size(); i++) {
				ElkBendPoint elkBendPoint = edgeSection.getBendPoints().get(i);
		        
				int xPos = (int) elkBendPoint.getX();
				int yPos = (int) elkBendPoint.getY();
				
		        IDiagramModelBendpoint dmb = DiagramModelUtils.createBendPointFromAbsolutePosition(dmc, xPos, yPos);
				dmc.getBendpoints().add(dmb);
			}
		}
	}
	
	
}
