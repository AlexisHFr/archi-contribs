package org.archicontribs.layout.elk;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.gef.EditPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.archimatetool.editor.diagram.ArchimateDiagramEditor;
import com.archimatetool.editor.diagram.editparts.ArchimateDiagramEditPartFactory;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.impl.DiagramModelArchimateObject;

public class ArchimateDiagramLayoutSetupTests {

	private ArchimateDiagramLayoutSetup setup;
    
    
    @BeforeEach
    public void runOnceBeforeEachTest() {
        setup = new ArchimateDiagramLayoutSetup();
    }
	
	
	@Test
	public void testSupportsDiagamModel() {
	    assertTrue(setup.supports(IArchimateFactory.eINSTANCE.createDiagramModelArchimateObject()));
	}

}
