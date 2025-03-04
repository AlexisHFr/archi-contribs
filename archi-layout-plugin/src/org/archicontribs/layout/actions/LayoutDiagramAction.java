package org.archicontribs.layout.actions;

import org.eclipse.elk.core.LayoutConfigurator;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.service.DiagramLayoutEngine;
import org.eclipse.elk.core.service.LayoutConfigurationManager;
import org.eclipse.elk.core.service.LayoutMapping;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.gef.Disposable;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;

import com.archimatetool.editor.diagram.ArchimateDiagramEditor;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IDiagramModel;

public class LayoutDiagramAction extends SelectionAction implements Disposable {

	public static final String ID = "com.archimatetool.editor.action.layoutDiagram"; //$NON-NLS-1$
	public static final String TEXT = "Layout";

	private IArchimateDiagramModel diagramModel;

	public LayoutDiagramAction(IWorkbenchPart part) {
		super(part);
		setId(ID);
		setText(TEXT);

		diagramModel = (IArchimateDiagramModel) part.getAdapter(IDiagramModel.class);
		diagramModel.eAdapters().add(eAdapter);
		// layout();
	}

	/*
	 * Adapter to listen to change from Model
	 */
	private Adapter eAdapter = new AdapterImpl() {
		@Override
		public void notifyChanged(Notification msg) {
			Object feature = msg.getFeature();
			/*
			 * if(feature == IArchimatePackage.Literals.ARCHIMATE_DIAGRAM_MODEL__VIEWPOINT)
			 * { layout(); }
			 */
		}
	};

	@Override
	public void run() {
		layout();
	}

	protected void layout() {


	}

	@Override
	public void dispose() {
		diagramModel.eAdapters().remove(eAdapter);
		diagramModel = null;
	}

	@Override
	protected boolean calculateEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

}
