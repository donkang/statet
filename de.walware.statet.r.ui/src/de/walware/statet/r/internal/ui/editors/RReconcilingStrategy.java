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

package de.walware.statet.r.internal.ui.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ui.sourceediting.EcoReconciler2.ISourceUnitStrategy;

import de.walware.statet.r.core.model.IRSourceUnit;
import de.walware.statet.r.internal.ui.RUIPlugin;


public class RReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension, ISourceUnitStrategy {
	
	
	private RDocumentProvider fDocumentProvider;
	private ISourceUnit fInput;
	private IProgressMonitor fMonitor;
	
	
	public RReconcilingStrategy() {
		fDocumentProvider = RUIPlugin.getDefault().getRDocumentProvider();
	}
	
	public void initialReconcile() {
		reconcile();
	}
	
	public void setDocument(final IDocument document) {
	}
	
	public void setInput(final ISourceUnit input) {
		fInput = input;
	}
	
	public void reconcile(final IRegion partition) {
		reconcile();
	}
	
	public void reconcile(final DirtyRegion dirtyRegion, final IRegion subRegion) {
		reconcile();
	}
	
	public void setProgressMonitor(final IProgressMonitor monitor) {
		fMonitor = monitor;
	}
	
	
	protected void reconcile() {
		final ISourceUnit su = fInput;
		if (!(su instanceof IRSourceUnit) || fMonitor.isCanceled()) {
			return;
		}
		((IRSourceUnit) su).reconcileRModel(IModelManager.MODEL_FILE, fMonitor);
	}
	
}
