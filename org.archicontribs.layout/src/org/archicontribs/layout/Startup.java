package org.archicontribs.layout;

import org.eclipse.elk.core.service.ElkServicePlugin;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ContributionItemFactory;

public class Startup implements IStartup{

	
	@Override
	public void earlyStartup() {
		//Force ELK Plugin loading as adding an start level 0 in archi.product does not seem to work.. 
		ElkServicePlugin.getInstance();

		
		//add a Window -> Show Views menu to give access to ELK debug views..
		IWorkbench workbench = PlatformUI.getWorkbench();
		 workbench.getDisplay().asyncExec(new Runnable() {
		   public void run() {
		     IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		     if (window != null) {
		 		IContributionItem viewList = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

				MenuManager windowMenu = new MenuManager("&Project", IWorkbenchActionConstants.M_WINDOW);
				MenuManager viewMenu = new MenuManager("Show &view");
				viewMenu.add(viewList);
				windowMenu.add(viewMenu);
		     } else {
		    	 System.err.println("No getActiveWorkbenchWindow ?! ");
		     }
		   }
		 });

		
		//IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		//handlerService.activateHandler("org.eclipse.elk.core.ui.command.layout", new LayoutHandler());
	}

}
