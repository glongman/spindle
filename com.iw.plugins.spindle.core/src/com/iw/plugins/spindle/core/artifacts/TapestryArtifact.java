/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Spindle, an Eclipse Plugin for Tapestry.
 *
 * The Initial Developer of the Original Code is
 * Intelligent Works Incorporated.
 * Portions created by the Initial Developer are Copyright (C) 2003
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */

package com.iw.plugins.spindle.core.artifacts;

import java.io.InputStream;
import java.io.PrintWriter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;

import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.spec.BaseSpecification;

/**
 *  Concrete Tapestry Model impl
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public class TapestryArtifact implements ITapestryArtifact
{

    public static final int APPLICATION = 0x00000001;
    public static final int LIBRARY = 0x00000002;
    public static final int PAGE = 0x00000004;
    public static final int SCRIPT = 0x00000010;
    public static final int TEMPLATE = 0x00000008;
    private BaseSpecification specification;

    private int type;
    private IStorage underlyingStorage;
    private boolean valid;

    public TapestryArtifact(int type)
    {
        super();
        this.type = type;
    }


    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#dispose()
     */
    public void dispose()
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class adapter)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#getResourceString(java.lang.String)
     */
    public String getResourceString(String key)
    {
        return TapestryCore.getString(key);
    }

    /**
     * @return
     */
    public BaseSpecification getSpecification()
    {
        return specification;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#getTimeStamp()
     */
    public long getTimeStamp()
    {
        return 0;
    }

    /**
     * @return
     */
    public int getType()
    {
        return type;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#getUnderlyingResource()
     */
    public IResource getUnderlyingResource()
    {
        return (IResource)underlyingStorage.getAdapter(IResource.class);
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.model.ITapestryModel#getUnderlyingStorage()
     */
    public IStorage getUnderlyingStorage()
    {
        return underlyingStorage;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IEditable#isDirty()
     */
    public boolean isDirty()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#isDisposed()
     */
    public boolean isDisposed()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IEditable#isEditable()
     */
    public boolean isEditable()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#isInSync()
     */
    public boolean isInSync()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#isLoaded()
     */
    public boolean isLoaded()
    {
        return false;
    }
  
    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.model.ITapestryModel#isStructureKnown()
     */
    public boolean isStructureKnown()
    {
        return specification != null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#isValid()
     */
    public boolean isValid()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#load()
     */
    public void load() throws CoreException
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
     */
    public void load(InputStream source, boolean outOfSync) throws CoreException
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
     */
    public void reload(InputStream source, boolean outOfSync) throws CoreException
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IEditable#save(java.io.PrintWriter)
     */
    public void save(PrintWriter writer)
    {
    }

    /* (non-Javadoc)
     * @see org.eclipse.pde.core.IEditable#setDirty(boolean)
     */
    public void setDirty(boolean dirty)
    {
    }

    /**
     * @param specification
     */
    public void setSpecification(BaseSpecification specification)
    {
        this.specification = specification;
    }

    /**
     * @param storage
     */
    public void setUnderlyingStorage(IStorage storage)
    {
        underlyingStorage = storage;
    }

    /**
     * @param b
     */
    public void setValid(boolean b)
    {
        valid = b;
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.model.ITapestryArtifact#isResource()
     */
    public boolean isResource()
    {        
        return getUnderlyingResource() != null;
    }

}
