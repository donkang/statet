/*******************************************************************************
 * Copyright (c) 2006-2010 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.nico;

import de.walware.statet.nico.core.runtime.Prompt;


/**
 * Prompt to continue incomplete input.
 * 
 * The flag IBasicRAdapter.META_PROMPT_INCOMPLETE_INPUT is setted.
 * The previous incomplete input accesible as field.
 */
public class ContinuePrompt extends Prompt {
	
	
	/**
	 * The whole previous incomplete input (can be multiple lines).
	 */
	public final String previousInput;
	
	
	ContinuePrompt(final Prompt previousPrompt, String lastInput, final String promptText, final int meta) {
		super(promptText, IRBasicAdapter.META_PROMPT_INCOMPLETE_INPUT | meta);
		if ((previousPrompt.meta & IRBasicAdapter.META_PROMPT_INCOMPLETE_INPUT) != 0) {
			lastInput = ((ContinuePrompt) previousPrompt).previousInput + lastInput;
		}
		previousInput = lastInput;
	}
	
}
