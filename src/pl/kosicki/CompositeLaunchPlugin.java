package pl.kosicki;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * The activator class controls the plug-in life cycle
 */
public class CompositeLaunchPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "pl.kosicki.CompositeLaunchPlugin"; //$NON-NLS-1$

	// Composite launch configuration tab image
	public static final String RUNDEBUG_IMAGE = "icons/rundebug.gif"; //$NON-NLS-1$

	// Composite launch configuration attribute name
	public static final String NESTED_CONFIGURATIONS = "nestedConfigurations"; //$NON-NLS-1$

	// Composite launch configuration type id
	public static final String COMPOSITE_CONF_TYPE_ID = "pl.kosicki.compositeLaunch"; //$NON-NLS-1$

	// The shared instance
	private static CompositeLaunchPlugin plugin;

	/**
	 * The constructor
	 */
	public CompositeLaunchPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		getImageRegistry().put(RUNDEBUG_IMAGE, imageDescriptorFromPlugin(PLUGIN_ID, RUNDEBUG_IMAGE));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static void setConfigurations(@Nonnull ILaunchConfigurationWorkingCopy configuration,
			@Nonnull Iterable<?> configurationName) {
		configuration.setAttribute(NESTED_CONFIGURATIONS, Joiner.on("@").join(configurationName));
	}

	/**
	 * @param configuration
	 *            composite launch configuration
	 * @param mode
	 *            "run" or "debug" constant or <code>null<code/> if mode does not matter
	 * @return list of child configurations contained in the given composite configuration
	 * @throws CoreException
	 */
	@Nonnull
	public static List<ILaunchConfiguration> getChildConfigurations(@Nonnull ILaunchConfiguration configuration,
			@Nullable String mode) throws CoreException {
		Preconditions.checkArgument(COMPOSITE_CONF_TYPE_ID.equals(configuration.getType().getIdentifier()));

		List<ILaunchConfiguration> childConfs = Lists.newLinkedList();
		Set<String> childConfNames = Sets.newHashSet(Splitter.on("@").split(
				configuration.getAttribute(NESTED_CONFIGURATIONS, "")));
		ILaunchConfiguration[] allConfigurations = DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurations();
		for (ILaunchConfiguration childConf : allConfigurations) {
			try {
				if (childConfNames.contains(childConf.getName())
						&& (mode == null || childConf.getType().supportsMode(mode))) {
					childConfs.add(childConf);
				}
			} catch (CoreException e) {
				log(e);
			}
		}
		return childConfs;
	}

	/**
	 * Shortcut for <code>getChildConfigurations(configuration, null)</code>
	 * 
	 * @param configuration
	 * @return
	 * @throws CoreException
	 * @see CompositeLaunchPlugin#getChildConfigurations(ILaunchConfiguration, String)
	 */
	@Nonnull
	public static List<ILaunchConfiguration> getChildConfigurations(@Nonnull ILaunchConfiguration configuration)
			throws CoreException {
		return getChildConfigurations(configuration, null);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CompositeLaunchPlugin getDefault() {
		return plugin;
	}

	public static void log(@Nonnull CoreException e) {
		CompositeLaunchPlugin.getDefault().getLog().log(e.getStatus());
	}
}
