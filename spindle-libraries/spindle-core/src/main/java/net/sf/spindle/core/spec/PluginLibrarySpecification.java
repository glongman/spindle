package net.sf.spindle.core.spec;

/*
 The contents of this file are subject to the Mozilla Public License
 Version 1.1 (the "License"); you may not use this file except in
 compliance with the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/

 Software distributed under the License is distributed on an "AS IS"
 basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 License for the specific language governing rights and limitations
 under the License.

 The Original Code is __Spindle, an Eclipse Plugin For Tapestry__.

 The Initial Developer of the Original Code is _____Geoffrey Longman__.
 Portions created by _____Initial Developer___ are Copyright (C) _2004, 2005, 2006__
 __Geoffrey Longman____. All Rights Reserved.

 Contributor(s): __glongman@gmail.com___.
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.spindle.core.scanning.IScannerValidator;

import org.apache.tapestry.INamespace;
import org.apache.tapestry.spec.IExtensionSpecification;
import org.apache.tapestry.spec.ILibrarySpecification;

/**
 * Spindle aware concrete implementation of ILibrarySpecification
 * 
 * @author glongman@gmail.com
 */
public class PluginLibrarySpecification extends BaseSpecLocatable implements ILibrarySpecification
{

    /**
     * Map of component alias to component specification path.
     */
    private Map<String, String> fComponents;

    /**
     * The locations and values of all component declarations in a document. Immutable after a
     * parse/scan episode.
     */
    private List<PluginComponentTypeDeclaration> fComponentTypeDeclarations;

    /**
     * Map of extension name to {@link IExtensionSpecification}.
     */

    private Map<String, IExtensionSpecification> fExtensions;

    private List<PluginExtensionSpecification> fExtensionDeclarations;

    /**
     * Map of library id to library specification path.
     */

    private Map<String, String> fLibraries;

    /**
     * The locations and values of all library declarations in a document. Immutable after a
     * parse/scan episode.
     */
    private List<PluginLibraryDeclaration> fLibraryDeclarations;

    /**
     * Map of page name to page specification path.
     */

    private Map<String, String> fPages;

    /**
     * The locations and values of all page declarations in a document. Immutable after a parse/scan
     * episode.
     */
    private List<PluginPageDeclaration> fPageDeclarations;

    /**
     * Map of service name to service class name.
     */

    private Map<String, String> fServices;

    /**
     * The locations and values of all service declarations in a document. Immutable after a
     * parse/scan episode.<p>
     * Deprecated as of Tapestry 4.0
     */
    private List<PluginEngineServiceDeclaration> fEngineServiceDeclarations;

    private INamespace fNamespace;

    private String fPublicId;

    /**
     * Map of extension name to Object for instantiated extensions.
     */

    public PluginLibrarySpecification()
    {
        this(SpecType.LIBRARY_SPEC); 
    }

    protected PluginLibrarySpecification(SpecType type)
    {
        super(type);
    }

    public List getLibraryDeclaration()
    {
        if (fLibraryDeclarations != null)
            return Collections.unmodifiableList(fLibraryDeclarations);

        return Collections.EMPTY_LIST;
    }

    public List getPageDeclarations()
    {
        if (fPageDeclarations != null)
            return Collections.unmodifiableList(fPageDeclarations);

        return Collections.EMPTY_LIST;
    }

    public List getComponentTypeDeclarations()
    {
        if (fComponentTypeDeclarations != null)
            return Collections.unmodifiableList(fComponentTypeDeclarations);

        return Collections.EMPTY_LIST;
    }

    public List getEngineServiceDeclarations()
    {
        if (fEngineServiceDeclarations != null)
            return Collections.unmodifiableList(fEngineServiceDeclarations);

        return Collections.EMPTY_LIST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#addExtensionSpecification(java.lang.String,
     *      org.apache.tapestry.spec.IExtensionSpecification)
     */
    public void addExtensionSpecification(String name, IExtensionSpecification extension)
    {
        if (fExtensions == null)
            fExtensions = new HashMap<String, IExtensionSpecification>();

        fExtensions.put(name, extension);
    }

