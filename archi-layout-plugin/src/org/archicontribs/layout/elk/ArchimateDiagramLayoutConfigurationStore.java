/*
 * 
 */
package org.archicontribs.layout.elk;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.elk.core.LayoutConfigurator;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.data.LayoutOptionData;
import org.eclipse.elk.core.data.LayoutOptionData.Target;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.service.ILayoutConfigurationStore;
import org.eclipse.elk.core.service.LayoutConfigurationManager;
import org.eclipse.elk.core.service.LayoutMapping;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.properties.IPropertyValueProxy;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.ui.IWorkbenchPart;

import com.archimatetool.editor.diagram.ArchimateDiagramEditor;
import com.archimatetool.editor.diagram.editparts.AbstractBaseEditPart;
import com.archimatetool.editor.diagram.editparts.ArchimateDiagramPart;
import com.archimatetool.model.IAdapter;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelComponent;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelNote;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.impl.ArchimateDiagramModel;
import com.google.inject.Inject;

/**
 * A layout configuration which derives layout options from properties attached
 * to layout data of graph elements.
 *
 */
public class ArchimateDiagramLayoutConfigurationStore implements ILayoutConfigurationStore {

	/**
	 * Provider for ArchimateDiagram layout configuration stores.
	 */
	public static final class Provider implements ILayoutConfigurationStore.Provider {

		/**
		 * The layout configuration manager used to handle configuration stores.
		 */
		@Inject
		private LayoutConfigurationManager configManager;

		@Override
		public ILayoutConfigurationStore get(final IWorkbenchPart workbenchPart, final Object context) {
			// we're only interested in IDiagramModelObject associated 'xxxEditPart' =>
			// ABstractBaseEditPart seems a good pick .. ?!

			if (context instanceof AbstractBaseEditPart) {

				AbstractBaseEditPart baseEditPart = (AbstractBaseEditPart) context;
				IDiagramModelComponent dmc = (IDiagramModelComponent) baseEditPart.getModel();

				// retrieve or create&attach the ILayoutConfigurator to the model object :
				ArchimateDiagramLayoutConfigurationStore configStore = (ArchimateDiagramLayoutConfigurationStore) dmc
						.getAdapter(ILayoutConfigurationStore.class);
				if (configStore == null || configStore.getContext() != dmc) {
					configStore = new ArchimateDiagramLayoutConfigurationStore(workbenchPart, (EditPart) context, dmc);
					dmc.setAdapter(ILayoutConfigurationStore.class, configStore);
				}
				configStore.fixEditPart(baseEditPart);
				
				return configStore;

			} else if (context instanceof ArchimateDiagramModel) {

				ArchimateDiagramModel model = (ArchimateDiagramModel) context;
				// FIXME : hack to pass LCM instance to LayoutDiagramAction ..
				if (model.getAdapter(LayoutConfigurationManager.class) == null) {
					model.setAdapter(LayoutConfigurationManager.class, configManager);
				}

				ArchimateDiagramLayoutConfigurationStore configStore = (ArchimateDiagramLayoutConfigurationStore) model
						.getAdapter(ILayoutConfigurationStore.class);
				if (configStore == null || configStore.getContext() != model) {
					configStore = new ArchimateDiagramLayoutConfigurationStore(workbenchPart, null, model);
					
					configStore.setOptionValue( CoreOptions.ALGORITHM.getId() , "org.eclipse.elk.box");
					configStore.setOptionValue( CoreOptions.SPACING_NODE_NODE.getId(), "10.0");
					
					model.setAdapter(ILayoutConfigurationStore.class, configStore);
				}

				return configStore;

			} else if (context instanceof IDiagramModelComponent) {

				IDiagramModelComponent dmc = (IDiagramModelComponent) context;

				// retrieve or create&attach the ILayoutConfigurator to the model object :
				ArchimateDiagramLayoutConfigurationStore configStore = (ArchimateDiagramLayoutConfigurationStore) dmc
						.getAdapter(ILayoutConfigurationStore.class);
				if (configStore == null || configStore.getContext() != dmc) {
					//FIXME : normalement on ne doit jamais passé ici, les AbstractBaseEditPart ont setté l'adapter avant ..
					configStore = new ArchimateDiagramLayoutConfigurationStore(workbenchPart, null, dmc);
					dmc.setAdapter(ILayoutConfigurationStore.class, configStore);
				}
				return configStore;

			} else if (context instanceof ArchimateDiagramPart) {
				ArchimateDiagramPart diagramPart = (ArchimateDiagramPart) context;
				IArchimateDiagramModel model = diagramPart.getModel();
				// retrieve or create&attach the ILayoutConfigurator to the model object :
				ArchimateDiagramLayoutConfigurationStore configStore = (ArchimateDiagramLayoutConfigurationStore) model
						.getAdapter(ILayoutConfigurationStore.class);
				if (configStore == null || configStore.getContext() != model) {
					//FIXME : normalement on ne doit jamais passé ici, les AbstractBaseEditPart ont setté l'adapter avant ..
					configStore = new ArchimateDiagramLayoutConfigurationStore(workbenchPart, diagramPart, model);
					model.setAdapter(ILayoutConfigurationStore.class, configStore);
				}
				return configStore;
				
			} else {
				return null;
			}
		}
	}

	/** The {@link IWorkbenchPart} attached to this context. */
	private final IWorkbenchPart workbenchPart;

	/** *The view ui EditPart component */
	private EditPart editPart;

	/** The view model part used as context for this configuration store. */
	private final IDiagramModelComponent graphElement;

