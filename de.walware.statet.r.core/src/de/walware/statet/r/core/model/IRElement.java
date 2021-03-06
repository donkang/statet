/*******************************************************************************
 * Copyright (c) 2008-2010 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.core.model;

import java.util.List;

import de.walware.ecommons.ltk.IModelElement;


public interface IRElement extends IModelElement {
	
	
	static final int R_S4CLASS =                C1_CLASS    | 0x80;
	static final int R_S4CLASS_EXTENSION =      C1_CLASS    | 0x90;
	
	static final int R_PACKAGE_LOAD =           C1_IMPORT   | 0x10;
	
	static final int R_COMMON_FUNCTION =        C1_METHOD   | 0x10;
	static final int R_COMMON_LOCAL_FUNCTION =  C1_METHOD   | 0x11;
	static final int R_GENERIC_FUNCTION =       C1_METHOD   | 0x20;
	static final int R_S4METHOD =               C1_METHOD   | 0x80;
	
	static final int R_GENERAL_VARIABLE =       C1_VARIABLE | 0x10;
	static final int R_GENERAL_LOCAL_VARIABLE = C1_VARIABLE | 0x11;
	static final int R_ARGUMENT =               C1_VARIABLE | 0x31;
	static final int R_S4SLOT =                 C1_VARIABLE | 0x80;
	
	static final int R_DOC_EXAMPLE_CHUNK =      C2_SOURCE_CHUNK | 0x4;
	
	
	static final Filter<IModelElement> R_S4SLOT_FILTER = new Filter<IModelElement>() {
		public boolean include(final IModelElement element) {
			return (element.getElementType() == R_S4SLOT);
		}
	};
	
	
	RElementName getElementName();
	
	IRElement getModelParent();
	
	boolean hasModelChildren(Filter<? super IRElement> filter);
	List<? extends IRElement> getModelChildren(Filter<? super IRElement> filter);
	
}
