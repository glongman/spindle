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
package com.iw.plugins.spindle.core.resources;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JarEntryFile;

import com.iw.plugins.spindle.core.TapestryModelException;
import com.iw.plugins.spindle.core.TapestryProject;
import com.iw.plugins.spindle.core.resources.search.ISearch;
import com.iw.plugins.spindle.core.resources.search.ISearchAcceptor;

// does not stay up to date as time goes on!

public class ClasspathSearch implements ISearch
{

    protected IPackageFragmentRoot[] packageFragmentRoots = null;

    protected HashMap packageFragments;

    protected IJavaProject jproject;
    protected TapestryProject tproject;

    private boolean initialized = false;

    public ClasspathSearch()
    {}

    public void configure(Object root) throws CoreException
    {
        this.jproject = (IJavaProject) root;
        configureClasspath();
        initialized = true;
    }

    /* pull the classpath info we need from the JavaModel */
    protected void configureClasspath() throws TapestryModelException
    {
        try
        {
            packageFragmentRoots = jproject.getAllPackageFragmentRoots();
            packageFragments = new HashMap();
            IPackageFragment[] frags = getPackageFragmentsInRoots(packageFragmentRoots, jproject);
            for (int i = 0; i < frags.length; i++)
            {
                IPackageFragment fragment = frags[i];
                IPackageFragment[] entry = (IPackageFragment[]) packageFragments.get(fragment.getElementName());
                if (entry == null)
                {
                    entry = new IPackageFragment[1];
                    entry[0] = fragment;
                    packageFragments.put(fragment.getElementName(), entry);
                } else
                {
                    IPackageFragment[] copy = new IPackageFragment[entry.length + 1];
                    System.arraycopy(entry, 0, copy, 0, entry.length);
                    copy[entry.length] = fragment;
                    packageFragments.put(fragment.getElementName(), copy);
                }
            }
        } catch (JavaModelException e)
        {
            throw new TapestryModelException(null);
        }
    }

    private IPackageFragment[] getPackageFragmentsInRoots(IPackageFragmentRoot[] roots, IJavaProject project)
    {

        ArrayList frags = new ArrayList();
        for (int i = 0; i < roots.length; i++)
        {
            IPackageFragmentRoot root = roots[i];
            try
            {
                IJavaElement[] children = root.getChildren();

                int length = children.length;
                if (length == 0)
                    continue;
                if (children[0].getParent().getParent().equals(project))
                {
                    for (int j = 0; j < length; j++)
                    {
                        frags.add(children[j]);
                    }
                } else
                {
                    for (int j = 0; j < length; j++)
                    {
                        frags.add(root.getPackageFragment(children[j].getElementName()));
                    }
                }
            } catch (JavaModelException e)
            {
                // do nothing
            }
        }
        IPackageFragment[] fragments = new IPackageFragment[frags.size()];
        frags.toArray(fragments);
        return fragments;
    }

    public void search(ISearchAcceptor acceptor)
    {
        if (!initialized)
        {
            throw new Error("not initialized");
        }
        int count = packageFragmentRoots.length;
        for (int i = 0; i < count; i++)
        {

            IPackageFragmentRoot root = packageFragmentRoots[i];
            IJavaElement[] packages = null;
            try
            {
                packages = root.getChildren();
            } catch (JavaModelException npe)
            {
                continue; // the root is not present, continue;
            }
            if (packages != null)
            {
                for (int j = 0, packageCount = packages.length; j < packageCount; j++)
                {
                    boolean keepGoing = searchInPackage((IPackageFragment) packages[j], acceptor);
                    if (!keepGoing)
                    {
                        return;
                    }
                }
            }
        }
    }

    protected boolean searchInPackage(IPackageFragment pkg, ISearchAcceptor acceptor)
    {

        if (!initialized)
        {
            throw new Error("not initialized");
        }
        boolean keepGoing = true;

        IPackageFragmentRoot root = (IPackageFragmentRoot) pkg.getParent();

        try
        {
            int packageFlavor = root.getKind();

            switch (packageFlavor)
            {
                case IPackageFragmentRoot.K_BINARY :
                    keepGoing = searchInBinaryPackage(pkg, acceptor);
                    break;
                case IPackageFragmentRoot.K_SOURCE :
                    keepGoing = searchInSourcePackage(pkg, acceptor);
                    break;
                default :
                    return keepGoing;
            }
        } catch (JavaModelException e)
        {}
        return keepGoing;
    }

    protected boolean searchInBinaryPackage(IPackageFragment pkg, ISearchAcceptor requestor)
    {
        boolean keepGoing = true;
        Object[] jarFiles = null;
        try
        {
            jarFiles = pkg.getNonJavaResources();
        } catch (JavaModelException npe)
        {
            return false; // the package is not present
        }
        int length = jarFiles.length;
        for (int i = 0; i < length; i++)
        {
            JarEntryFile jarFile = null;
            try
            {
                jarFile = (JarEntryFile) jarFiles[i];
            } catch (ClassCastException ccex)
            {
                //skip it
                continue;
            }

            keepGoing = requestor.accept(pkg, (IStorage) jarFile);

        }
        return keepGoing;
    }

    protected boolean searchInSourcePackage(IPackageFragment pkg, ISearchAcceptor requestor)
    {
        Object[] files = null;

        boolean keepGoing = true;
        try
        {
            files = getSourcePackageResources(pkg);

        } catch (CoreException npe)
        {
            return false; // the package is not present
        }
        if (files == null)
        {
            return false;
        }
        int length = files.length;
        for (int i = 0; i < length; i++)
        {

            IFile file = null;
            try
            {
                file = (IFile) files[i];
            } catch (ClassCastException ccex)
            {
                // skip it
                continue;
            }

            keepGoing = requestor.accept(pkg, (IStorage) file);

        }
        return keepGoing;
    }

    /**
     * Method getPackageResources.
     * @param pkg
     * @return Object[]
     */
    private Object[] getSourcePackageResources(IPackageFragment pkg) throws CoreException
    {
        Object[] result = new Object[0];
        if (!pkg.isDefaultPackage())
        {
            result = pkg.getNonJavaResources();
        } else
        {
            IContainer container = (IContainer) pkg.getUnderlyingResource();
            if (container != null && container.exists())
            {
                IResource[] members = container.members(false);
                ArrayList resultList = new ArrayList();
                for (int i = 0; i < members.length; i++)
                {
                    if (members[i] instanceof IFile)
                    {
                        resultList.add(members[i]);
                    }
                    result = resultList.toArray();
                }
            }

        }
        return result;
    }

}