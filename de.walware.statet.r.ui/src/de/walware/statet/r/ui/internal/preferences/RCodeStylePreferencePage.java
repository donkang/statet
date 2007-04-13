/*******************************************************************************
 * Copyright (c) 2007 StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.ui.internal.preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import de.walware.statet.ext.ui.preferences.PropertyAndPreferencePage;


/**
 * A Property- and PreferencePage for RCodeStyle settings.
 */
public class RCodeStylePreferencePage extends PropertyAndPreferencePage<RCodeStylePreferenceBlock> {

	public static final String PREF_ID = "de.walware.statet.r.ui.preferencePages.RCodeStyle"; //$NON-NLS-1$

	
	@Override
	protected String getPreferencePageID() {
		
		return PREF_ID;
	}
	
	@Override
	protected String getPropertyPageID() {
		
		return null;
	}
	
	@Override
	protected RCodeStylePreferenceBlock createConfigurationBlock() throws CoreException {
		
		return new RCodeStylePreferenceBlock(getProject(), getNewStatusChangedListener());
	}
	
	@Override
	protected boolean hasProjectSpecificSettings(IProject project) {
		
		return fBlock.hasProjectSpecificOptions(project);
	}

}
