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
package gov.redhawk.ide.sad.graphiti.ui.runtime.chalkboard.tests;

import gov.redhawk.ide.sad.graphiti.ext.impl.ComponentShapeImpl;
import gov.redhawk.ide.swtbot.diagram.DiagramTestUtils;
import gov.redhawk.ide.swtbot.scaExplorer.ScaExplorerTestUtils;

import org.eclipse.swtbot.eclipse.gef.finder.widgets.SWTBotGefEditPart;
import org.eclipse.swtbot.eclipse.gef.finder.widgets.SWTBotGefEditor;
import org.junit.Assert;
import org.junit.Test;

public class ChalkboardStartOrderTest extends AbstractGraphitiChalkboardTest {

	private SWTBotGefEditor editor;
	private static final String[] CHALKBOARD_PARENT_PATH = {"Sandbox"};
	private static final String CHALKBOARD = "Chalkboard";
	private static final String SIGGEN = "SigGen";

	/**
	 * IDE-326
	 * Test to make sure the Start Order ellipse doesn't not get drawn in the sandbox
	 */
	@Test
	public void removeStartOrderIconTest() {
		// open chalkboard diagram
		ScaExplorerTestUtils.openDiagramFromScaExplorer(gefBot, CHALKBOARD_PARENT_PATH, CHALKBOARD);
		editor = gefBot.gefEditor(CHALKBOARD);
		editor.setFocus();

		//drag SigGen to diagram and verify loaded in sca explorer
		DiagramTestUtils.dragFromPaletteToDiagram(editor, SIGGEN, 0, 0);
		ScaExplorerTestUtils.waitUntilComponentDisplaysInScaExplorer(bot, CHALKBOARD_PARENT_PATH, CHALKBOARD, SIGGEN + "_1");
		

		SWTBotGefEditPart sigGenEditPart = editor.getEditPart(SIGGEN);
		ComponentShapeImpl componentShape = (ComponentShapeImpl) sigGenEditPart.part().getModel();
		Assert.assertNull("Start Order ellipse should not be created during runtime", componentShape.getStartOrderEllipseShape());
	}
}