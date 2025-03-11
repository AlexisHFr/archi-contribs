package org.archicontribs.layout.menu;

import org.eclipse.ui.internal.ShowViewMenu;

public class ShowViewContributionItem extends ShowViewMenu {

	public ShowViewContributionItem() {
		this("org.archicontribs.layout.menu.showViewId");
	}
	
	public ShowViewContributionItem(String id) {
		super(org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow(), id);
	}

}
