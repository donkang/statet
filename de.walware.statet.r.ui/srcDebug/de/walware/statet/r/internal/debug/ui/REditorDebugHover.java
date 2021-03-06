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

package de.walware.statet.r.internal.debug.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;

import de.walware.ecommons.ConstList;
import de.walware.ecommons.ltk.IModelManager;
import de.walware.ecommons.ltk.ISourceUnit;
import de.walware.ecommons.ltk.ISourceUnitModelInfo;
import de.walware.ecommons.ltk.ast.AstSelection;
import de.walware.ecommons.ltk.ui.sourceediting.ISourceEditor;
import de.walware.ecommons.text.TextUtil;

import de.walware.statet.nico.core.runtime.IToolRunnable;
import de.walware.statet.nico.core.runtime.IToolRunnableControllerAdapter;
import de.walware.statet.nico.core.runtime.Queue;
import de.walware.statet.nico.core.runtime.SubmitType;
import de.walware.statet.nico.core.runtime.ToolController;
import de.walware.statet.nico.core.runtime.ToolProcess;
import de.walware.statet.nico.ui.NicoUITools;

import de.walware.rj.data.RList;
import de.walware.rj.data.RObject;
import de.walware.rj.data.RStore;
import de.walware.rj.data.defaultImpl.RListImpl;

import de.walware.statet.r.core.data.ICombinedRElement;
import de.walware.statet.r.core.model.RElementAccess;
import de.walware.statet.r.core.model.RElementName;
import de.walware.statet.r.core.model.RModel;
import de.walware.statet.r.core.rsource.IRDocumentPartitions;
import de.walware.statet.r.core.rsource.RHeuristicTokenScanner;
import de.walware.statet.r.core.rsource.ast.RAstNode;
import de.walware.statet.r.internal.ui.editors.ISourceEditorHover;
import de.walware.statet.r.nico.IRCombinedDataAdapter;
import de.walware.statet.r.nico.IRDataAdapter;
import de.walware.statet.r.nico.RTool;


public class REditorDebugHover implements ISourceEditorHover {
	
	
	private static class RUpdater implements IToolRunnable {
		
		
		private RElementName fElementRef;
		private boolean fCancelled;
		
		private ICombinedRElement fElementStruct;
		private RList fElementAttr;
		private String fElementDetailTitle;
		private String fElementDetailInfo;
		
		
		public RUpdater(final RElementName elementRef) {
			fElementRef = elementRef;
		}
		
		
		public SubmitType getSubmitType() {
			return SubmitType.OTHER;
		}
		
		public String getTypeId() {
			return "reditor/hover";
		}
		
		public String getLabel() {
			return "Element Detail for Hover";
		}
		
		public void changed(final int event, final ToolProcess process) {
			if (event == Queue.ENTRIES_DELETE || event == Queue.ENTRIES_ABANDONED) {
				synchronized (this) {
					this.notifyAll();
				}
			}
		}
		
		public void run(final IToolRunnableControllerAdapter adapter, final IProgressMonitor monitor) throws InterruptedException, CoreException {
			try {
				final IRDataAdapter r = (IRDataAdapter) adapter;
				if (fCancelled || monitor.isCanceled()) {
					throw new CoreException(Status.CANCEL_STATUS);
				}
				if (fElementRef.getNamespace() == null) {
					fElementRef = checkName(r, monitor);
				}
				if (fElementRef == null) {
					return;
				}
				final String name = RElementName.createDisplayName(fElementRef, RElementName.DISPLAY_NS_PREFIX);
				if (r instanceof IRCombinedDataAdapter) {
					final IRCombinedDataAdapter r2 = (IRCombinedDataAdapter) r;
					final String cmd = name;
					fElementStruct = r2.evalCombinedStruct(cmd, 0, 1, fElementRef, monitor);
				}
				{	final String cmd = "class("+name+")";
					final RObject robject = r.evalData(cmd, monitor);
					if (robject != null) {
						fElementAttr = new RListImpl(new RObject[] { robject }, null, new String[] { "class" });
					}
				}
				{	final String title = "str";
					final String cmd = ".statet.captureStr("+name+")";
					final RObject robject = r.evalData(cmd, monitor);
					
					if ((robject != null)
							&& (robject.getRObjectType() == RObject.TYPE_VECTOR)
							&& (robject.getData().getStoreType() == RStore.CHARACTER)) {
						final RStore data = robject.getData();
						final StringBuilder sb = new StringBuilder(data.getLength()*30);
						final String ln = TextUtil.getPlatformLineDelimiter();
						for (int i = 0; i < data.getLength(); i++) {
							if (!data.isNA(i)) {
								sb.append(data.getChar(i));
								sb.append(ln);
							}
						}
						if (sb.length() > 0) {
							sb.setLength(sb.length()-ln.length());
						}
						fElementDetailTitle = title;
						fElementDetailInfo = sb.toString();
					}
				}
			}
			finally {
				synchronized (this) {
					this.notifyAll();
				}
			}
		}
		
		private RElementName checkName(final IRDataAdapter r, final IProgressMonitor monitor) throws CoreException {
			final RElementName mainName = RElementName.cloneSegment(fElementRef);
			final String name = mainName.getDisplayName();
			if (name != null) {
				final RObject found = r.evalData("find(\""+name+"\")", monitor);
				if (found != null && found.getRObjectType() == RObject.TYPE_VECTOR && found.getData().getStoreType() == RStore.CHARACTER
						&& found.getLength() > 0) {
					final List<RElementName> segments = new ArrayList<RElementName>();
					segments.add(RElementName.create(RElementName.MAIN_SEARCH_ENV, found.getData().getChar(0)));
					RElementName a = fElementRef;
					while (a != null) {
						segments.add(a);
						a = a.getNextSegment();
					}
					return RElementName.concat(segments);
				}
			}
			return null;
		}
		
	}
	
