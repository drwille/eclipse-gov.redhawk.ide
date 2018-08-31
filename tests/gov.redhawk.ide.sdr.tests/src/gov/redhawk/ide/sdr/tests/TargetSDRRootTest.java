/**
 * This file is protected by Copyright.
 * Please refer to the COPYRIGHT file distributed with this source distribution.
 *
 * This file is part of REDHAWK IDE.
 *
 * All rights reserved.  This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */
package gov.redhawk.ide.sdr.tests;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

import gov.redhawk.ide.sdr.IdeSdrActivator;
import gov.redhawk.ide.sdr.LoadState;
import gov.redhawk.ide.sdr.SdrPackage;
import gov.redhawk.ide.sdr.SdrRoot;
import gov.redhawk.ide.sdr.TargetSdrRoot;
import gov.redhawk.ide.sdr.preferences.IdeSdrPreferenceConstants;
import gov.redhawk.model.sca.commands.ScaModelCommand;

public class TargetSDRRootTest {

	private Adapter adapter;

	@Test
	public void test() throws InterruptedException, URISyntaxException, IOException {
		// We should be able to access the SDR root
		SdrRoot sdrRoot = TargetSdrRoot.getSdrRoot();
		Assert.assertNotNull(sdrRoot);

		// Wait for the SDR root to be done with its initial load
		long startTime = System.currentTimeMillis();
		while ((sdrRoot.getState() != LoadState.LOADED || Job.getJobManager().find(TargetSdrRoot.FAMILY_REFRESH_SDR).length > 0)
			&& System.currentTimeMillis() < startTime + 5000) {
			Thread.sleep(250);
		}
		Assert.assertTrue(sdrRoot.getState() == LoadState.LOADED);

		// Listen to state changes
		final BlockingQueue<LoadState> loadStates = new LinkedBlockingQueue<>();
		adapter = new AdapterImpl() {
			public void notifyChanged(Notification msg) {
				if (msg.getFeatureID(SdrRoot.class) == SdrPackage.SDR_ROOT__STATE) {
					loadStates.add((LoadState) msg.getNewValue());
				}
			}
		};
		ScaModelCommand.execute(sdrRoot, () -> {
			sdrRoot.eAdapters().add(adapter);
		});

		// Change the SDR root path preference
		String path = FileLocator.getBundleFile(FrameworkUtil.getBundle(getClass())).toPath().resolve("testFiles/sdr").toString();
		InstanceScope.INSTANCE.getNode(IdeSdrActivator.PLUGIN_ID).put(IdeSdrPreferenceConstants.SCA_LOCAL_SDR_PATH_PREFERENCE, path);

		// SDR root should re-load and have the appropriate contents
		verifyLoadAndContents(loadStates, sdrRoot);

		// Refresh the SDR root; it should again re-load and have the appropriate contents
		loadStates.clear();
		TargetSdrRoot.scheduleRefresh();
		verifyLoadAndContents(loadStates, sdrRoot);
	}

	private void verifyLoadAndContents(BlockingQueue<LoadState> loadStates, SdrRoot sdrRoot) throws InterruptedException {
		// Wait for the appropriate state changes
		Assert.assertEquals(LoadState.UNLOADED, loadStates.poll(5, TimeUnit.SECONDS));
		Assert.assertEquals(LoadState.LOADING, loadStates.poll(5, TimeUnit.SECONDS));
		Assert.assertEquals(LoadState.LOADED, loadStates.poll(10, TimeUnit.SECONDS));

		// Verify the appropriate components, devices, etc are present
		Set<String> names = sdrRoot.getComponentsContainer().getComponents().stream() //
				.map(component -> component.getName()) //
				.collect(Collectors.toSet());
		assertNames(names, Arrays.asList("CppComponentWithDeps", "CppComponentWithDeps2", "FrequencyShift", "Reader", "Writer"));
		names = sdrRoot.getSharedLibrariesContainer().getComponents().stream() //
				.map(sharedlib -> sharedlib.getName()) //
				.collect(Collectors.toSet());
		assertNames(names, Arrays.asList("CppDepA", "CppDepAB", "CppDepAC", "CppDepD", "CppDepDE"));
		names = sdrRoot.getDevicesContainer().getComponents().stream() //
				.map(device -> device.getName()) //
				.collect(Collectors.toSet());
		assertNames(names, Arrays.asList("BasicTestDevice"));
		names = sdrRoot.getServicesContainer().getComponents().stream() //
				.map(service -> service.getName()) //
				.collect(Collectors.toSet());
		assertNames(names, Collections.emptyList());
		names = sdrRoot.getWaveformsContainer().getWaveforms().stream() //
				.map(waveform -> waveform.getName()) //
				.collect(Collectors.toSet());
		assertNames(names, Arrays.asList("test"));
		names = sdrRoot.getNodesContainer().getNodes().stream() //
				.map(node -> node.getName()) //
				.collect(Collectors.toSet());
		assertNames(names, Arrays.asList("DeviceManager"));
	}

	private void assertNames(Set<String> actualNames, List<String> requiredNames) {
		for (String requiredName : requiredNames) {
			Assert.assertTrue("Did not find " + requiredName, actualNames.remove(requiredName));
		}
		Assert.assertTrue(actualNames.isEmpty());
	}

	@After
	public void after() throws CoreException {
		// Remove the adapter we added to the target SdrRoot object
		if (adapter != null) {
			EObject target = (EObject) adapter.getTarget();
			if (target != null) {
				ScaModelCommand.execute(target, () -> {
					target.eAdapters().remove(adapter);
				});
			}
			adapter = null;
		}

		// Remove our preference change
		InstanceScope.INSTANCE.getNode(IdeSdrActivator.PLUGIN_ID).remove(IdeSdrPreferenceConstants.SCA_LOCAL_SDR_PATH_PREFERENCE);
	}
}