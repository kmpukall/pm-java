/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob;

import java.util.Properties;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Autofrob implements IApplication, BundleActivator {
	// The plug-in ID
	public static final String PLUGIN_ID = "Autofrob";

	// The shared instance
	private static Autofrob plugin;
	
	/**
	 * The constructor
	 */
	public Autofrob() {
	}

	/*
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public Object start(IApplicationContext ctx) throws Exception {
		plugin = this;
		
        System.err.println("[plugin] Autofrob start");
        String args[] = (String [])ctx.getArguments().get("application.args");
        
        Properties p = System.getProperties();
        String instance_area = (String) p.getProperty("osgi.instance.area");
        String install_area = (String) p.getProperty("osgi.install.area");
        if (instance_area.startsWith("file:"))
                instance_area = instance_area.substring(5);
        if (install_area.startsWith("file:"))
                install_area = install_area.substring(5);
 
        if (args.length < 1)
                        System.err.println("Error:  Must pass class to invoke (subclass of autofrob.FrobInterface)");
        else try {
                        FrobInterface frobber = (FrobInterface) (Class.forName(args[0])).newInstance();
                        frobber.run(this, ctx, args);
        } catch (Exception e) {
                System.err.println("While trying to instantiate " + args[0] + ":");
                e.printStackTrace(System.err);
        }
        
        return null;
		
	}

	/*
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop() {
		plugin = null;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Autofrob getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		System.err.println("[plugin] activator start");
		
	}

	@Override
	public void stop(BundleContext context) throws Exception {
 		System.err.println("[plugin] activator stop");
	}

}
