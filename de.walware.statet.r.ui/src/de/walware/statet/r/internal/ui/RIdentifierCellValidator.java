/*******************************************************************************
 * Copyright (c) 2009-2010 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.ui;

import org.eclipse.jface.viewers.ICellEditorValidator;

import de.walware.statet.r.core.refactoring.RRefactoringAdapter;


public class RIdentifierCellValidator implements ICellEditorValidator {
	
	
	private RRefactoringAdapter fAdapter = new RRefactoringAdapter();
	
	
	public String isValid(final Object value) {
		final String s = (value instanceof String) ? (String) value : null;
		final String message = fAdapter.validateIdentifier(s, null);
		if (message != null) {
			return message;
		}
		return isValidInContext(s);
	}
	
	protected String isValidInContext(final String value) {
		return null;
	}
	
}