	public static Object getElementDetail(final RElementName name, final Control control, final IWorkbenchPart part) {
		if (name == null || part == null) {
			return null;
		}
		final ToolProcess process = NicoUITools.getTool(part);
		final ToolController controller = NicoUITools.getController(RTool.TYPE, RTool.R_DATA_FEATURESET_ID, process);
		if (controller == null) {
			return null;
		}
		
		final RUpdater rTask = new RUpdater(name);
		try {
			synchronized (rTask) {
				controller.submit(rTask);
				rTask.wait();
			}
		}
		catch (final InterruptedException e) {
			if (Thread.interrupted()) { // interrupted by hover controller
				rTask.fCancelled = true;
				process.getQueue().removeElements(new Object[] { rTask });
				return null;
			}
		}
		if (rTask.fElementStruct != null) {
			return new RElementInfoHoverCreator.Data(control, rTask.fElementStruct, rTask.fElementAttr,
					rTask.fElementDetailTitle, rTask.fElementDetailInfo);
		}
		return null;
	}
	
	
	private RHeuristicTokenScanner fScanner;
	private ISourceEditor fEditor;
	
	private IInformationControlCreator fControlCreator;
	
	
	public REditorDebugHover() {
	}
	
	
	private void init() {
		if (fScanner == null) {
			fScanner = new RHeuristicTokenScanner();
			fControlCreator = new RElementInfoHoverCreator();
		}
	}
	
	public void setEditor(final ISourceEditor editor) {
		fEditor = editor;
	}
	
	public IRegion getHoverRegion(final int offset) {
		init();
		try {
			final IDocument document = fEditor.getViewer().getDocument();
			fScanner.configure(document);
			final IRegion word = fScanner.findRWord(offset, false, true);
			if (word != null) {
				final ITypedRegion partition = fScanner.getPartition(word.getOffset());
				if (fScanner.getPartitioningConfig().getDefaultPartitionConstraint().matches(partition.getType())
						|| partition.getType() == IRDocumentPartitions.R_STRING
						|| partition.getType() == IRDocumentPartitions.R_QUOTED_SYMBOL) {
					return word;
				}
			}
			final char c = document.getChar(offset);
			if (c == '[') {
				final ITypedRegion partition = fScanner.getPartition(offset);
				if (fScanner.getPartitioningConfig().getDefaultPartitionConstraint().matches(partition.getType())) {
					return new Region(offset, 1);
				}
			}
		}
		catch (final Exception e) {
			// TODO
			e.printStackTrace();
		}
		return null;
	}
	
	public Object getHoverInfo(final IRegion hoverRegion) {
		try {
			// we are not in UI thread
			final ISourceUnit su = fEditor.getSourceUnit();
			if (su == null) {
				return null;
			}
			final ISourceUnitModelInfo modelInfo = su.getModelInfo(RModel.TYPE_ID, IModelManager.MODEL_FILE, new NullProgressMonitor());
			if (modelInfo == null) {
				return null;
			}
			final AstSelection astSelection = AstSelection.search(modelInfo.getAst().root, hoverRegion.getOffset(), hoverRegion.getOffset()+hoverRegion.getLength(), AstSelection.MODE_COVERING_SAME_LAST);
			RAstNode node = (RAstNode) astSelection.getCovering();
			if (node != null) {
				RElementAccess access = null;
				while (node != null && access == null) {
					if (Thread.interrupted()) {
						return null;
					}
					final Object[] attachments = node.getAttachments();
					for (int i = 0; i < attachments.length; i++) {
						if (attachments[i] instanceof RElementAccess) {
							access = (RElementAccess) attachments[i];
							final RElementName e = getElementAccessOfRegion(access, hoverRegion);
							if (Thread.interrupted() || e == null) {
								return null;
							}
							return getElementDetail(e, fEditor.getViewer().getTextWidget(), fEditor.getWorkbenchPart());
						}
					}
					node = node.getRParent();
				}
			}
		}
		catch (final Exception e) {
			// TODO
			e.printStackTrace();
		}
		return null;
	}
	
	private RElementName getElementAccessOfRegion(final RElementAccess access, final IRegion region) {
		int segmentCount = 0;
		RElementAccess current = access;
		while (current != null) {
			segmentCount++;
			final RAstNode nameNode = current.getNameNode();
			if (nameNode != null
					&& (nameNode.getOffset() <= region.getOffset() && nameNode.getStopOffset() >= region.getOffset()+region.getLength()) ) {
				final RElementName[] segments = new RElementName[segmentCount];
				RElementAccess segment = access;
				for (int i = 0; i < segments.length; i++) {
					if (segment.getSegmentName() == null) {
						return null;
					}
					switch (segment.getType()) {
					case RElementName.MAIN_DEFAULT:
					case RElementName.SUB_NAMEDSLOT:
					case RElementName.SUB_NAMEDPART:
						segments[i] = segment;
						break;
					case RElementName.SUB_INDEXED_S:
					case RElementName.SUB_INDEXED_D:
						return null; // not yet supported
					case RElementName.MAIN_CLASS:
						if (segmentCount != 1) {
							return null;
						}
						segments[i] = segment;
						break;
					default:
//					case RElementName.MAIN_PACKAGE:
//					case RElementName.MAIN_ENV:
						return null;
					}
					segment = segment.getNextSegment();
				}
				return RElementName.concat(new ConstList<RElementName>(segments));
			}
			current = current.getNextSegment();
		}
		
		return null;
	}
	
	public IInformationControlCreator getHoverControlCreator() {
		return fControlCreator;
	}
	
}
