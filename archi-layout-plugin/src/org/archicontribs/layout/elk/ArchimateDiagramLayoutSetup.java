package org.archicontribs.layout.elk;

import java.util.Collection;

import org.eclipse.elk.core.service.IDiagramLayoutConnector;
import org.eclipse.elk.core.service.ILayoutConfigurationStore;
import org.eclipse.elk.core.service.ILayoutSetup;

import com.archimatetool.editor.diagram.ArchimateDiagramEditor;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

public class ArchimateDiagramLayoutSetup implements ILayoutSetup {

	
	public ArchimateDiagramLayoutSetup() {
		super();
	}

	@Override
	public boolean supports(Object object) {

		 // This method may be invoked on a whole collection of elements selected
	    // in an editor
	    if (object instanceof Collection) {
	        // Check if we support layout on at least one of the selected objects
	        for (Object o : (Collection<?>) object) {
	            if (o instanceof IDiagramModelArchimateObject) {
	                return true;
	            }
	        }
	        return false;
	    }

	    // If it is not a collection, it may be either a workbench part we support
	    // or the diagram element class we already checked for above
		return object instanceof ArchimateDiagramEditor 
				|| object instanceof IDiagramModelArchimateObject;
	}

	@Override
	public Injector createInjector(final Module defaultModule) {
		// Modules basically provide a mapping between types and implementations
		// to instantiate whenever an instance of the type is requested. We use
		// the default module supplied by ELK and override that with custom
		// overrides to get our IDiagramLayoutConnector to enter the picture.
		return Guice.createInjector(Modules.override(defaultModule).with(new DiagramLayoutModule()));
	}

	public static class DiagramLayoutModule implements Module {
		@Override
		public void configure(final Binder binder) {
			// This is the most important binding
			binder.bind(IDiagramLayoutConnector.class).to(ArchimateDiagramLayoutConnector.class);
			binder.bind(ILayoutConfigurationStore.Provider.class).to(ArchimateDiagramLayoutConfigurationStore.Provider.class);
		}
	}

}
