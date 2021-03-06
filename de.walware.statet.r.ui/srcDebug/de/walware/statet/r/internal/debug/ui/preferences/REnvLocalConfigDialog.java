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

package de.walware.statet.r.internal.debug.ui.preferences;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.ibm.icu.text.Collator;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import de.walware.ecommons.ConstList;
import de.walware.ecommons.databinding.jface.DatabindingSupport;
import de.walware.ecommons.debug.ui.LaunchConfigUtil;
import de.walware.ecommons.debug.ui.ProcessOutputCollector;
import de.walware.ecommons.ui.components.ButtonGroup;
import de.walware.ecommons.ui.components.ExtensibleTextCellEditor;
import de.walware.ecommons.ui.dialogs.ExtStatusDialog;
import de.walware.ecommons.ui.util.LayoutUtil;
import de.walware.ecommons.ui.util.MessageUtil;
import de.walware.ecommons.ui.util.ViewerUtil;
import de.walware.ecommons.ui.util.ViewerUtil.TreeComposite;
import de.walware.ecommons.ui.workbench.ResourceInputComposite;
import de.walware.ecommons.variables.core.VariableFilter;

import de.walware.statet.r.core.RUtil;
import de.walware.statet.r.core.renv.IREnvConfiguration;
import de.walware.statet.r.core.renv.IREnvConfiguration.Exec;
import de.walware.statet.r.core.renv.IRLibraryGroup;
import de.walware.statet.r.core.renv.IRLibraryLocation;
import de.walware.statet.r.core.renv.IRLibraryLocation.WorkingCopy;
import de.walware.statet.r.internal.ui.RUIPlugin;
import de.walware.statet.r.internal.ui.help.IRUIHelpContextIds;
import de.walware.statet.r.ui.RUI;


/**
 * Dialog for a local standard {@link IREnvConfiguration} (<code>user-local</code>)
 */
public class REnvLocalConfigDialog extends ExtStatusDialog {
	
	
	private static final Integer T_64 = Integer.valueOf(64);
	private static final Integer T_32 = Integer.valueOf(32);
	
	private static final String DETECT_START = "_R-Path-And-Library-Configuration_"; //$NON-NLS-1$
	private static final String DETECT_COMMAND = "cat('"+DETECT_START+"', "
			+ "Sys.getenv(\'R_HOME\'),"
			+ "paste(.Library, collapse=.Platform$path.sep),"
			+ "paste(.Library.site, collapse=.Platform$path.sep),"
			+ "Sys.getenv('R_LIBS'),"
			+ "Sys.getenv('R_LIBS_USER'),"
			+ "R.home('doc'),"
			+ "R.home('share'),"
			+ "R.home('include'),"
			+ "R.version$arch, "
			+ ".Platform$OS.type, "
			+ "sep='\\n');"; //$NON-NLS-1$ 
	// R.version$arch
	private static final int DETECT_LENGTH = 11;
	private static final int DETECT_R_HOME = 1;
	private static final int DETECT_R_DEFAULT = 2;
	private static final int DETECT_R_SITE = 3;
	private static final int DETECT_R_OTHER = 4;
	private static final int DETECT_R_USER = 5;
	private static final int DETECT_R_DOC_DIR = 6;
	private static final int DETECT_R_SHARE_DIR = 7;
	private static final int DETECT_R_INCLUDE_DIR = 8;
	private static final int DETECT_R_ARCH = 9;
	private static final int DETECT_R_OS = 10;
	private static final Pattern DETECT_ITEM_PATTERN = RUtil.LINE_SEPARATOR_PATTERN;
	private static final Pattern DETECT_PATH_PATTERN = Pattern.compile(Pattern.quote(File.pathSeparator));
	
	
	private class RHomeComposite extends ResourceInputComposite {
		
		public RHomeComposite(final Composite parent) {
			super (parent, 
					ResourceInputComposite.STYLE_TEXT,
					ResourceInputComposite.MODE_DIRECTORY | ResourceInputComposite.MODE_OPEN, 
					Messages.REnv_Detail_Location_label);
			setShowInsertVariable(true, new ConstList<VariableFilter>(
					VariableFilter.EXCLUDE_BUILD_FILTER,
					VariableFilter.EXCLUDE_INTERACTIVE_FILTER,
					VariableFilter.EXCLUDE_JAVA_FILTER ), null);
		}
		
