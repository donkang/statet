/*******************************************************************************
 * Copyright (c) 2005-2010 StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.internal.nico.ui;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;

import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ui.sourceediting.SourceEditorViewerConfigurator;
import de.walware.ecommons.ui.ISettingsChangedHandler;

import de.walware.statet.base.ui.IStatetUICommandIds;
import de.walware.statet.nico.core.runtime.Prompt;
import de.walware.statet.nico.ui.console.ConsolePageEditor;

import de.walware.statet.r.core.rsource.RHeuristicTokenScanner;
import de.walware.statet.r.nico.ContinuePrompt;
import de.walware.statet.r.nico.IRBasicAdapter;
import de.walware.statet.r.nico.ui.RConsolePage;
import de.walware.statet.r.ui.editors.InsertAssignmentAction;
import de.walware.statet.r.ui.editors.RSourceViewerConfigurator;


/**
 * R Console input line
 */
public class RInputGroup extends ConsolePageEditor implements ISettingsChangedHandler {
	
	
	private RSourceViewerConfigurator fRConfig;
	
	
	public RInputGroup(final RConsolePage page) {
		super(page);
	}
	
	
	@Override
	protected ISourceUnit createSourceUnit() {
		return new RConsoleSourceUnit((RConsolePage) getConsolePage(), fDocument);
	}
	
	
	@Override
	protected void onPromptUpdate(final Prompt prompt) {
		if ((prompt.meta & IRBasicAdapter.META_PROMPT_INCOMPLETE_INPUT) != 0) {
			final ContinuePrompt p = (ContinuePrompt) prompt;
			fDocument.setPrefix(p.previousInput);
		}
		else {
			fDocument.setPrefix(""); //$NON-NLS-1$
		}
	}
	
	@Override
	public Composite createControl(final Composite parent, final SourceEditorViewerConfigurator editorConfig) {
		fRConfig = (RSourceViewerConfigurator) editorConfig;
		final Composite control = super.createControl(parent, editorConfig);
		return control;
	}
	
	@Override
	public void configureServices(final IHandlerService commands, final IContextService keys) {
		super.configureServices(commands, keys);
		
		keys.activateContext("de.walware.statet.r.contexts.REditor"); //$NON-NLS-1$
		
		IAction action;
		action = new InsertAssignmentAction(this);
		commands.activateHandler(IStatetUICommandIds.INSERT_ASSIGNMENT, new ActionHandler(action));
	}
	
	@Override
	public Object getAdapter(final Class required) {
		if (RHeuristicTokenScanner.class.equals(required)) {
			return new RHeuristicTokenScanner();
		}
		return super.getAdapter(required);
	}
	
}
