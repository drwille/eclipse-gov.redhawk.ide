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
package gov.redhawk.ide.graphiti.ui.diagram.patterns;

import gov.redhawk.diagram.util.FindByStubUtil;
import gov.redhawk.ide.graphiti.ui.diagram.providers.ImageProvider;
import gov.redhawk.ide.graphiti.ui.diagram.wizards.FindByCORBANameWizardPage;

import mil.jpeojtrs.sca.partitioning.FindByStub;
import mil.jpeojtrs.sca.partitioning.NamingService;
import mil.jpeojtrs.sca.partitioning.PartitioningFactory;
import mil.jpeojtrs.sca.partitioning.ProvidesPortStub;
import mil.jpeojtrs.sca.partitioning.UsesPortStub;

import org.eclipse.emf.common.util.EList;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.pattern.IPattern;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

public class FindByCORBANamePattern extends AbstractFindByPattern implements IPattern {

	public static final String NAME = "Find By Name";

	public FindByCORBANamePattern() {
		super();
	}

	@Override
	public String getCreateName() {
		return NAME;
	}

	@Override
	public String getCreateDescription() {
		return "";
	}

	@Override
	public String getCreateImageId() {
		return ImageProvider.IMG_FIND_BY_CORBA_NAME;
	}

	// THE FOLLOWING METHOD DETERMINE IF PATTERN IS APPLICABLE TO OBJECT
	@Override
	protected boolean isMatchingFindByType(FindByStub findByStub) {
		return FindByStubUtil.isFindByStubName(findByStub);
	}

	// DIAGRAM FEATURES
	@Override
	protected FindByStub createFindByStub(ICreateContext context) {
		// prompt user for CORBA Name
		FindByCORBANameWizardPage page = openWizard();
		if (page == null) {
			return null;
		}

		FindByStub findByStub = FindByCORBANamePattern.create(page.getModel().getCorbaName());

		// if applicable add uses port stub(s)
		addUsesPortStubs(findByStub, page.getModel().getUsesPortNames());

		// if applicable add provides port stub(s)
		addProvidesPortStubs(findByStub, page.getModel().getProvidesPortNames());

		return findByStub;
	}

	/**
	 * Creates the FindByStub in the diagram with the provided namingServiceText
	 * Has no real purpose in this class except that it's logic is extremely similar to the above create method. It's
	 * purpose
	 * is to create a FindByStub using information in the model sad.xml file when no diagram file is available
	 * @param namingServiceName
	 * @return
	 */
	public static FindByStub create(final String namingServiceName) {
		FindByStub findByStub = PartitioningFactory.eINSTANCE.createFindByStub();

		// interface stub (lollipop)
		findByStub.setInterface(PartitioningFactory.eINSTANCE.createComponentSupportedInterfaceStub());

		// naming service (corba name)
		NamingService namingService = PartitioningFactory.eINSTANCE.createNamingService();
		namingService.setName(namingServiceName);
		findByStub.setNamingService(namingService);

		return findByStub;
	}

	@Override
	public String getInnerTitle(FindByStub findByStub) {
		return findByStub.getNamingService().getName();
	}
	
	@Override
	public void setInnerTitle(FindByStub findByStub, String value) {
		findByStub.getNamingService().setName(value);
	}

	@Override 
	public String getCheckValueValidName() {
		return "CORBA";
	}

	protected static FindByCORBANameWizardPage openWizard() {
		return openWizard(null, new Wizard() {
			public boolean performFinish() {
				return true;
			}
		});
	}

	public static FindByCORBANameWizardPage openWizard(FindByStub existingFindByStub, Wizard wizard) {
		FindByCORBANameWizardPage page = new FindByCORBANameWizardPage();
		wizard.addPage(page);
		if (existingFindByStub != null) {
			fillWizardFieldsWithExistingProperties(page, existingFindByStub);
		}
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
		if (dialog.open() == WizardDialog.CANCEL) {
			return null;
		}
		return page;
	}

	private static void fillWizardFieldsWithExistingProperties(FindByCORBANameWizardPage page, FindByStub findByStub) {
		// Grab existing properties from findByStub
		String corbaName = findByStub.getNamingService().getName();
		EList<UsesPortStub> usesPorts = findByStub.getUses();
		EList<ProvidesPortStub> providesPorts = findByStub.getProvides();

		// Fill wizard fields with existing properties
		page.getModel().setCorbaName(corbaName);
		if (usesPorts != null && !usesPorts.isEmpty()) {
			for (UsesPortStub port : usesPorts) {
				page.getModel().getUsesPortNames().add(port.getName());
			}
		}
		if (providesPorts != null && !providesPorts.isEmpty()) {
			for (ProvidesPortStub port : providesPorts) {
				page.getModel().getProvidesPortNames().add(port.getName());
			}
		}
	}

	@Override
	public String getOuterImageId() {
		return ImageProvider.IMG_FIND_BY;
	}
}
