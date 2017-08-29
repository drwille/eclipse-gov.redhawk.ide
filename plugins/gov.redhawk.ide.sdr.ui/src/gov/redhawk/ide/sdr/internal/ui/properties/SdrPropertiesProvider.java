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
package gov.redhawk.ide.sdr.internal.ui.properties;

import gov.redhawk.ide.sdr.SdrRoot;
import gov.redhawk.ide.sdr.ui.SdrUiPlugin;
import gov.redhawk.sca.properties.Category;
import gov.redhawk.sca.properties.IPropertiesProvider;

import java.util.ArrayList;
import java.util.List;

import mil.jpeojtrs.sca.scd.ComponentType;

/**
 * @since 5.0
 */
public class SdrPropertiesProvider implements IPropertiesProvider {

	public SdrPropertiesProvider() {
		SdrRoot targetSdr = SdrUiPlugin.getDefault().getTargetSdrRoot();
		targetSdr.load(null);
	}

	@Override
	public String getName() {
		return "Target SDR";
	}

	@Override
	public String getDescription() {
		return "Properties from software installed in the SDRROOT";
	}

	@Override
	public String getIconPluginId() {
		return "gov.redhawk.ide.sdr.edit";
	}

	@Override
	public String getIconPath() {
		return "icons/full/obj16/SdrRoot.gif";
	}

	@Override
	public List<Category> getCategories() {
		final List<Category> myList = new ArrayList<Category>();
		SdrRoot targetSdr = SdrUiPlugin.getDefault().getTargetSdrRoot();
		myList.add(new ComponentCategory(targetSdr.getComponentsContainer().getComponents(), "Components", ComponentType.RESOURCE));
		myList.add(new ComponentCategory(targetSdr.getDevicesContainer().getComponents(), "Devices", ComponentType.DEVICE));
		myList.add(new ComponentCategory(targetSdr.getServicesContainer().getComponents(), "Services", ComponentType.SERVICE));
		return myList;
	}
}