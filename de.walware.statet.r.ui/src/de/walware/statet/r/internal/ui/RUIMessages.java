/*******************************************************************************
 * Copyright (c) 2007 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.ui;

import org.eclipse.osgi.util.NLS;


public class RUIMessages extends NLS {
	

	public static String ChooseREnv_WorkbenchDefault_label;
	public static String ChooseREnv_Selected_label;
	public static String ChooseREnv_Configure_label;
	public static String ChooseREnv_error_InvalidPreferences_message;
	public static String ChooseREnv_error_IncompleteSelection_message;
	public static String ChooseREnv_error_InvalidSelection_message;
	
	
	static {
		NLS.initializeMessages(RUIMessages.class.getName(), RUIMessages.class);
	}
	
}