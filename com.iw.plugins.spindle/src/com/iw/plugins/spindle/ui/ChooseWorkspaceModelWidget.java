package com.iw.plugins.spindle.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.iw.plugins.spindle.TapestryImages;
import com.iw.plugins.spindle.TapestryPlugin;
import com.iw.plugins.spindle.model.ITapestryModel;
import com.iw.plugins.spindle.util.lookup.ILookupRequestor;
import com.iw.plugins.spindle.util.lookup.TapestryLookup;
import com.iw.plugins.spindle.wizards.fields.SuperClassDialogField;

/**
 * @author gwl
 * @version $Id$
 *
 * Copyright 2002, Intelligent Work Inc.
 * All Rights Reserved.
 */
public class ChooseWorkspaceModelWidget extends TwoListChooserWidget {

  static private final Object[] empty = new Object[0];

  private int acceptFlags;

  private ScanCollector collector = new ScanCollector();

  private TapestryLookup lookup;

  private String resultString;

  private IPackageFragment resultPackage;

  public ChooseWorkspaceModelWidget(IJavaProject project, int acceptFlags) {
  	
    super();

    configure(project);
    this.acceptFlags = acceptFlags;

    setFilterLabel("Search:");
    setInitialFilter("*");

    setUpperListLabel("Chose:");
    setUpperListLabelProvider(new TapestryStorageLabelProvider());
    setUpperListContentProvider(new StorageContentProvider());

    setLowerListLabel("in package:");
    setLowerListLabelProvider(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS));
    setLowerListContentProvider(new PackageContentProvider());

  }

  public ISelection getSelection() {

    IStructuredSelection selection = (IStructuredSelection) super.getSelection();

    if (selection == null || selection.isEmpty()) {

      return selection;

    }

    Object[] selectionData = selection.toArray();

    IStorage selectedStorage = (IStorage) selectionData[0];
    IPackageFragment selectedPackage = (IPackageFragment) selectionData[1];

    resultString = null;
    resultPackage = null;

    if (selectedStorage != null) {

      resultString = selectedStorage.getName();
    }

    if (selectedPackage != null) {

      resultPackage = selectedPackage;

    }

    if (resultString == null) {

      return new StructuredSelection();

    }

    return new StructuredSelection(resultString);
  }

  public void refresh() {
    if (lookup == null) {

      return;

    } else {

      super.refresh();
    }

  }

  public void configure(IJavaProject project) {
    lookup = new TapestryLookup();
    try {

      lookup.configure(project);

    } catch (JavaModelException jmex) {

      TapestryPlugin.getDefault().logException(jmex);
      lookup = null;
    }

  }

  public void dispose() {
    super.dispose();
    lookup = null;
  }

  public String getResultString() {
    return resultString;
  }

  public IPackageFragment getResultPackage() {
    return resultPackage;
  }

  public ITapestryModel getResultModel() {

    return collector.getModel(resultString, resultPackage);
  }

  public IStorage getResultStorage() {

    return collector.getStorage(resultString, resultPackage);

  }
  
  public String getResultPath() {
  	
  	String name = getResultStorage().getName();
  	String path = "/";
  	if ("".equals(resultPackage.getElementName())) {
  		
  		return  path+name;
  		
  	}
  	
  	path += resultPackage.getElementName().replace('.', '/');
  	path += "/"+name;
  	
  	return path;
  	
  }

  class StorageContentProvider implements IStructuredContentProvider {

    public Object[] getElements(Object inputElement) {

      String searchFilter = (String) inputElement;

      if (searchFilter == null || "".equals(searchFilter)) {

        return empty;

      }

      collector.reset();

      lookup.findAll(searchFilter, true, acceptFlags, collector);

      return collector.getStorages().toArray();

    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer, Object, Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

  }

  class PackageContentProvider implements IStructuredContentProvider {

    public Object[] getElements(Object inputElement) {

      IStorage selectedStorage = (IStorage) inputElement;

      if (selectedStorage == null) {

        return empty;

      }

      return collector.getPackagesFor(selectedStorage.getName());

    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(Viewer, Object, Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

  }

  class ScanCollector implements ILookupRequestor {

    Map results;
    List storages;
    Map storageLookup;

    /**
     * Constructor for ScanCollector
     */
    public ScanCollector() {
      super();
      reset();
    }

    public void reset() {
      results = new HashMap();
      storageLookup = new HashMap();
      storages = new ArrayList();
    }

    public Map getResults() {
      return results;
    }

    public List getStorages() {

      return storages;

    }

    public IStorage getStorage(String name, IPackageFragment pack) {

      String packname = "(default package)";
      if (pack != null) {

        packname = pack.getElementName();
      }
      return (IStorage) storageLookup.get(name + packname);

    }

    public ITapestryModel getModel(String name, IPackageFragment pack) {

      try {

        IStorage storage = getStorage(name, pack);

        return (ITapestryModel) TapestryPlugin.getTapestryModelManager(storage).getReadOnlyModel(
          storage);

      } catch (CoreException e) {

        return null;
      }
    }

    public Object[] getApplicationNames() {
      if (results == null) {
        return empty;
      }
      return new TreeSet(results.keySet()).toArray();
    }

    public Object[] getPackagesFor(String name) {
      if (results == null) {

        return empty;

      }
      Set packages = (Set) results.get(name);

      if (packages == null) {

        return empty;
      }
      return packages.toArray();
    }

    /**
     * @see ITapestryLookupRequestor#isCancelled()
     */
    public boolean isCancelled() {
      return false;
    }

    /**
     * @see ITapestryLookupRequestor#accept(IStorage, IPackageFragment)
     */
    public boolean accept(IStorage storage, IPackageFragment fragment) {

      String name = storage.getName();
      Object storePackageFragment;
      String packageElementName;

      storages.add(storage);

      if (fragment == null) {

        storePackageFragment = "(default package)";
        packageElementName = (String) storePackageFragment;

      } else {

        storePackageFragment = fragment;
        packageElementName = fragment.getElementName();
      }

      storageLookup.put(name + packageElementName, storage);

      Set packages = (Set) results.get(name);

      if (packages == null) {

        packages = new HashSet();
        packages.add(storePackageFragment);
        results.put(name, packages);

      } else if (!packages.contains(storePackageFragment)) {

        packages.add(storePackageFragment);
      }
      return true;
    }

  }

}
