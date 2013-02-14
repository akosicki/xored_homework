package pl.kosicki.ui;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationTreeContentProvider;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupFilter;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.WorkbenchViewerComparator;

import pl.kosicki.CompositeLaunchPlugin;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Settings for the composite launch configuration, allows user to select list of a child configurations
 */
public class CompositeLaunchConfigurationTab extends AbstractLaunchConfigurationTab {
	@Nullable
	private CheckboxTreeViewer viewer;
	@Nullable
	private ITreeContentProvider contentProvider;

	/**
	 * Selected children launch configurations that do not support current mode e.g. we are in "run" and some child
	 * launch configurations are for "debug" only
	 */
	@Nullable
	private List<String> unsupportedChildConfs;

	@Override
	public void createControl(Composite parent) {
		viewer = new ContainerCheckedTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		setControl(viewer.getTree());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	/**
	 * Updates the buttons and message in this page's launch configuration dialog.
	 */
	protected void updateLaunchConfigurationDialog() {
		assert getLaunchConfigurationDialog() != null;
		// order is important here due to the call to
		// refresh the tab viewer in updateButtons()
		// which ensures that the messages are up to date
		getLaunchConfigurationDialog().updateButtons();
		getLaunchConfigurationDialog().updateMessage();
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		Preconditions.checkNotNull(configuration);

		final String mode = getLaunchConfigurationDialog().getMode();
		viewer.setLabelProvider(new DecoratingLabelProvider(DebugUITools.newDebugModelPresentation(), PlatformUI
				.getWorkbench().getDecoratorManager().getLabelDecorator()));
		viewer.setComparator(new WorkbenchViewerComparator());
		contentProvider = new LaunchConfigurationTreeContentProvider(mode, null);
		viewer.setContentProvider(contentProvider);
		viewer.addFilter(new LaunchGroupFilter(DebugUITools.getLaunchGroup(configuration, mode)));
		viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());

		List<Object> checkedElements = Lists.newArrayList();
		unsupportedChildConfs = Lists.newLinkedList();

		try {
			for (ILaunchConfiguration childConf : CompositeLaunchPlugin.getChildConfigurations(configuration)) {
				try {
					if (childConf.getType().supportsMode(mode)) {
						checkedElements.add(childConf);
					} else {
						unsupportedChildConfs.add(childConf.getName());
					}
				} catch (CoreException e) {
					CompositeLaunchPlugin.log(e);
				}
			}
			checkedElements.addAll(CompositeLaunchPlugin.getChildConfigurations(configuration, mode));
			viewer.setCheckedElements(checkedElements.toArray());
		} catch (CoreException e) {
			CompositeLaunchPlugin.log(e);
		}

		viewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				updateLaunchConfigurationDialog();
			}
		});
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		Preconditions.checkNotNull(configuration);
		assert unsupportedChildConfs != null;

		List<Object> checkedElements = Arrays.asList(viewer.getCheckedElements());
		CompositeLaunchPlugin.setConfigurations(configuration, Iterables.concat(
				Iterables.filter(checkedElements, Predicates.instanceOf(ILaunchConfiguration.class)),
				unsupportedChildConfs));
	}

	@Override
	public boolean isValid(ILaunchConfiguration configuration) {
		assert contentProvider != null;

		String name = configuration.getName();
		for (Object element : viewer.getCheckedElements()) {
			if (element instanceof ILaunchConfiguration) {
				if (contains((ILaunchConfiguration) element, name)) {
					setErrorMessage("Cannot select mutually dependent configurations");
					return false;
				}
			} else if (!contentProvider.hasChildren(element)) {
				assert element instanceof ILaunchConfigurationType;
				setErrorMessage("Cannot select empty configuration type");
				return false;
			}
		}

		setErrorMessage(null);
		return true;
	}

	/**
	 * @param configuration
	 * @param name
	 * @return true if the given configuration is of a composite type and contains child configuration with the given
	 *         name (possibly via other intermediate composite configurations) or holds itself the given name
	 */
	private boolean contains(@Nonnull ILaunchConfiguration configuration, @Nonnull String name) {
		if (name.equals(configuration.getName())) {
			return true;
		}
		try {
			final String mode = getLaunchConfigurationDialog().getMode();
			if (CompositeLaunchPlugin.COMPOSITE_CONF_TYPE_ID.equals(configuration.getType().getIdentifier())) {
				for (ILaunchConfiguration childConf : CompositeLaunchPlugin.getChildConfigurations(configuration, mode)) {
					if (contains(childConf, name)) {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			CompositeLaunchPlugin.log(e);
		}

		return false;
	}

	@Override
	public String getName() {
		return "Nested launches";
	}

	@Override
	public Image getImage() {
		return CompositeLaunchPlugin.getDefault().getImageRegistry().get(CompositeLaunchPlugin.RUNDEBUG_IMAGE);
	}

	@Override
	public boolean canSave() {
		return true;
	}
}
