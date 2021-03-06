/*******************************************************************************
 * This file is protected by Copyright. 
 * Please refer to the COPYRIGHT file distributed with this source distribution.
 *
 * This file is part of REDHAWK IDE.
 *
 * All rights reserved.  This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package gov.redhawk.ide.dcd.generator.newnode.tests;

import gov.redhawk.ide.builders.SCABuilder;
import gov.redhawk.ide.dcd.generator.newnode.NodeProjectCreator;

import org.junit.Assert;
import mil.jpeojtrs.sca.dcd.DeviceConfiguration;
import mil.jpeojtrs.sca.spd.SoftPkg;
import mil.jpeojtrs.sca.util.ScaResourceFactoryUtil;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.junit.Test;

/**
 * A class to test {@link NodeProjectCreatorTest}.
 */
public class NodeProjectCreatorTest {

	/**
	 * Tests creating an empty project.
	 * IDE-1707 Ensure ScaProjectNature was configured, adding the SCA builder
	 */
	@Test
	public void createEmptyProject() throws CoreException {
		final IProject project = NodeProjectCreator.createEmptyProject("nodeProjectTest", null, new NullProgressMonitor());

		Assert.assertNotNull(project);
		Assert.assertTrue("nodeProjectTest".equals(project.getName()));
		boolean found = false;
		for (ICommand command : project.getDescription().getBuildSpec()) {
			if (command.getBuilderName().equals(SCABuilder.ID)) {
				found = true;
			}
		}
		Assert.assertTrue("SCA builder not found", found);

		project.delete(true, new NullProgressMonitor());
	}

	/**
	 * Tests creating the device files
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCreateDeviceFiles() throws CoreException {
		final IProject project = NodeProjectCreator.createEmptyProject("nodeProjectTest", null, new NullProgressMonitor());
		Assert.assertNotNull(project);
		Assert.assertTrue("nodeProjectTest".equals(project.getName()));
		
		NodeProjectCreator.createNodeFiles(project, "nodeProjectTest", "Author", "REDHAWK_DEV", new SoftPkg[0], new NullProgressMonitor());
		
		final IFile dcdFile = project.getFile("DeviceManager.dcd.xml");
		Assert.assertTrue(dcdFile.exists());
		
		final ResourceSet resourceSet = ScaResourceFactoryUtil.createResourceSet();
		final DeviceConfiguration dev = DeviceConfiguration.Util.getDeviceConfiguration(resourceSet.getResource(URI.createPlatformResourceURI("/nodeProjectTest/DeviceManager.dcd.xml", true), true));
		Assert.assertEquals(project.getName(), dev.getName());
		Assert.assertEquals("REDHAWK_DEV/REDHAWK_DEV", dev.getDomainManager().getNamingService().getName());
		Assert.assertEquals(0, dev.getPartitioning().getComponentPlacement().size());

		project.delete(true, new NullProgressMonitor());
	}
}
