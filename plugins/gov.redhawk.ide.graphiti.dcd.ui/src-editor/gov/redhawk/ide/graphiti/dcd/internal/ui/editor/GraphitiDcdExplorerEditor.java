/**
 * This file is protected by Copyright. 
 * Please refer to the COPYRIGHT file distributed with this source distribution.
 * 
 * This file is part of REDHAWK IDE.
 * 
 * All rights reserved.  This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 */
package gov.redhawk.ide.graphiti.dcd.internal.ui.editor;

import gov.redhawk.ide.graphiti.ui.diagram.util.DUtil;
import mil.jpeojtrs.sca.dcd.DeviceConfiguration;

import org.eclipse.emf.ecore.resource.Resource;

public class GraphitiDcdExplorerEditor extends GraphitiDcdSandboxEditor {

	public GraphitiDcdExplorerEditor() {
	}

	@Override
	public String getDiagramContext(Resource sadResource) {
		return DUtil.DIAGRAM_CONTEXT_EXPLORER;
	}

	@Override
	protected void createModel() {
		super.createModel();
		setDcd(DeviceConfiguration.Util.getDeviceConfiguration(super.getMainResource()));
		initModelMap();
	}
}
