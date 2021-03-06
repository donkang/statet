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

package de.walware.statet.r.nico.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

import de.walware.ecommons.ICommonStatusConstants;

import de.walware.statet.nico.core.runtime.IRequireSynch;
import de.walware.statet.nico.core.runtime.IToolEventHandler;
import de.walware.statet.nico.core.runtime.IToolRunnable;
import de.walware.statet.nico.core.runtime.IToolRunnableControllerAdapter;
import de.walware.statet.nico.core.runtime.Prompt;
import de.walware.statet.nico.core.runtime.SubmitType;
import de.walware.statet.nico.core.runtime.ToolProcess;
import de.walware.statet.nico.core.runtime.ToolStatus;

import de.walware.statet.r.core.RCore;
import de.walware.statet.r.core.RUtil;
import de.walware.statet.r.internal.core.RCorePlugin;
import de.walware.statet.r.internal.nico.RNicoMessages;
import de.walware.statet.r.nico.AbstractRController;
import de.walware.statet.r.nico.RWorkspace;


/**
 * Controller for RTerm.
 */
public class RTermController extends AbstractRController implements IRequireSynch {
	
	
	private static final Pattern INT_OUTPUT_PATTERN = Pattern.compile("\\Q[1] \\E(\\d*)"); //$NON-NLS-1$
	private static final Pattern STRING_OUTPUT_PATTERN = Pattern.compile("\\Q[1] \"\\E((?:\\Q\\\"\\E|[^\"])*)\\\""); //$NON-NLS-1$
	
	
	private class ReadThread extends Thread {
		
		volatile int hasNoOutput;
		private int SYNC_COUNT = 2;
		private int SYNC_MS = 33;
		
		final Lock streamLock = new ReentrantLock();
		
		public ReadThread() {
			super("Rterm-Output Monitor"); //$NON-NLS-1$
		}
		
		@Override
		public void run() {
			boolean locked = false;
			try {
				boolean canRead = false;
				final char[] b = new char[1024];
				while (fProcess != null | (canRead = fProcessOutputReader.ready())) {
					fProcessOutputBuffer.available();
					if (canRead || hasNoOutput > SYNC_COUNT) {
						if (!canRead && locked) {
							streamLock.unlock();
							locked = false;
						}
						int n = fProcessOutputReader.read(b);
						if (n > 0) {
							hasNoOutput = 0;
							if (!locked) {
								streamLock.lock();
								locked = true;
							}
							final String s = new String(b, 0, n);
							fDefaultOutputStream.append(s, SubmitType.CONSOLE, 0);
							n = s.length();
							if (n >= 2 && s.charAt(--n) == ' ' && (s.charAt(--n) == '>' || s.charAt(n) == '+')) {
								hasNoOutput++;
								getControllerThread().interrupt();
							}
							continue;
						}
						else if (n < 0) {
							onRTerminated();
							return;
						}
					}
					try {
						Thread.sleep(SYNC_MS);
						hasNoOutput++;
					} catch (final InterruptedException e) {
						Thread.interrupted();
					}
				}
			}
			catch (final IOException e) {
				onRTerminated();
				return;
			}
			finally {
				if (locked) {
					streamLock.unlock();
					locked = false;
				}
				try {
					fProcessOutputReader.close();
				} catch (final IOException e1) {
				}
			}
		}
		
		private void onRTerminated() {
			markAsTerminated();
			RTermController.this.resume();
		}
	}
	
	private class UpdateProcessIdTask implements IToolRunnable {
		
		
		public UpdateProcessIdTask() {
		}
		
		
		public String getTypeId() {
			return "r/rterm/fetch-process-id"; //$NON-NLS-1$
		}
		
		public SubmitType getSubmitType() {
			return SubmitType.OTHER;
		}
		
		public String getLabel() {
			return "Fetch Process Id";
		}
		
		public void run(final IToolRunnableControllerAdapter adapter, final IProgressMonitor monitor)
				throws InterruptedException, CoreException {
			final StringBuilder output = readOutputLine("Sys.getpid()", monitor); //$NON-NLS-1$
			if (output != null) {
				final Matcher matcher = INT_OUTPUT_PATTERN.matcher(output);
				if (matcher.find()) {
					final String idString = matcher.group(1);
					if (idString != null) {
						try {
							fProcessId = Long.valueOf(idString);
						}
						catch (final NumberFormatException e) {
							fProcessId = null;
						}
					}
					else {
						fProcessId = null;
					}
				}
			}
		}
		
