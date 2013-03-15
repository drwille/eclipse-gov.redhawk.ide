/** 
 * REDHAWK HEADER
 *
 * Identification: $Revision: 9208 $
 */
package gov.redhawk.ide.debug;

import gov.redhawk.ide.sdr.util.ScaEnvironmentUtil;
import gov.redhawk.sca.launch.ScaLaunchConfigurationConstants;

import java.util.Map;

import mil.jpeojtrs.sca.scd.ComponentType;
import mil.jpeojtrs.sca.scd.SoftwareComponent;
import mil.jpeojtrs.sca.spd.SoftPkg;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;

/**
 * @since 2.0
 * 
 */
public abstract class AbstractLaunchConfigurationFactory implements ILaunchConfigurationFactory, IExecutableExtension {

	private String launchConfigId;
	private String id;

	public void setLaunchConfigID(final String launchConfigId) {
		this.launchConfigId = launchConfigId;
	}

	public ILaunchConfigurationWorkingCopy createLaunchConfiguration(String name, final String implId, final SoftPkg spd) throws CoreException {
		if (name == null) {
			name = spd.getName();
		}

		final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		final String launchConfigName = launchManager.generateLaunchConfigurationName(name);

		final String configTypeId = getLaunchConfigTypeID();
		final ILaunchConfigurationType configType = launchManager.getLaunchConfigurationType(configTypeId);
		if (configType == null) {
			throw new CoreException(new Status(Status.ERROR, ScaDebugPlugin.ID, "Failed to find launch configuration type of: " + configTypeId + " invalid launch configuration factory " + id, null));
		}
		final ILaunchConfigurationWorkingCopy retVal = configType.newInstance(null, launchConfigName);

		retVal.setAttribute(ScaDebugLaunchConstants.ATT_IMPL_ID, implId);
		retVal.setAttribute(ScaLaunchConfigurationConstants.ATT_PROFILE, getProfile(spd));

		// Setup Environment variables for override locations of OSSIEHOME and SDRROOT
		final Map<String, String> envVar = ScaEnvironmentUtil.getLauncherEnvMap();

		retVal.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
		retVal.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, envVar);

		final String progArgs = getProgramArguments(spd);
		setProgramArguments(progArgs, retVal);

		return retVal;
	}

	protected String getProgramArguments(final SoftPkg spd) {
		final ComponentType type = SoftwareComponent.Util.getWellKnownComponentType(spd.getDescriptor().getComponent());
		return SpdLauncherUtil.getDefaultProgramArguments(type);
	}

	protected String getLaunchConfigTypeID() {
		return this.launchConfigId;
	}

	protected String getProfile(final SoftPkg spd) {
		return spd.eResource().getURI().path();
	}

	public void setInitializationData(final IConfigurationElement config, final String propertyName, final Object data) throws CoreException {
		this.launchConfigId = config.getAttribute("launchConfigType");
		this.id = config.getAttribute("id");

	}

}
