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

package de.walware.statet.r.internal.ui.help;

import org.eclipse.osgi.util.NLS;


public class Messages extends NLS {
	
	public static String RHelp_category;
	public static String RHelp_Search_RSiteSearch_label;
	public static String RHelp_Run_Help_label;
	public static String RHelp_Run_HelpSearch_label;
	
	
	static {
		NLS.initializeMessages(Messages.class.getName(), Messages.class);
	}
	private Messages() {}
	
}