		@Override
		protected void fillMenu(final Menu menu) {
			super.fillMenu(menu);
			
			final MenuItem item = new MenuItem(menu, SWT.PUSH);
			item.setText(Messages.REnv_Detail_Location_FindAuto_label);
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					final String[] rhome = searchRHOME();
					if (rhome != null) {
						setText(rhome[0], true);
						fRBitViewer.setSelection(new StructuredSelection(
								rhome[0].contains("64") ? T_64 : T_32)); //$NON-NLS-1$
						final String current = fNameControl.getText().trim();
						if ((current.length() == 0 || current.equals("R")) && rhome[1] != null) { //$NON-NLS-1$
							fNameControl.setText(rhome[1]);
						}
					}
					else {
						final String name = Messages.REnv_Detail_Location_label;
						MessageDialog.openInformation(getShell(), 
								MessageUtil.removeMnemonics(name), 
								NLS.bind(Messages.REnv_Detail_Location_FindAuto_Failed_message, name));
					}
					getTextControl().setFocus();
				}
			});
			
		}
	}
	
	private static class RLibraryContainer {
		
		IRLibraryGroup.WorkingCopy.WorkingCopy parent;
		IRLibraryLocation.WorkingCopy library;
		
		RLibraryContainer(final IRLibraryGroup.WorkingCopy.WorkingCopy parent, final IRLibraryLocation.WorkingCopy library) {
			this.parent = parent;
			this.library = library;
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (!(obj instanceof RLibraryContainer)) {
				return false;
			}
			final RLibraryContainer other = (RLibraryContainer) obj;
			return (library == other.library);
		}
	}
	
	
	private final IREnvConfiguration.WorkingCopy fConfigModel;
	private final boolean fIsNewConfig;
	private final Set<String> fExistingNames;
	
	private DatabindingSupport fDatabinding;
	
	private Text fNameControl;
	private ResourceInputComposite fRHomeControl;
	
	private Button fLoadButton;
	
	private ComboViewer fRBitViewer;
	
	private TreeViewer fRLibrariesViewer;
	private ButtonGroup<IRLibraryLocation.WorkingCopy> fRLibrariesButtons;
	
	private ResourceInputComposite fRDocDirectoryControl;
	private ResourceInputComposite fRShareDirectoryControl;
	private ResourceInputComposite fRIncludeDirectoryControl;
	
	
	public REnvLocalConfigDialog(final Shell parent, 
			final IREnvConfiguration.WorkingCopy config, final boolean isNewConfig, 
			final Collection<IREnvConfiguration> existingConfigs) {
		super(parent, true);
		
		fConfigModel = config;
		fIsNewConfig = isNewConfig;
		fExistingNames = new HashSet<String>();
		for (final IREnvConfiguration ec : existingConfigs) {
			fExistingNames.add(ec.getName());
		}
		setTitle(fIsNewConfig ?
				Messages.REnv_Detail_AddDialog_title : 
				Messages.REnv_Detail_Edit_Dialog_title );
	}
	
	
	@Override
	protected Control createDialogArea(final Composite parent) {
		final Composite dialogArea = new Composite(parent, SWT.NONE);
		dialogArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		dialogArea.setLayout(LayoutUtil.applyDialogDefaults(new GridLayout(), 2));
		
		{	// Name:
			final Label label = new Label(dialogArea, SWT.LEFT);
			label.setText(Messages.REnv_Detail_Name_label+':');
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			fNameControl = new Text(dialogArea, SWT.BORDER);
			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.widthHint = LayoutUtil.hintWidth(fNameControl, 60);
			fNameControl.setLayoutData(gd);
		}
		{	// Location:
			final Label label = new Label(dialogArea, SWT.LEFT);
			label.setText(Messages.REnv_Detail_Location_label+':');
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			fRHomeControl = new RHomeComposite(dialogArea);
			fRHomeControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		
		LayoutUtil.addSmallFiller(dialogArea, false);
		
		{	// Type (Bits):
			final Label label = new Label(dialogArea, SWT.LEFT);
			label.setText(Messages.REnv_Detail_Bits_label+':');
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			final Composite composite = new Composite(dialogArea, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			composite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 2));
			
			fRBitViewer = new ComboViewer(composite);
			fRBitViewer.setContentProvider(new ArrayContentProvider());
			fRBitViewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(final Object element) {
					return ((Integer) element).toString() + "-bit";  //$NON-NLS-1$
				}
			});
			fRBitViewer.setInput(new Integer[] { T_32, T_64 });
			
			fLoadButton = new Button(composite, SWT.PUSH);
			fLoadButton.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			fLoadButton.setText(Messages.REnv_Detail_DetectSettings_label);
			fLoadButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					detectSettings();
				}
			});
		}
		
		{	// Libraries:
			final Label label = new Label(dialogArea, SWT.LEFT);
			label.setText(Messages.REnv_Detail_Libraries_label+":"); //$NON-NLS-1$
			label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			final Composite composite = new Composite(dialogArea, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			composite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 2));
			
			final TreeComposite treeComposite = new ViewerUtil.TreeComposite(composite, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
			final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.widthHint = LayoutUtil.hintWidth(fNameControl, 80);
			gd.heightHint = LayoutUtil.hintHeight(treeComposite.tree, 10);
			treeComposite.setLayoutData(gd);
			fRLibrariesViewer = treeComposite.viewer;
			treeComposite.viewer.setContentProvider(new ITreeContentProvider() {
				public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
				}
				public void dispose() {
				}
				public Object[] getElements(final Object inputElement) {
					return fConfigModel.getRLibraryGroups().toArray();
				}
				public Object getParent(final Object element) {
					if (element instanceof RLibraryContainer) {
						return ((RLibraryContainer) element).parent;
					}
					return null;
				}
				public boolean hasChildren(final Object element) {
					if (element instanceof IRLibraryGroup.WorkingCopy) {
						return !((IRLibraryGroup.WorkingCopy) element).getLibraries().isEmpty();
					}
					return false;
				}
				public Object[] getChildren(final Object parentElement) {
					if (parentElement instanceof IRLibraryGroup.WorkingCopy) {
						final IRLibraryGroup.WorkingCopy group = (IRLibraryGroup.WorkingCopy) parentElement;
						final List<? extends IRLibraryLocation.WorkingCopy> libs = group.getLibraries();
						final RLibraryContainer[] array = new RLibraryContainer[libs.size()];
						for (int i = 0; i < libs.size(); i++) {
							array[i] = new RLibraryContainer(group, libs.get(i));
						}
						return array;
					}
					return null;
				}
			});
			final TreeViewerColumn column = treeComposite.addColumn(SWT.LEFT, new ColumnWeightData(100));
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(final ViewerCell cell) {
					final Object element = cell.getElement();
					if (element instanceof IRLibraryGroup.WorkingCopy) {
						final IRLibraryGroup.WorkingCopy group = (IRLibraryGroup.WorkingCopy) element;
						cell.setImage(RUI.getImage(RUI.IMG_OBJ_LIBRARY_GROUP));
						cell.setText(group.getLabel());
					}
					else if (element instanceof RLibraryContainer) {
						final IRLibraryLocation lib = ((RLibraryContainer) element).library;
						cell.setImage(RUI.getImage(RUI.IMG_OBJ_LIBRARY_LOCATION));
						cell.setText(lib.getDirectoryPath());
					} else {
						throw new UnsupportedOperationException();
					}
				}
			});
			column.setEditingSupport(new EditingSupport(treeComposite.viewer) {
				@Override
				protected boolean canEdit(final Object element) {
					if (element instanceof RLibraryContainer) {
						final IRLibraryGroup.WorkingCopy group = ((RLibraryContainer) element).parent;
						return !group.getId().equals(IRLibraryGroup.R_DEFAULT);
					}
					return false;
				}
				@Override
				protected void setValue(final Object element, final Object value) {
					final RLibraryContainer container = (RLibraryContainer) element;
					container.library.setDirectoryPath((String) value);
					
					getViewer().refresh(container, true);
					fDatabinding.updateStatus();
				}
				@Override
				protected Object getValue(final Object element) {
					final RLibraryContainer container = (RLibraryContainer) element;
					return container.library.getDirectoryPath();
				}
				@Override
				protected CellEditor getCellEditor(final Object element) {
					return new ExtensibleTextCellEditor(treeComposite.tree) {
						@Override
						protected Control createCustomControl(final Composite parent) {
							final ResourceInputComposite chooseResourceComposite = new ResourceInputComposite(parent,
									ResourceInputComposite.STYLE_TEXT,
									ResourceInputComposite.MODE_DIRECTORY | ResourceInputComposite.MODE_OPEN,
									Messages.REnv_Detail_LibraryLocation_label) {
								@Override
								protected void beforeMenuAction() {
									getFocusGroup().discontinueTracking();
								}
								@Override
								protected void afterMenuAction() {
									getFocusGroup().continueTracking();
								}
							};
							chooseResourceComposite.setShowInsertVariable(true, new ConstList<VariableFilter>(
									VariableFilter.EXCLUDE_BUILD_FILTER,
									VariableFilter.EXCLUDE_INTERACTIVE_FILTER,
									VariableFilter.EXCLUDE_JAVA_FILTER ), null);
							fText = (Text) chooseResourceComposite.getTextControl();
							return chooseResourceComposite;
						}
					};
				}
			});
			treeComposite.viewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
			treeComposite.viewer.setInput(fConfigModel);
			ViewerUtil.installDefaultEditBehaviour(treeComposite.viewer);
			
			fRLibrariesButtons = new ButtonGroup<IRLibraryLocation.WorkingCopy>(composite) {
				private IRLibraryGroup.WorkingCopy getGroup(final Object element) {
					if (element instanceof IRLibraryGroup.WorkingCopy) {
						return (IRLibraryGroup.WorkingCopy) element;
					}
					else {
						return ((RLibraryContainer) element).parent;
					}
				}
				@Override
				protected IRLibraryLocation.WorkingCopy getModelItem(final Object element) {
					if (element instanceof RLibraryContainer) {
						return ((RLibraryContainer) element).library;
					}
					return (IRLibraryLocation.WorkingCopy) element;
				}
				@Override
				protected Object getViewerElement(final IRLibraryLocation.WorkingCopy item, final Object parent) {
					return new RLibraryContainer((IRLibraryGroup.WorkingCopy.WorkingCopy) parent, item);
				}
				@Override
				protected boolean isAddAllowed(final Object element) {
					return !getGroup(element).getId().equals(IRLibraryGroup.R_DEFAULT);
				}
				@Override
				protected boolean isModifyAllowed(final Object element) {
					return ( element instanceof RLibraryContainer
							&& !getGroup(element).getId().equals(IRLibraryGroup.R_DEFAULT) );
				}
				@Override
				protected Object getAddParent(final Object element) {
					return getGroup(element);
				}
				@Override
				protected List<? super IRLibraryLocation.WorkingCopy> getChildContainer(final Object element) {
					if (element instanceof IRLibraryGroup.WorkingCopy) {
						return ((IRLibraryGroup.WorkingCopy) element).getLibraries();
					}
					else {
						return ((RLibraryContainer) element).parent.getLibraries();
					}
				}
				@Override
				protected IRLibraryLocation.WorkingCopy edit1(final IRLibraryLocation.WorkingCopy item, final boolean newItem, final Object parent) {
					if (newItem) {
						return ((IRLibraryGroup.WorkingCopy) parent).newLibrary(""); //$NON-NLS-1$
					}
					return item;
				}
				@Override
				public void updateState() {
					super.updateState();
					if (isDirty()) {
						fDatabinding.updateStatus();
					}
				}
			};
			fRLibrariesButtons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
			fRLibrariesButtons.addAddButton();
			fRLibrariesButtons.addDeleteButton();
