/*******************************************************************************
 * Copyright (c) 2005-2010 WalWare/StatET-Project (www.walware.de/goto/statet).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.statet.r.nico;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import de.walware.statet.nico.core.runtime.IToolRunnable;
import de.walware.statet.nico.core.runtime.IToolRunnableControllerAdapter;
import de.walware.statet.nico.core.runtime.ITrack;
import de.walware.statet.nico.core.runtime.Prompt;
import de.walware.statet.nico.core.runtime.SubmitType;
import de.walware.statet.nico.core.runtime.ToolController;
import de.walware.statet.nico.core.runtime.ToolProcess;
import de.walware.statet.nico.core.util.TrackWriter;
import de.walware.statet.nico.core.util.TrackingConfiguration;

import de.walware.statet.r.core.model.RElementName;
import de.walware.statet.r.internal.nico.RNicoMessages;


/**
 * Abstract superclass of controllers for R.
 * 
 * All implementations of {@link ToolController} for R should extends this class.
 */
public abstract class AbstractRController
		extends ToolController<RWorkspace>
		implements IRBasicAdapter, IRSetupAdapter {
	
	
	public static class RCommandRunnable extends ConsoleCommandRunnable {
		
		protected RCommandRunnable(final String text, final SubmitType type) {
			super(text, type);
		}
		
		@Override
		public void run(final IToolRunnableControllerAdapter adapter, final IProgressMonitor monitor) throws InterruptedException, CoreException {
			final AbstractRController r = (AbstractRController) adapter;
			super.run(r, monitor);
			if ((r.fCurrentPrompt.meta & IRBasicAdapter.META_PROMPT_INCOMPLETE_INPUT) == 0) {
				r.briefAboutChange(RWorkspace.REFRESH_AUTO);
			}
		}
		
	}
	
	public static final String SHOW_RHELP_HANDLER_ID = "r/showHelp"; //$NON-NLS-1$
	
	public static final String INIT_RGRAPHIC_FACTORY_HANDLER_ID = "r/initRGraphicFactory"; //$NON-NLS-1$
	
	
	protected String fStartupSnippet;
	
	protected String fContinuePromptText;
	protected String fDefaultPromptText;
	
	protected int fChanged;
	protected final Set<RElementName> fChangedEnvirs = new HashSet<RElementName>();
	
	protected List<TrackingConfiguration> fTrackingConfigurations;
	
	
	public AbstractRController(final ToolProcess process, final Map<String, Object> initData) {
		super(process, initData);
		process.registerFeatureSet(RTool.R_BASIC_FEATURESET_ID);
		fChanged = RWorkspace.REFRESH_COMPLETE;
	}
	
	
	public void setStartupSnippet(final String code) {
		fStartupSnippet = code;
	}
	
	@Override
	protected final IToolRunnable createQuitRunnable() {
		return new IToolRunnable() {
			
			public String getTypeId() {
				return ToolController.QUIT_TYPE_ID;
			}
			
			public String getLabel() {
				return RNicoMessages.Quit_Task_label;
			}
			
			public SubmitType getSubmitType() {
				return SubmitType.TOOLS;
			}
			
			public void changed(final int event, final ToolProcess process) {
			}
			
			public void run(final IToolRunnableControllerAdapter adapter, final IProgressMonitor monitor) throws InterruptedException, CoreException {
				((IRBasicAdapter) adapter).quit(monitor);
			}
		};
	}
	
	@Override
	protected IToolRunnable createCancelPostRunnable(final int options) {
		return new IToolRunnable() {
			public SubmitType getSubmitType() {
				return SubmitType.OTHER;
			}
			public String getTypeId() {
				return "common/cancel/post"; //$NON-NLS-1$
			}
			public String getLabel() {
				return "Reset prompt";
			}
			public void changed(final int event, final ToolProcess process) {
			}
			public void run(final IToolRunnableControllerAdapter tools, final IProgressMonitor monitor) throws InterruptedException, CoreException {
				if (!isTerminated()) {
					postCancelTask(options, monitor);
				}
			}
		};
	}
	
	protected void postCancelTask(final int options, final IProgressMonitor monitor) throws CoreException {
		final String text = fCurrentPrompt.text + (
				((fCurrentPrompt.meta & IRBasicAdapter.META_PROMPT_INCOMPLETE_INPUT) != 0) ?
						"(Input cancelled)" : "(Command cancelled)") + 
						fLineSeparator;
		fInfoStream.append(text,
				(fCurrentRunnable != null) ? fCurrentRunnable.getSubmitType() : SubmitType.TOOLS, fCurrentPrompt.meta);
	}
	
	public boolean supportsBusy() {
		return false;
	}
	
	public boolean isBusy() {
		return false;
	}
	
	
//-- Runnable Adapter
	@Override
	protected void initRunnableAdapter() {
		super.initRunnableAdapter();
		setDefaultPromptText("> "); //$NON-NLS-1$
		setContinuePromptText("+ "); //$NON-NLS-1$
	}
	
	protected void initTracks(final String directory, final IProgressMonitor monitor, final List<IStatus> warnings)
			throws CoreException {
		if (fTrackingConfigurations != null) {
			final List<ITrack> tracks = new ArrayList<ITrack>(fTrackingConfigurations.size());
			for (final TrackingConfiguration trackingConfig : fTrackingConfigurations) {
				final TrackWriter tracker = new TrackWriter(this, trackingConfig);
				final IStatus status = tracker.init(monitor);
				if (status.getSeverity() == IStatus.OK) {
					tracks.add(tracker);
					addDisposable(tracker);
				}
				else {
					warnings.add(status);
				}
			}
			setTracks(tracks);
		}
	}
	
	
	public void setRObjectDB(final boolean enable) {
		fWorkspaceData.enableRObjectDB(enable);
	}
	
	public Object getAdapter(final Class adapter) {
		if (IRSetupAdapter.class.equals(adapter)) {
			return this;
		}
		return null;
	}
	
	@Override
	public IToolRunnable createCommandRunnable(final String command, final SubmitType type) {
		return new RCommandRunnable(command, type);
	}
	
	@Override
	public void setDefaultPromptText(String text) {
		if (text == null || text.equals(fDefaultPromptText)) {
			return;
		}
		text = text.intern();
		fDefaultPromptText = text;
		super.setDefaultPromptText(text);
	}
	
	public void setContinuePromptText(String text) {
		if (text == null || text.equals(fContinuePromptText)) {
			return;
		}
		text = text.intern();
		fContinuePromptText = text;
	}
	
	protected final void setCurrentPrompt(final String text, final boolean addToHistory) {
		if (fDefaultPromptText.equals(text)) {
			if (addToHistory) {
				setCurrentPrompt(fDefaultPrompt);
			}
			else {
				setCurrentPrompt(new Prompt(fDefaultPromptText,
						IToolRunnableControllerAdapter.META_HISTORY_DONTADD | IToolRunnableControllerAdapter.META_PROMPT_DEFAULT));
			}
		}
		else if (fContinuePromptText.equals(text)) {
			setCurrentPrompt(new ContinuePrompt(
					fCurrentPrompt, fCurrentInput+fLineSeparator, fContinuePromptText,
					addToHistory ? 0 : IToolRunnableControllerAdapter.META_HISTORY_DONTADD));
		}
		else if (text != null) {
			setCurrentPrompt(new Prompt(text,
					addToHistory ? 0 : IToolRunnableControllerAdapter.META_HISTORY_DONTADD));
		}
		else { // TODO log warning / exception?
			setCurrentPrompt(new Prompt("", //$NON-NLS-1$
					addToHistory ? 0 : IToolRunnableControllerAdapter.META_HISTORY_DONTADD));
		}
	}
	
	public void briefAboutChange(final int o) {
		fChanged |= o;
	}
	
	public void briefAboutChange(final Object changed, final int o) {
		if (changed instanceof Collection) {
			final Collection collection = (Collection) changed;
			for (final Object object : collection) {
				briefAboutChange(object, o);
			}
		}
		if (changed instanceof RElementName) {
			final RElementName name = (RElementName) changed;
			if (name.getType() == RElementName.MAIN_SEARCH_ENV) {
				fChangedEnvirs.add(name);
			}
		}
	}
	
	public void quit(final IProgressMonitor monitor) throws CoreException {
		final String command = "q()"; //$NON-NLS-1$
		submitToConsole(command, monitor);
	}
	
}
