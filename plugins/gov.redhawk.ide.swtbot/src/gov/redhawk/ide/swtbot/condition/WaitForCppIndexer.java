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
package gov.redhawk.ide.swtbot.condition;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.ui.statushandlers.StatusManager;

import gov.redhawk.ide.swtbot.SwtBotActivator;

/**
 * Waits for the C/C++ index to finish. If the indexer is not running, the condition is satisfied.
 */
public class WaitForCppIndexer extends DefaultCondition {

	public static final long TIMEOUT = 60000;

	private long startingWaitTime;
	private boolean firstTime;

	@Override
	public void init(SWTBot bot) {
		super.init(bot);
		startingWaitTime = System.currentTimeMillis();
		firstTime = true;
	}

	@Override
	public boolean test() throws Exception {
		Job[] jobs = Job.getJobManager().find(null);
		for (Job job : jobs) {
			if ("org.eclipse.cdt.internal.core.pdom.PDOMIndexerJob".equals(job.getClass().getName())) {
				firstTime = false;
				return false;
			}
		}

		if (firstTime) {
			StatusManager.getManager().handle(new Status(IStatus.INFO, SwtBotActivator.PLUGIN_ID, "CDT indexer was not running"), StatusManager.LOG);
		} else {
			String msg = String.format("CDT indexer completed in %f seconds", (System.currentTimeMillis() - startingWaitTime) / 1000.0);
			StatusManager.getManager().handle(new Status(IStatus.INFO, SwtBotActivator.PLUGIN_ID, msg), StatusManager.LOG);
		}
		return true;
	}

	@Override
	public String getFailureMessage() {
		return "C/C++ indexer was still running";
	}

}