//			fRLibrariesButtons.addSeparator();
//			fRLibrariesButtons.addUpButton();
//			fRLibrariesButtons.addDownButton();
			
			fRLibrariesButtons.connectTo(fRLibrariesViewer, null, null);
		}
		
		final Composite installGroup = createInstallDirGroup(dialogArea);
		if (installGroup != null) {
			installGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		}
		
		LayoutUtil.addSmallFiller(dialogArea, true);
		applyDialogFont(dialogArea);
		
		fDatabinding = new DatabindingSupport(dialogArea);
		addBindings(fDatabinding, fDatabinding.getRealm());
		fDatabinding.installStatusListener(new StatusUpdater());
		fRLibrariesButtons.updateState();
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), IRUIHelpContextIds.R_ENV);
		
		return dialogArea;
	}
	
	private Composite createInstallDirGroup(final Composite parent) {
		final Group composite = new Group(parent, SWT.NONE);
		composite.setLayout(LayoutUtil.applyGroupDefaults(new GridLayout(), 2));
		composite.setText("Advanced - Installation locations:");
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Documentation ('R_DOC_DIR'):");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			final ResourceInputComposite text = new ResourceInputComposite(composite, ResourceInputComposite.STYLE_TEXT,
					(ResourceInputComposite.MODE_DIRECTORY | ResourceInputComposite.MODE_OPEN), "R_DOC_DIR");
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			text.setShowInsertVariable(true, new ConstList<VariableFilter>(
					VariableFilter.EXCLUDE_BUILD_FILTER,
					VariableFilter.EXCLUDE_INTERACTIVE_FILTER,
					VariableFilter.EXCLUDE_JAVA_FILTER ), null);
			fRDocDirectoryControl = text;
		}
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Shared files ('R_SHARE_DIR'):");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			final ResourceInputComposite text = new ResourceInputComposite(composite, ResourceInputComposite.STYLE_TEXT,
					(ResourceInputComposite.MODE_DIRECTORY | ResourceInputComposite.MODE_OPEN), "R_SHARE_DIR");
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			text.setShowInsertVariable(true, new ConstList<VariableFilter>(
					VariableFilter.EXCLUDE_BUILD_FILTER,
					VariableFilter.EXCLUDE_INTERACTIVE_FILTER,
					VariableFilter.EXCLUDE_JAVA_FILTER ), null);
			fRShareDirectoryControl = text;
		}
		{	final Label label = new Label(composite, SWT.NONE);
			label.setText("Include files ('R_INCLUDE_DIR'):");
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			
			final ResourceInputComposite text = new ResourceInputComposite(composite, ResourceInputComposite.STYLE_TEXT,
					(ResourceInputComposite.MODE_DIRECTORY | ResourceInputComposite.MODE_OPEN), "R_INCLUDE_DIR");
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			text.setShowInsertVariable(true, new ConstList<VariableFilter>(
					VariableFilter.EXCLUDE_BUILD_FILTER,
					VariableFilter.EXCLUDE_INTERACTIVE_FILTER,
					VariableFilter.EXCLUDE_JAVA_FILTER ), null);
			fRIncludeDirectoryControl = text;
		}
		return composite;
	}
	
	protected void addBindings(final DatabindingSupport db, final Realm realm) {
		db.getContext().bindValue(SWTObservables.observeText(fNameControl, SWT.Modify), 
				BeansObservables.observeValue(fConfigModel, IREnvConfiguration.PROP_NAME), 
				new UpdateValueStrategy().setAfterGetValidator(new IValidator() {
					public IStatus validate(final Object value) {
						String s = (String) value;
						s = s.trim();
						if (s.length() == 0) {
							return ValidationStatus.error(Messages.REnv_Detail_Name_error_Missing_message);
						}
						if (fExistingNames.contains(s)) {
							return ValidationStatus.error(Messages.REnv_Detail_Name_error_Duplicate_message);
						}
						if (s.contains("/")) {  //$NON-NLS-1$
							return ValidationStatus.error(Messages.REnv_Detail_Name_error_InvalidChar_message);
						}
						return ValidationStatus.ok();
					}
				}), null);
		final Binding rHomeBinding = db.getContext().bindValue(fRHomeControl.getObservable(), 
				BeansObservables.observeValue(fConfigModel, IREnvConfiguration.PROP_RHOME), 
				new UpdateValueStrategy().setAfterGetValidator(new IValidator() {
					public IStatus validate(final Object value) {
						final IStatus status = fRHomeControl.getValidator().validate(value);
						if (!status.isOK()) {
							return status;
						}
						if (!fConfigModel.isValidRHomeLocation(fRHomeControl.getResourceAsFileStore())) {
							return ValidationStatus.error(Messages.REnv_Detail_Location_error_NoRHome_message);
						}
						return ValidationStatus.ok();
					}
				}), null);
		rHomeBinding.getValidationStatus().addValueChangeListener(new IValueChangeListener() {
			public void handleValueChange(final ValueChangeEvent event) {
				final IStatus status = (IStatus) event.diff.getNewValue();
				fLoadButton.setEnabled(status.isOK());
			}
		});
		rHomeBinding.validateTargetToModel();
		db.getContext().bindValue(ViewersObservables.observeSingleSelection(fRBitViewer),
				BeansObservables.observeValue(fConfigModel, IREnvConfiguration.PROP_RBITS),
				null, null);
		db.getContext().bindValue(fRDocDirectoryControl.getObservable(),
				BeansObservables.observeValue(fConfigModel, IREnvConfiguration.PROP_RDOC_DIRECTORY) );
		db.getContext().bindValue(fRShareDirectoryControl.getObservable(),
				BeansObservables.observeValue(fConfigModel, IREnvConfiguration.PROP_RSHARE_DIRECTORY) );
		db.getContext().bindValue(fRIncludeDirectoryControl.getObservable(),
				BeansObservables.observeValue(fConfigModel, IREnvConfiguration.PROP_RINCLUDE_DIRECTORY) );
	}
	
	private String[] searchRHOME() {
		try {
			final IStringVariableManager variables = VariablesPlugin.getDefault().getStringVariableManager();
			
			String loc = variables.performStringSubstitution("${env_var:R_HOME}", false); //$NON-NLS-1$
			if (loc != null && loc.length() > 0) {
				if (EFS.getLocalFileSystem().getStore(
						new Path(loc)).fetchInfo().exists()) {
					return new String[] { loc, Messages.REnv_SystemRHome_name };
				}
			}
			if (Platform.getOS().startsWith("win")) { //$NON-NLS-1$
				loc = "${env_var:PROGRAMFILES}\\R";  //$NON-NLS-1$
				final IFileStore res = EFS.getLocalFileSystem().getStore(
						new Path(variables.performStringSubstitution(loc)));
				if (!res.fetchInfo().exists()) {
					return null;
				}
				final String[] childNames = res.childNames(EFS.NONE, null);
				Arrays.sort(childNames, 0, childNames.length, Collator.getInstance());
				for (int i = childNames.length-1; i >= 0; i--) {
					if (fConfigModel.isValidRHomeLocation(res.getChild(childNames[i]))) {
						return new String[] { loc + '\\' + childNames[i], childNames[i] };
					}
				}
			}
			else if (Platform.getOS().equals(Platform.OS_MACOSX)) {
				loc = "/Library/Frameworks/R.framework/Resources";  //$NON-NLS-1$
				if (fConfigModel.isValidRHomeLocation(EFS.getLocalFileSystem().getStore(new Path(loc)))) {
					return new String[] { loc, null };
				}
			}
			else {
				final String[] defLocations = new String[] {
						"/usr/lib/R", //$NON-NLS-1$
						"/usr/lib64/R", //$NON-NLS-1$
				};
				for (int i = 0; i < defLocations.length; i++) {
					loc = defLocations[i];
					if (fConfigModel.isValidRHomeLocation(EFS.getLocalFileSystem().getStore(new Path(loc)))) {
						return new String[] { loc, null };
					}
				}
			}
		}
		catch (final Exception e) {
			RUIPlugin.logError(-1, "Error when searching R_HOME location", e); //$NON-NLS-1$
		}
		return null;
	}
	
	private void detectSettings() {
		try {
			run(true, true, new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						detectSettings(monitor);
					}
					catch (final CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		catch (final InvocationTargetException e) {
			final String message = (e.getCause() instanceof CoreException) ?
					Messages.REnv_Detail_DetectSettings_error_message :
					Messages.REnv_Detail_DetectSettings_error_Unexpected_message;
			StatusManager.getManager().handle(new Status(IStatus.ERROR, RUI.PLUGIN_ID, -1,
					message, e), StatusManager.LOG);
			StatusManager.getManager().handle(new Status(IStatus.ERROR, RUI.PLUGIN_ID, -1,
					message, e.getCause()), StatusManager.SHOW);
		}
		catch (final InterruptedException e) {
		}
		fRLibrariesButtons.refresh();
		fRLibrariesViewer.expandAll();
	}
	
	private void detectSettings(final IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(Messages.REnv_Detail_DetectSettings_task, 10);
		
		final ProcessBuilder processBuilder = new ProcessBuilder(fConfigModel.getExecCommand(Exec.TERM));
		processBuilder.command().add("--no-save"); //$NON-NLS-1$
		processBuilder.command().add("--slave"); //$NON-NLS-1$
		processBuilder.command().add("-e"); //$NON-NLS-1$
		processBuilder.command().add(DETECT_COMMAND);
		
		final Map<String, String> envp = processBuilder.environment();
		LaunchConfigUtil.configureEnvironment(envp, null, fConfigModel.getEnvironmentsVariables(false));
		
		monitor.worked(1);
		
		final ProcessOutputCollector reader = new ProcessOutputCollector(processBuilder, "'Detect R settings'", monitor); //$NON-NLS-1$
		final String output = reader.collect();
		final int start = output.indexOf(DETECT_START);
		if (start >= 0) {
			final String[] lines = DETECT_ITEM_PATTERN.split(output.substring(start));
			if (lines.length == DETECT_LENGTH) {
				updateLibraries(fConfigModel.getRLibraryGroup(IRLibraryGroup.R_DEFAULT),
						lines[DETECT_R_DEFAULT], lines[DETECT_R_HOME]);
				
				final IRLibraryGroup.WorkingCopy.WorkingCopy group = fConfigModel.getRLibraryGroup(IRLibraryGroup.R_SITE);
				updateLibraries(group, lines[DETECT_R_SITE], lines[DETECT_R_HOME]);
				if (group.getLibraries().isEmpty()) {
					group.getLibraries().add(group.newLibrary(IRLibraryGroup.DEFAULTLOCATION_R_SITE));
				}
				updateLibraries(fConfigModel.getRLibraryGroup(IRLibraryGroup.R_OTHER),
						lines[DETECT_R_OTHER], lines[DETECT_R_HOME]);
				updateLibraries(fConfigModel.getRLibraryGroup(IRLibraryGroup.R_USER),
						lines[DETECT_R_USER], lines[DETECT_R_HOME]);
				
				fConfigModel.setRDocDirectory(checkDir(lines[DETECT_R_DOC_DIR], lines[DETECT_R_HOME]));
				fConfigModel.setRShareDirectory(checkDir(lines[DETECT_R_SHARE_DIR], lines[DETECT_R_HOME]));
				fConfigModel.setRIncludeDirectory(checkDir(lines[DETECT_R_INCLUDE_DIR], lines[DETECT_R_HOME]));
				
				if (lines[DETECT_R_ARCH].endsWith("86")) {
					fConfigModel.setRBits(32);
				}
				else if (lines[DETECT_R_ARCH].endsWith("64")) {
					fConfigModel.setRBits(64);
				}
				fConfigModel.setROS(lines[DETECT_R_OS]);
				return;
			}
		}
		throw new CoreException(new Status(IStatus.ERROR, RUI.PLUGIN_ID, -1,
				"Unexpected output:\n" + output, null)); //$NON-NLS-1$
	}
	
	private void updateLibraries(final IRLibraryGroup.WorkingCopy.WorkingCopy group, final String var, final String rHome) {
		final List<WorkingCopy> libraries = group.getLibraries();
		libraries.clear();
		final String[] locations = DETECT_PATH_PATTERN.split(var);
		final IPath rHomePath = new Path(rHome);
		final IPath userHomePath = new Path(System.getProperty("user.home")); //$NON-NLS-1$
		for (final String location : locations) {
			if (location.length() == 0) {
				continue;
			}
			String s;
			final IPath path;
			if (location.startsWith("~/")) {
				path = userHomePath.append(location.substring(2));
			}
			else {
				path = new Path(location);
			}
			if (rHomePath.isPrefixOf(path)) {
				s = "${env_var:R_HOME}/" + path.makeRelativeTo(rHomePath).toString(); //$NON-NLS-1$
			}
			else if (userHomePath.isPrefixOf(path)) {
				s = "${system_property:user.home}/" + path.makeRelativeTo(userHomePath).toString(); //$NON-NLS-1$
			}
			else {
				s = path.toString();
			}
			libraries.add(group.newLibrary(s));
		}
	}
	
	private String checkDir(String dir, final String rHome) {
		if (dir != null && dir.length() > 0) {
			final IPath rHomePath = new Path(rHome);
			final IPath path = new Path(dir);
			if (rHomePath.isPrefixOf(path)) {
				dir = "${env_var:R_HOME}/" + path.makeRelativeTo(rHomePath).toString(); //$NON-NLS-1$
			}
			return dir;
		}
		return null;
	}
	
}
