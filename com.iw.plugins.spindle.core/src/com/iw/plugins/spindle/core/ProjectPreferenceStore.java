/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *    glongman@gmail.com - tweaks for Spindle.
 *******************************************************************************/
package com.iw.plugins.spindle.core;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
/**
 * A concrete preference store implementation based on an internal
 * <code>java.util.Properties</code> object, with support for persisting the
 * non-default preference values to files or streams.
 * <p>
 * Changed by GWL to use another preference store to provide defaults
 * </p>
 * 
 * @see IPreferenceStore
 */
public class ProjectPreferenceStore implements IPersistentPreferenceStore
{

  public static ProjectPreferenceStore createEmptyStore(IPreferenceStore backingStore)
  {
    Assert.isNotNull(backingStore);
    return new ProjectPreferenceStore("", backingStore);
  }

  public static ProjectPreferenceStore getStore(
      IProject project,
      String filename,
      IPreferenceStore defaultStore)
  {
    Assert.isNotNull(project);
    Assert.isNotNull(defaultStore);

    ProjectPreferenceStore store = new ProjectPreferenceStore("", defaultStore);

    IFile dataFile = project.getFile(filename);

    String fullname;
    if (dataFile.exists())
    {
      fullname = dataFile.getLocation().toOSString();
    } else
    {
      fullname = project.getLocation().toOSString() + File.separator + filename;
    }

    store.setFilename(fullname);

    if (dataFile.exists())
    {
      try
      {
        store.load();
        for (Iterator iter = store.properties.keySet().iterator(); iter.hasNext();)
        {
          String name = (String) iter.next();
          String value = store.properties.getProperty(name);

          if (store.backingStore.getString(name).equals(value))
            store.properties.remove(name);
        }
      } catch (IOException e)
      {
        TapestryCore.log(e);
        MessageDialog.openError(
            TapestryCore.getDefault().getActiveWorkbenchShell(),
            "Error",
            "an error occured reading project prefs: " + e.getMessage());
      }
    }
    return store;
  }

