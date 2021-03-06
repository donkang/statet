/*******************************************************************************
 * Copyright (c) 2007-2010 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.core;

import de.walware.ecommons.preferences.IPreferenceAccess;


/**
 * Interface to access R Core services, respecting the scope.
 */
public interface IRCoreAccess {
	
	
	public IPreferenceAccess getPrefs();
	
	public RCodeStyleSettings getRCodeStyle();
	
/*	public REnvConfiguration getREnvironment();*/
	
}
