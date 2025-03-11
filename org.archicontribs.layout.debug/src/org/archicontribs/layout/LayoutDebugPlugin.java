/*******************************************************************************
 * Copyright (c) 2016, 2019 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.archicontribs.layout;

import org.archicontribs.layout.debug.model.ExecutionInfo;
import org.archicontribs.layout.debug.model.ExecutionInfoModel;
import org.eclipse.elk.core.service.ElkServicePlugin;
import org.eclipse.elk.core.service.ILayoutListener;
import org.eclipse.elk.core.service.LayoutConnectorsService;
import org.eclipse.elk.core.service.LayoutMapping;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class LayoutDebugPlugin extends AbstractUIPlugin {

	/** The plug-in ID. */
	public static final String PLUGIN_ID = "org.archicontribs.layout"; //$NON-NLS-1$
	/** The shared instance. */
	private static LayoutDebugPlugin plugin;
	
	/** Our central execution info model that feeds our debug views. */
	private ExecutionInfoModel model = new ExecutionInfoModel();
	/** The layout listener we will be using to update the view we are contributing. */
	private ILayoutListener layoutListener = new ILayoutListener() {
        @Override
        public void layoutAboutToStart(final LayoutMapping mapping, final IElkProgressMonitor progressMonitor) {
        }
        
        @Override
        public void layoutDone(final LayoutMapping mapping, final IElkProgressMonitor progressMonitor) {
            model.addExecution(ExecutionInfo.fromProgressMonitor(progressMonitor));
        }
    };
	
    @Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

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
		
        LayoutConnectorsService.getInstance().addLayoutListener(layoutListener);
	}
    
    @Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		
		LayoutConnectorsService.getInstance().removeLayoutListener(layoutListener);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static LayoutDebugPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns our central execution info model.
	 */
	public ExecutionInfoModel getModel() {
	    return model;
	}

}
