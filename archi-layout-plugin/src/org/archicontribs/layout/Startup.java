package org.archicontribs.layout;

import org.eclipse.elk.core.service.ElkServicePlugin;
import org.eclipse.ui.IStartup;

public class Startup implements IStartup{

	
	@Override
	public void earlyStartup() {
		//Force ELK Plugin loading as adding an start level 0 in archi.product does not seem to work.. 
		ElkServicePlugin.getInstance();

		
		//IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		//handlerService.activateHandler("org.eclipse.elk.core.ui.command.layout", new LayoutHandler());
	}

}