		public void changed(final int event, final ToolProcess process) {
		}
		
	}
	
	
	private ProcessBuilder fConfig;
	private Charset fCharset;
	private Process fProcess;
	private OutputStreamWriter fProcessInputWriter;
	private BufferedInputStream fProcessOutputBuffer;
	private InputStreamReader fProcessOutputReader;
	private ReadThread fProcessOutputThread;
	Long fProcessId;
	
	
	public RTermController(final ToolProcess process, final ProcessBuilder config, final Charset charset) {
		super(process, null);
		fConfig = config;
		fCharset = charset;
		
		fWorkspaceData = new RWorkspace(this) {
			@Override
			protected void refreshFromTool(final AbstractRController controller, final int options, final IProgressMonitor monitor) throws CoreException {
				if ((options & RWorkspace.REFRESH_COMPLETE) != 0 || (options & RWorkspace.REFRESH_AUTO) == 0) {
					final StringBuilder output = readOutputLine("getwd()", monitor); //$NON-NLS-1$
					if (output != null) {
						final Matcher matcher = STRING_OUTPUT_PATTERN.matcher(output);
						if (matcher.find()) {
							final String wd = matcher.group(1);
							setWorkspaceDir(EFS.getLocalFileSystem().getStore(new Path(wd)));
						}
					}
				}
				fChanged = 0;
				fChangedEnvirs.clear();
			}
		};
		setWorkspaceDir(EFS.getLocalFileSystem().fromLocalFile(config.directory()));
		initRunnableAdapter();
	}
	
	@Override
	protected IToolRunnable createStartRunnable() {
		return new StartRunnable() {
			@Override
			public String getLabel() {
				return RNicoMessages.Rterm_StartTask_label;
			}
		};
	}
	
	@Override
	protected void startTool(final IProgressMonitor monitor) throws CoreException {
		OutputStream processInput = null;
		InputStream processOutput;
		try {
			fConfig.redirectErrorStream(true);
			fProcess = fConfig.start();
			processOutput = fProcess.getInputStream();
			if (processOutput instanceof BufferedInputStream) {
				fProcessOutputBuffer = (BufferedInputStream) processOutput;
			}
			fProcessOutputReader = new InputStreamReader(processOutput, fCharset);
			fProcessOutputThread = new ReadThread();
			fProcessOutputThread.start();
			processInput = fProcess.getOutputStream();
			fProcessInputWriter = new OutputStreamWriter(processInput, fCharset);
			setCurrentPrompt(fDefaultPrompt);
			
			final List<IStatus> warnings = new ArrayList<IStatus>();
			
			initTracks(fConfig.directory().toString(), monitor, warnings);
			
			submit(new UpdateProcessIdTask());
			if (fStartupSnippet != null && fStartupSnippet.length() > 0) {
				submit(RUtil.LINE_SEPARATOR_PATTERN.split(fStartupSnippet), SubmitType.TOOLS);
			}
			fStartupSnippet = null;
			
			scheduleControllerRunnable(new IToolRunnable() {
				public SubmitType getSubmitType() {
					return SubmitType.OTHER;
				}
				public String getTypeId() {
					return "r/rj/start2"; //$NON-NLS-1$
				}
				public String getLabel() {
					return "Finish Initialization";
				}
				public void changed(final int event, final ToolProcess process) {
				}
				public void run(final IToolRunnableControllerAdapter adapter, final IProgressMonitor monitor) throws InterruptedException, CoreException {
					for (final IStatus status : warnings) {
						handleStatus(status, monitor);
					}
				}
			});
		}
		catch (final IOException e) {
			if (processInput != null) {
				try {
					processInput.close();
				} catch (final IOException e1) {
				}
			}
			throw new CoreException(new Status(IStatus.ERROR, RCore.PLUGIN_ID, ICommonStatusConstants.LAUNCHING,
					RNicoMessages.RTerm_error_Starting_message, e));
		}
		
	}
	
	@Override
	protected void interruptTool(final int hardness) {
		runSendCtrlC();
		if (hardness == 0) {
			return;
		}
		getControllerThread().interrupt();
	}
	
	@Override
	protected void postCancelTask(final int options, final IProgressMonitor monitor) throws CoreException {
	}
	
	@Override
	protected void killTool(final IProgressMonitor monitor) {
		final Process p = fProcess;
		if (p != null) {
			p.destroy();
			fProcess = null;
		}
		markAsTerminated();
	}
	