	/**
	 * Create a ArchimateDiagramLayout layout configuration store.
	 * 
	 * @param workbenchPart The {@link IWorkbenchPart} attached to this context.
	 * @param uiEditPart
	 * @param model         The {@link KGraphElement} of the view model this
	 *                      {@link ILayoutConfigurationStore} is attached to.
	 */
	public ArchimateDiagramLayoutConfigurationStore(final IWorkbenchPart workbenchPart, EditPart uiEditPart,
			final IDiagramModelComponent model) {
		this.workbenchPart = workbenchPart;
		this.editPart = uiEditPart;
		this.graphElement = model;

	}

	public void fixEditPart(AbstractBaseEditPart baseEditPart) {
		if (editPart == null) {
			editPart = baseEditPart;
		} else if (editPart != baseEditPart) {
			throw new IllegalStateException("Tentative to fix a corrupted editPart ! " + baseEditPart + " != " + editPart);
		}
		
	}

	public IDiagramModelComponent getContext() {
		return graphElement;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getOptionValue(final String optionId) {
		LayoutOptionData optionData = LayoutMetaDataService.getInstance().getOptionData(optionId);

		if (optionData == null || graphElement == null) {
			return null;
		}

		Properties properties = getProperties();
		final Object value = properties != null ? properties.get(optionId) : null;

		if (value instanceof IPropertyValueProxy) {
			return ((IPropertyValueProxy) value).resolveValue(optionData);
		} else if (value == null) {
			return getDefaultOptionValue(optionData);
		} else {
			return value;
		}
	}

	private Properties getProperties() {
		return (Properties) graphElement.getAdapter(Properties.class);
	}

	/** The aspect ratio is rounded to two decimal places. */
	// private static final double ASPECT_RATIO_ROUND = 100;

	private Object getDefaultOptionValue(final LayoutOptionData optionData) {
		switch (optionData.getId()) {
		case "org.eclipse.elk.algorithm":
			return "org.eclipse.elk.box";
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setOptionValue(final String optionId, final String valueString) {
		// LayoutOptionData optionData =
		// LayoutMetaDataService.getInstance().getOptionData(optionId);
		// Object value = optionData.parseValue(valueString);

		if (graphElement != null) {
			Properties propAdapater = getProperties();
			if (propAdapater == null) {
				propAdapater = new Properties();
				graphElement.setAdapter(Properties.class, propAdapater);
			}

			propAdapater.setProperty(optionId, valueString);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<String> getAffectedOptions() {
		if (graphElement == null || getProperties() == null) {
			return Collections.emptyList();
		}

		final List<String> options = new LinkedList<String>();

		for (Object optionDataId : getProperties().keySet()) {
			options.add((String) optionDataId);
		}

		// handle special layout options
		/*
		 * if (getContainer() == null || isSingleNodeOnRootLevel()) {
		 * options.add(CoreOptions.ASPECT_RATIO.getId()); }
		 */

		return options;

	}

	/**
	 * {@inheritDoc}
	 */
	public EditingDomain getEditingDomain() {
		// MIGRATE What is our editing domain here?
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<Target> getOptionTargets() {
		if (graphElement instanceof IDiagramModel) {
			return EnumSet.of(Target.PARENTS);
		} else if (graphElement instanceof IDiagramModelArchimateConnection) {
			return EnumSet.of(Target.EDGES);
		} else if (graphElement instanceof IDiagramModelNote) {
			return EnumSet.of(Target.LABELS);
			// } else if (graphElement instanceof IDia) {
			// return EnumSet.of(Target.PORTS);
		} else if (graphElement instanceof IDiagramModelContainer) {
			if (((IDiagramModelContainer) graphElement).getChildren().isEmpty()) {
				return EnumSet.of(Target.NODES);
			} else {
				return EnumSet.of(Target.NODES, Target.PARENTS);
			}
		}

		return EnumSet.noneOf(LayoutOptionData.Target.class);
	}

	/**
	 * {@inheritDoc}
	 */
	public ILayoutConfigurationStore getParent() {
		/*
		 * KGraphElement container = getContainer(); if (container != null) { return new
		 * KlighdLayoutConfigurationStore(workbenchPart, container); } else {
		 */
		if (graphElement instanceof IArchimateDiagramModel) {
			// root of the UI hiearchy
			return null;
		} else {

			EditPart uiParent = getUIContainerParent();
			if (uiParent == null) {
				return null;
			}
			Object model = uiParent.getModel();
			if (model instanceof IDiagramModelObject || model instanceof IArchimateDiagramModel) {
				IAdapter adaptModel = (IAdapter) model;
				ILayoutConfigurationStore configStore = (ILayoutConfigurationStore) adaptModel
						.getAdapter(ILayoutConfigurationStore.class);
				if (configStore == null) {
					configStore = new ArchimateDiagramLayoutConfigurationStore(workbenchPart, uiParent,
							(IArchimateDiagramModel) model);
					adaptModel.setAdapter(ILayoutConfigurationStore.class, configStore);
				}

				return configStore;
			}

			throw new IllegalStateException("LayoutConfigurationStore graph is invalid !");
			/*
			 * LayoutMapping mapping = (LayoutMapping)
			 * model.getAdapter(LayoutMapping.class); if (mapping == null) { throw new
			 * IllegalStateException("model is not IAdapted with a LayoutMapping.. !?" +
			 * this); } ElkGraphElement elkGraphElement =
			 * mapping.getGraphMap().inverse().get(model);
			 */
		}
		// }
	}

	private EditPart getUIContainerParent() {
		// walk the uiContainers hierarchy until we found a ELK-supported graph Element
		if (editPart == null) {
			return null;
		}
		EditPart parent = editPart;
		EditPart current = null;
		do {
			current = parent;
			parent = current.getParent();
		} while (parent != null && !(parent.getModel() instanceof IArchimateDiagramModel)); // should call
																							// LayoutSetup.supports() ?!

		return parent; // null if no ancestor is a member of ELK graph..

	}

}