    public void addExtension(PluginExtensionSpecification extension)
    {
        if (fExtensionDeclarations == null)
            fExtensionDeclarations = new ArrayList<PluginExtensionSpecification>();

        fExtensionDeclarations.add(extension);
        extension.setParent(this);

        addExtensionSpecification(extension.getIdentifier(), extension);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#checkExtension(java.lang.String)
     */
    public boolean checkExtension(String name)
    {
        return true;
    }

    public Map getComponents()
    {
        if (fComponents != null)
            return Collections.unmodifiableMap(fComponents);

        return Collections.EMPTY_MAP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getComponentSpecificationPath(java.lang.String)
     */
    public String getComponentSpecificationPath(String type)
    {
        return (String) get(fComponents, type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getComponentTypes()
     */
    public List getComponentTypes()
    {
        return keys(fComponents);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getExtension(java.lang.String)
     */
    public Object getExtension(String name)
    {
        return get(fExtensions, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getExtension(java.lang.String,
     *      java.lang.Class)
     */
    public Object getExtension(String name, Class typeConstraint)
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getExtensionNames()
     */
    public List getExtensionNames()
    {
        return keys(fExtensions);
    }

    public Map getExtensions()
    {
        if (fExtensions != null)
            return Collections.unmodifiableMap(fExtensions);

        return Collections.EMPTY_MAP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getExtensionSpecification(java.lang.String)
     */
    public IExtensionSpecification getExtensionSpecification(String name)
    {
        return (IExtensionSpecification) get(fExtensions, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getExtensionSpecifications()
     */
    public Map getExtensionSpecifications()
    {
        if (fExtensions == null)
            return Collections.EMPTY_MAP;

        return Collections.unmodifiableMap(fExtensions);
    }

    public Map getLibraries()
    {
        if (fLibraries != null)
            return Collections.unmodifiableMap(fLibraries);

        return Collections.EMPTY_MAP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getLibraryIds()
     */
    public List getLibraryIds()
    {
        return keys(fLibraries);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getLibrarySpecificationPath(java.lang.String)
     */
    public String getLibrarySpecificationPath(String id)
    {
        return (String) get(fLibraries, id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getPageNames()
     */
    public List getPageNames()
    {
        return keys(fPages);
    }

    public Map getPages()
    {
        if (fPages != null)
            return Collections.unmodifiableMap(fPages);

        return Collections.EMPTY_MAP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getPageSpecificationPath(java.lang.String)
     */
    public String getPageSpecificationPath(String name)
    {
        return (String) get(fPages, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getPublicId()
     */
    public String getPublicId()
    {
        return fPublicId;
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see org.apache.tapestry.spec.ILibrarySpecification#getResourceResolver()
    // */
    // public IResourceResolver getResourceResolver()
    // {
    // return fResourceResolver;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getServiceClassName(java.lang.String)
     */
    public String getServiceClassName(String name)
    {
        return (String) get(fServices, name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#getServiceNames()
     */
    public List getServiceNames()
    {
        return keys(fServices);
    }

    public Map getServices()
    {
        if (fServices != null)
            return Collections.unmodifiableMap(fServices);

        return Collections.EMPTY_MAP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#instantiateImmediateExtensions()
     */
    public void instantiateImmediateExtensions()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#setComponentSpecificationPath(java.lang.String,
     *      java.lang.String)
     */
    public void setComponentSpecificationPath(String type, String path)
    {
        if (fComponents == null)
            fComponents = new HashMap<String, String>();

        fComponents.put(type, path);

    }

    public void addComponentTypeDeclaration(PluginComponentTypeDeclaration declaration)
    {
        if (fComponentTypeDeclarations == null)
            fComponentTypeDeclarations = new ArrayList<PluginComponentTypeDeclaration>();

        fComponentTypeDeclarations.add(declaration);

        if (!getComponentTypes().contains(declaration.getId()))
            setComponentSpecificationPath(declaration.getId(), declaration.getResourcePath());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#setLibrarySpecificationPath(java.lang.String,
     *      java.lang.String)
     */
    public void setLibrarySpecificationPath(String id, String path)
    {
        if (fLibraries == null)
            fLibraries = new HashMap<String, String>();

        fLibraries.put(id, path);

    }

    public void addLibraryDeclaration(PluginLibraryDeclaration declaration)
    {
        if (fLibraryDeclarations == null)
            fLibraryDeclarations = new ArrayList<PluginLibraryDeclaration>();

        fLibraryDeclarations.add(declaration);
        declaration.setParent(this);

        if (!getLibraryIds().contains(declaration.getName()))
            setLibrarySpecificationPath(declaration.getName(), declaration.getResourcePath());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#setPageSpecificationPath(java.lang.String,
     *      java.lang.String)
     */
    public void setPageSpecificationPath(String name, String path)
    {
        if (fPages == null)
            fPages = new HashMap<String, String>();

        fPages.put(name, path);
    }

    public void addPageDeclaration(PluginPageDeclaration declaration)
    {
        if (fPageDeclarations == null)
            fPageDeclarations = new ArrayList<PluginPageDeclaration>();

        fPageDeclarations.add(declaration);

        declaration.setParent(this);

        if (!getPageNames().contains(declaration.getName()))
            setPageSpecificationPath(declaration.getName(), declaration.getResourcePath());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#setServiceClassName(java.lang.String,
     *      java.lang.String)
     */
    public void setServiceClassName(String name, String className)
    {
        if (fServices == null)
            fServices = new HashMap<String, String>();

        fServices.put(name, className);
    }

    public void addEngineServiceDeclaration(PluginEngineServiceDeclaration declaration)
    {
        if (fEngineServiceDeclarations == null)
            fEngineServiceDeclarations = new ArrayList<PluginEngineServiceDeclaration>();

        fEngineServiceDeclarations.add(declaration);

        if (!getServiceNames().contains(declaration.getName()))
            setServiceClassName(declaration.getName(), declaration.getServiceClass());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.tapestry.spec.ILibrarySpecification#setPublicId(java.lang.String)
     */
    public void setPublicId(String value)
    {
        fPublicId = value;
    }

    // /*
    // * (non-Javadoc)
    // *
    // * @see
    // org.apache.tapestry.spec.ILibrarySpecification#setResourceResolver(org.apache.tapestry.IResourceResolver)
    // */
    // public void setResourceResolver(IResourceResolver resolver)
    // {
    // this.fResourceResolver = resolver;
    // }

    /**
     * @return
     */
    public INamespace getNamespace()
    {
        return fNamespace;
    }

    /**
     * @param namespace
     */
    public void setNamespace(INamespace namespace)
    {
        this.fNamespace = namespace;
    }

    public void validate(IScannerValidator validator)
    {

        if (fComponentTypeDeclarations != null)
        {
            for (int i = 0; i < fComponentTypeDeclarations.size(); i++)
            {

                PluginComponentTypeDeclaration element = (PluginComponentTypeDeclaration) fComponentTypeDeclarations
                        .get(i);
                element.validate(this, validator);
            }
        }

        if (fPageDeclarations != null)
        {
            for (int i = 0; i < fPageDeclarations.size(); i++)
            {

                PluginPageDeclaration element = (PluginPageDeclaration) fPageDeclarations.get(i);
                element.validate(this, validator);
            }
        }

        if (fEngineServiceDeclarations != null)
        {
            for (int i = 0; i < fEngineServiceDeclarations.size(); i++)
            {

                PluginEngineServiceDeclaration element = (PluginEngineServiceDeclaration) fEngineServiceDeclarations
                        .get(i);
                element.validate(this, validator);
            }
        }

        if (fExtensionDeclarations != null)
        {
            for (int i = 0; i < fExtensionDeclarations.size(); i++)
            {

                PluginExtensionSpecification element = (PluginExtensionSpecification) fExtensionDeclarations
                        .get(i);
                element.validate(this, validator);
            }
        }

        if (fLibraryDeclarations != null)
        {
            for (int i = 0; i < fLibraryDeclarations.size(); i++)
            {

                PluginLibraryDeclaration element = (PluginLibraryDeclaration) fLibraryDeclarations
                        .get(i);
                element.validate(this, validator);
            }

        }
    }

}