	@Override
	protected boolean isToolAlive() {
		final Process p = fProcess;
		if (p != null) {
			try {
				p.exitValue();
			}
			catch (final IllegalThreadStateException e) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void clear() {
		fProcess = null;
		super.clear();
	}
	
	
	private boolean runSendCtrlC() {
		if (!Platform.getOS().startsWith("win") //$NON-NLS-1$
				|| getStatus() == ToolStatus.TERMINATED) {
			return false;
		}
		
		final IToolEventHandler handler = getEventHandler(IToolEventHandler.RUN_BLOCKING_EVENT_ID);
		if (handler != null) {
			final RTermCancelRunnable cancelRunnable = new RTermCancelRunnable();
			final Map<String, Object> data = Collections.singletonMap(IToolEventHandler.RUN_RUNNABLE_DATA_KEY, (Object) cancelRunnable); 
			return (handler.handle(IToolEventHandler.RUN_BLOCKING_EVENT_ID, this, data, null).isOK());
		}
		return false;
	}
	
	
//-- RunnabelAdapter
	
	@Override
	protected void doBeforeSubmit() {
		// adds control stream
		// without prompt
		final SubmitType type = fCurrentRunnable.getSubmitType();
		try {
			fProcessOutputThread.streamLock.lock();
			fInputStream.append(fCurrentInput, type,
					(fCurrentPrompt.meta & IToolRunnableControllerAdapter.META_HISTORY_DONTADD) );
			fInputStream.append(fWorkspaceData.getLineSeparator(), type,
					IToolRunnableControllerAdapter.META_HISTORY_DONTADD);
		}
		finally {
			fProcessOutputThread.streamLock.unlock();
		}
	}
	
	@Override
	protected void doSubmit(final IProgressMonitor monitor) {
		monitor.subTask(fDefaultPrompt.text + " " + fCurrentInput);  //$NON-NLS-1$
		
		try {
			fProcessInputWriter.write(fCurrentInput + fLineSeparator);
			fProcessInputWriter.flush();
		}
		catch (final IOException e) {
			RCorePlugin.logError(-1, "Rterm IO error", e); //$NON-NLS-1$
			if (!isToolAlive()) {
				markAsTerminated();
				setCurrentPrompt(Prompt.NONE);
				return;
			}
		}
		
		try {
			Thread.sleep(fProcessOutputThread.SYNC_MS*2);
		} catch (final InterruptedException e) {
			Thread.interrupted();
		}
		fProcessOutputThread.streamLock.lock();
		fProcessOutputThread.streamLock.unlock();
		
		setCurrentPrompt(fDefaultPrompt);
	}
	
	public Pattern synch(final IProgressMonitor monitor) throws CoreException {
		final String stamp = "Synch"+System.nanoTime(); //$NON-NLS-1$
		final AtomicBoolean patternFound = new AtomicBoolean(false);
		final IStreamListener listener = new IStreamListener() {
			
			private String lastLine = ""; //$NON-NLS-1$
			
			public void streamAppended(final String text, final IStreamMonitor monitor) {
				if (text.contains(stamp)) {
					found();
					return;
				}
				final String[] lines = RUtil.LINE_SEPARATOR_PATTERN.split(text, -1);
				if ((lastLine + lines[0]).contains(stamp)) {
					found();
					return;
				}
				lastLine = lines[lines.length-1];
			}
			
			private void found() {
				fDefaultOutputStream.removeListener(this);
				patternFound.set(true);
			}
			
		};
		try {
			fDefaultOutputStream.addListener(listener);
			submitToConsole("cat(\""+stamp+"\\n\");", monitor); //$NON-NLS-1$ //$NON-NLS-2$
			while (!patternFound.get()) {
				if (monitor.isCanceled()) {
					throw cancelTask();
				}
				try {
					Thread.sleep(50);
				}
				catch (final InterruptedException e) {
					Thread.interrupted();
				}
			}
			return Pattern.compile("(?:"+Pattern.quote(getWorkspaceData().getDefaultPrompt().text) + ")?"+stamp); //$NON-NLS-1$ //$NON-NLS-2$
		}
		finally {
			fDefaultOutputStream.removeListener(listener);
		}
	}
	
	private StringBuilder readOutputLine(final String command, final IProgressMonitor monitor) throws CoreException {
		final StringBuilder output = new StringBuilder();
		final AtomicBoolean patternFound = new AtomicBoolean(false);
		final IStreamListener listener = new IStreamListener() {
			
			public void streamAppended(final String text, final IStreamMonitor monitor) {
				final Matcher matcher = RUtil.LINE_SEPARATOR_PATTERN.matcher(text);
				if (matcher.find()) {
					output.append(text.substring(0, matcher.start()));
					found();
				}
				else {
					output.append(text);
				}
			}
			
			private void found() {
				fDefaultOutputStream.removeListener(this);
				patternFound.set(true);
			}
			
		};
		synch(monitor);
		try {
			fDefaultOutputStream.addListener(listener);
			if (monitor.isCanceled()) {
				return null;
			}
			submitToConsole(command, monitor);
			while (!patternFound.get()) {
				if (monitor.isCanceled()) {
					throw cancelTask();
				}
				try {
					Thread.sleep(50);
				}
				catch (final InterruptedException e) {
					Thread.interrupted();
				}
			}
			return output;
		}
		finally {
			fDefaultOutputStream.removeListener(listener);
		}
	}
	
}
