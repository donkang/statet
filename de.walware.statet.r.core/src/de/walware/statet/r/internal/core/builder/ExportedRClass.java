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

package de.walware.statet.r.internal.core.builder;

import java.io.Serializable;
import java.util.List;

import de.walware.statet.r.core.model.IRClass;
import de.walware.statet.r.core.model.IRLangElement;


public class ExportedRClass extends ExportedRElement implements IRClass, Serializable {
	
	
	private static final long serialVersionUID = -7356541747661973279L;
	
	
	private List<String> fSuperClassNames;
	
	
	public ExportedRClass(final IRLangElement parent, final IRClass sourceElement) {
		super(parent, sourceElement);
		fSuperClassNames = sourceElement.getExtendedClassNames();
	}
	
	public ExportedRClass() {
	}
	
	
	public List<String> getExtendedClassNames() {
		return fSuperClassNames;
	}
	
}