  /**
   * List of registered listeners (element type:
   * <code>IPropertyChangeListener</code>). These listeners are to be
   * informed when the current value of a preference changes.
   */
  private ListenerList listeners = new ListenerList();
  /**
   * The mapping from preference name to preference value (represented as
   * strings).
   */
  private Properties properties;
  /**
   * The backing store that provides default values.
   */
  private IPreferenceStore backingStore;
  /**
   * The mapping from preference name to default preference value (represented
   * as strings); <code>null</code> if none.
   */
  private Properties defaultProperties;
  /**
   * Indicates whether a value as been changed by <code>setToDefault</code> or
   * <code>setValue</code>; initially <code>false</code>.
   */
  private boolean dirty = false;
  /**
   * The file name used by the <code>load</code> method to load a property
   * file. This filename is used to save the properties file when
   * <code>save</code> is called.
   */
  private String filename;
  /**
   * Creates an empty preference store.
   * <p>
   * Use the methods <code>load(InputStream)</code> and
   * <code>save(InputStream)</code> to load and store this preference store.
   * </p>
   * 
   * @see #load(InputStream)
   * @see #save(OutputStream, String)
   */
  public ProjectPreferenceStore(String filename, IPreferenceStore defaultStore)
  {
    properties = new Properties(defaultProperties);
    backingStore = defaultStore;
    setFilename(filename);
  }

  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void addPropertyChangeListener(IPropertyChangeListener listener)
  {
    listeners.add(listener);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public boolean contains(String name)
  {
    return (properties.containsKey(name) || defaultProperties.containsKey(name));
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void firePropertyChangeEvent(String name, Object oldValue, Object newValue)
  {
    final Object[] finalListeners = this.listeners.getListeners();
    // Do we need to fire an event.
    if (finalListeners.length > 0 && (oldValue == null || !oldValue.equals(newValue)))
    {
      final PropertyChangeEvent pe = new PropertyChangeEvent(
          this,
          name,
          oldValue,
          newValue);
      //	FIXME: need to do this without dependency on
      // org.eclipse.core.runtime
      //		Platform.run(new
      // SafeRunnable(JFaceResources.getString("PreferenceStore.changeError"))
      // { //$NON-NLS-1$
      //			public void run() {
      for (int i = 0; i < finalListeners.length; ++i)
      {
        IPropertyChangeListener l = (IPropertyChangeListener) finalListeners[i];
        l.propertyChange(pe);
      }
      //			}
      //		});
    }
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public boolean getBoolean(String name)
  {
    return getBoolean(properties, name);
  }
  /**
   * Helper function: gets boolean for a given name.
   * 
   * @param p
   * @param name
   * @return boolean
   */
  private boolean getBoolean(Properties p, String name)
  {
    String value = p != null ? p.getProperty(name) : null;
    if (value == null)
      return BOOLEAN_DEFAULT_DEFAULT;
    if (value.equals(IPreferenceStore.TRUE))
      return true;
    return false;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public boolean getDefaultBoolean(String name)
  {
    return backingStore.getBoolean(name);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public double getDefaultDouble(String name)
  {
    return backingStore.getDouble(name);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public float getDefaultFloat(String name)
  {
    return backingStore.getFloat(name);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public int getDefaultInt(String name)
  {
    return backingStore.getInt(name);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public long getDefaultLong(String name)
  {
    return backingStore.getLong(name);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public String getDefaultString(String name)
  {
    return backingStore.getString(name);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public double getDouble(String name)
  {
    return getDouble(properties, name);
  }
  /**
   * Helper function: gets double for a given name.
   * 
   * @param p
   * @param name
   * @return double
   */
  private double getDouble(Properties p, String name)
  {
    String value = p != null ? p.getProperty(name) : null;
    if (value == null)
      return backingStore.getDouble(name);
    double ival = DOUBLE_DEFAULT_DEFAULT;
    try
    {
      ival = new Double(value).doubleValue();
    } catch (NumberFormatException e)
    {}
    return ival;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public float getFloat(String name)
  {
    return getFloat(properties, name);
  }
  /**
   * Helper function: gets float for a given name.
   * 
   * @param p
   * @param name
   * @return float
   */
  private float getFloat(Properties p, String name)
  {
    String value = p != null ? p.getProperty(name) : null;
    if (value == null)
      return backingStore.getFloat(name);
    float ival = FLOAT_DEFAULT_DEFAULT;
    try
    {
      ival = new Float(value).floatValue();
    } catch (NumberFormatException e)
    {}
    return ival;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public int getInt(String name)
  {
    return getInt(properties, name);
  }
  /**
   * Helper function: gets int for a given name.
   * 
   * @param p
   * @param name
   * @return int
   */
  private int getInt(Properties p, String name)
  {
    String value = p != null ? p.getProperty(name) : null;
    if (value == null)
      return backingStore.getInt(name);
    int ival = 0;
    try
    {
      ival = Integer.parseInt(value);
    } catch (NumberFormatException e)
    {}
    return ival;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public long getLong(String name)
  {
    return getLong(properties, name);
  }
  /**
   * Helper function: gets long for a given name.
   * 
   * @param p
   * @param name
   * @return
   */
  private long getLong(Properties p, String name)
  {
    String value = p != null ? p.getProperty(name) : null;
    if (value == null)
      return backingStore.getLong(name);
    long ival = LONG_DEFAULT_DEFAULT;
    try
    {
      ival = Long.parseLong(value);
    } catch (NumberFormatException e)
    {}
    return ival;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public String getString(String name)
  {
    return getString(properties, name);
  }
  /**
   * Helper function: gets string for a given name.
   * 
   * @param p
   * @param name
   * @return
   */
  private String getString(Properties p, String name)
  {
    String value = p != null ? p.getProperty(name) : null;
    if (value == null)
      return backingStore.getString(name);
    return value;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public boolean isDefault(String name)
  {
    if (!properties.containsKey(name))
      return true;

    String value = properties.getProperty(name, null);
    if (value == null)
      return true;

    return value.equals(backingStore.getString(name));
  }
  /**
   * Prints the contents of this preference store to the given print stream.
   * 
   * @param out the print stream
   */
  public void list(PrintStream out)
  {
    properties.list(out);
  }
  /**
   * Prints the contents of this preference store to the given print writer.
   * 
   * @param out the print writer
   */
  public void list(PrintWriter out)
  {
    properties.list(out);
  }
  /**
   * Loads this preference store from the file established in the constructor
   * <code>PreferenceStore(java.lang.String)</code> (or by
   * <code>setFileName</code>). Default preference values are not affected.
   * 
   * @exception java.io.IOException if there is a problem loading this store
   */
  public void load() throws IOException
  {
    if (filename == null)
      throw new IOException("File name not specified");//$NON-NLS-1$
    FileInputStream in = new FileInputStream(filename);
    load(in);
    in.close();
  }
  /**
   * Loads this preference store from the given input stream. Default preference
   * values are not affected.
   * 
   * @param in the input stream
   * @exception java.io.IOException if there is a problem loading this store
   */
  public void load(InputStream in) throws IOException
  {
    properties.load(in);
    dirty = false;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public boolean needsSaving()
  {
    return dirty;
  }
  /**
   * Returns an enumeration of all preferences known to this store which have
   * current values other than their default value.
   * 
   * @return an array of preference names
   */
  public String[] preferenceNames()
  {
    ArrayList list = new ArrayList();
    Enumeration enumeration = properties.propertyNames();
    while (enumeration.hasMoreElements())
    {
      list.add(enumeration.nextElement());
    }
    return (String[]) list.toArray(new String[list.size()]);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void putValue(String name, String value)
  {
    String oldValue = getString(name);
    if (oldValue == null || !oldValue.equals(value))
    {
      setValue(properties, name, value);
      dirty = true;
    }
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void removePropertyChangeListener(IPropertyChangeListener listener)
  {
    listeners.remove(listener);
  }
  /**
   * Saves the non-default-valued preferences known to this preference store to
   * the file from which they were originally loaded.
   * 
   * @exception java.io.IOException if there is a problem saving this store
   */
  public void save() throws IOException
  {
    if (filename == null)
      throw new IOException("File name not specified");//$NON-NLS-1$
    FileOutputStream out = null;
    try
    {
      out = new FileOutputStream(filename);
      save(out, "if empty, workspace defaults are used");
    } finally
    {
      if (out != null)
        out.close();
    }
  }
  /**
   * Saves this preference store to the given output stream. The given string is
   * inserted as header information.
   * 
   * @param out the output stream
   * @param header the header
   * @exception java.io.IOException if there is a problem saving this store
   */
  public void save(OutputStream out, String header) throws IOException
  {
    properties.store(out, header);
    dirty = false;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setDefault(String name, double value)
  {
    setValue(defaultProperties, name, value);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setDefault(String name, float value)
  {
    setValue(defaultProperties, name, value);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setDefault(String name, int value)
  {
    setValue(defaultProperties, name, value);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setDefault(String name, long value)
  {
    setValue(defaultProperties, name, value);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setDefault(String name, String value)
  {
    setValue(defaultProperties, name, value);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setDefault(String name, boolean value)
  {
    setValue(defaultProperties, name, value);
  }
  /**
   * Sets the name of the file used when loading and storing this preference
   * store.
   * <p>
   * Afterward, the methods <code>load()</code> and <code>save()</code> can
   * be used to load and store this preference store.
   * </p>
   * 
   * @param name the file name
   * @see #load()
   * @see #save()
   */
  public void setFilename(String name)
  {
    filename = name;
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setToDefault(String name)
  {
    Object oldValue = properties.get(name);
    properties.remove(name);
    dirty = true;
    Object newValue = null;
    if (backingStore != null)
      newValue = backingStore.getString(name);
    // XXX this is hacked for String only
    firePropertyChangeEvent(name, oldValue, newValue);
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setValue(String name, double value)
  {
    double oldValue = getDouble(name);
    if (oldValue != value)
    {
      setValue(properties, name, value);
      dirty = true;
      firePropertyChangeEvent(name, new Double(oldValue), new Double(value));
    }
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setValue(String name, float value)
  {
    float oldValue = getFloat(name);
    if (oldValue != value)
    {
      setValue(properties, name, value);
      dirty = true;
      firePropertyChangeEvent(name, new Float(oldValue), new Float(value));
    }
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setValue(String name, int value)
  {
    int oldValue = getInt(name);
    if (oldValue != value)
    {
      setValue(properties, name, value);
      dirty = true;
      firePropertyChangeEvent(name, new Integer(oldValue), new Integer(value));
    }
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setValue(String name, long value)
  {
    long oldValue = getLong(name);
    if (oldValue != value)
    {
      setValue(properties, name, value);
      dirty = true;
      firePropertyChangeEvent(name, new Long(oldValue), new Long(value));
    }
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setValue(String name, String value)
  {
    String oldValue = getString(name);
    if (oldValue == null || !oldValue.equals(value))
    {
      setValue(properties, name, value);
      dirty = true;
      firePropertyChangeEvent(name, oldValue, value);
    }
  }
  /*
   * (non-Javadoc) Method declared on IPreferenceStore.
   */
  public void setValue(String name, boolean value)
  {
    boolean oldValue = getBoolean(name);
    if (oldValue != value)
    {
      setValue(properties, name, value);
      dirty = true;
      firePropertyChangeEvent(name, new Boolean(oldValue), new Boolean(value));
    }
  }
  /**
   * Helper method: sets value for a given name.
   * 
   * @param p
   * @param name
   * @param value
   */
  private void setValue(Properties p, String name, double value)
  {
    Assert.isTrue(p != null);
    p.put(name, Double.toString(value));
  }
  /**
   * Helper method: sets value for a given name.
   * 
   * @param p
   * @param name
   * @param value
   */
  private void setValue(Properties p, String name, float value)
  {
    Assert.isTrue(p != null);
    p.put(name, Float.toString(value));
  }
  /**
   * Helper method: sets value for a given name.
   * 
   * @param p
   * @param name
   * @param value
   */
  private void setValue(Properties p, String name, int value)
  {
    Assert.isTrue(p != null);
    p.put(name, Integer.toString(value));
  }
  /**
   * Helper method: sets the value for a given name.
   * 
   * @param p
   * @param name
   * @param value
   */
  private void setValue(Properties p, String name, long value)
  {
    Assert.isTrue(p != null);
    p.put(name, Long.toString(value));
  }
  /**
   * Helper method: sets the value for a given name.
   * 
   * @param p
   * @param name
   * @param value
   */
  private void setValue(Properties p, String name, String value)
  {
    Assert.isTrue(p != null && value != null);
    p.put(name, value);
  }
  /**
   * Helper method: sets the value for a given name.
   * 
   * @param p
   * @param name
   * @param value
   */
  private void setValue(Properties p, String name, boolean value)
  {
    Assert.isTrue(p != null);
    p.put(name, value == true ? IPreferenceStore.TRUE : IPreferenceStore.FALSE);
  }
}