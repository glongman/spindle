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
package com.iw.plugins.spindle.core.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.w3c.dom.Node;

import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.builder.util.CoreLookup;
import com.iw.plugins.spindle.core.builder.util.ILookupRequestor;
import com.iw.plugins.spindle.core.resources.ClasspathResourceWorkspaceLocation;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.core.scanning.ScannerException;
import com.iw.plugins.spindle.core.util.Markers;

/**
 * Builds a Tapestry project from scratch.
 * 
 * @version $Id$
 * @author glongman@intelligentworks.com
 */
public class FullBuild extends Build
{

    protected IType tapestryServletType;
    protected Map knownValidServlets;
    protected Map infoCache;

    BuilderQueue buildQueue;

    /**
     * Constructor for FullBuilder.
     */
    public FullBuild(TapestryBuilder builder)
    {
        super(builder);
        this.tapestryServletType = getType(TapestryCore.getString(TapestryBuilder.APPLICATION_SERVLET_NAME));

    }

    public void build()
    {
        if (TapestryBuilder.DEBUG)
            System.out.println("FULL Tapestry build");

        try
        {
            notifier.subTask(TapestryCore.getString(TapestryBuilder.STARTING));
            Markers.removeProblemsForProject(tapestryBuilder.currentProject);

            if (tapestryServletType == null)
            {
                Markers.addBuildBrokenProblemMarkerToResource(
                    tapestryBuilder.currentProject,
                    TapestryCore.getString(TapestryBuilder.TAPESTRY_JAR_MISSING));
            } else
            {
                findDeclaredApplications();
            }
            notifier.updateProgressDelta(0.1f);

            notifier.subTask(TapestryCore.getString(TapestryBuilder.LOCATING_ARTIFACTS));
            buildQueue = new BuilderQueue();

            buildQueue.addAll(findAllTapestryArtifacts());
            notifier.updateProgressDelta(0.15f);
            if (buildQueue.hasWaiting())
            {
                notifier.setProcessingProgressPer(0.75f / buildQueue.getWaitingCount());
                while (buildQueue.getWaitingCount() > 0)
                {

                    IResourceWorkspaceLocation location = (IResourceWorkspaceLocation) buildQueue.peekWaiting();
                    notifier.processed(location);
                    buildQueue.finished(location);
                }
            }

        } catch (CoreException e)
        {
            TapestryCore.log(e);
        } finally
        {
            cleanUp();
        }
    }

    /**
     * Method findAllTapestryArtifacts.
     */
    protected List findAllTapestryArtifacts() throws CoreException
    {
        ArrayList found = new ArrayList();
        findAllArtifactsInWebContext(found);
        findAllArtifactsInClasspath(found);
        return found;
    }

    /**
     * Method findAllArtifactsInBinaryClasspath.
     */
    private void findAllArtifactsInClasspath(final ArrayList found)
    {
        CoreLookup lookup = new CoreLookup();
        try
        {
            lookup.configure(tapestryBuilder.tapestryProject);
            lookup.findAll(new ArtifactCollector()
            {
                public void accept(IStorage storage, Object parent)
                {
                    IResourceWorkspaceLocation location =
                        new ClasspathResourceWorkspaceLocation((IPackageFragment) parent, storage);
                    found.add(location);
                    if (TapestryBuilder.DEBUG)
                    {
                        System.out.println(location);
                    }
                }
            });
        } catch (CoreException e)
        {
            TapestryCore.log(e);
            e.printStackTrace();
        }
    }

    /**
     * Method findAllArtifactsInProjectProper.
     */
    private void findAllArtifactsInWebContext(ArrayList found)
    {
        try
        {
            tapestryBuilder.getProject().accept(new BuilderContextVisitor(this, found), IResource.DEPTH_INFINITE, false);
        } catch (CoreException e)
        {
            TapestryCore.log(e);
        }
    }

    public void cleanUp()
    {}

    protected void findDeclaredApplications()
    {

        IFile webXML = tapestryBuilder.contextRoot.getFile("web.xml");
        if (webXML != null && webXML.exists())
        {
            // TODO need to pull any IProblems out and make them into Markers!
            Node wxmlElement = null;
            try
            {
                wxmlElement = parseToNode(webXML);
            } catch (IOException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (CoreException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            if (wxmlElement == null)
            {
                return;
            }
            ServletInfo[] servletInfos = null;
            try
            {
                WebXMLScanner wscanner = new WebXMLScanner(this);
                servletInfos = wscanner.getServletInformation(wxmlElement);
                Markers.addTapestryProblemMarkersToResource(webXML, wscanner.getProblems());
            } catch (ScannerException e)
            {
                TapestryCore.log(e);
            }
            if (servletInfos != null && servletInfos.length > 0)
            {
                knownValidServlets = new HashMap();
                for (int i = 0; i < servletInfos.length; i++)
                {
                    knownValidServlets.put(servletInfos[i].name, servletInfos[i]);
                }
            }

        } else
        {
            String definedWebRoot = tapestryBuilder.tapestryProject.getWebContext();
            if (definedWebRoot != null && !"".equals(definedWebRoot))
            {
                Markers.addTapestryProblemMarkerToResource(
                    tapestryBuilder.getProject(),
                    TapestryCore.getString(TapestryBuilder.MISSING_CONTEXT, definedWebRoot),
                    IMarker.SEVERITY_WARNING,
                    0,
                    0,
                    0);
            }
        }

    }

    private abstract class ArtifactCollector implements ILookupRequestor
    {
        public boolean isCancelled()
        {
            try
            {
                tapestryBuilder.notifier.checkCancel();
            } catch (OperationCanceledException e)
            {
                return true;
            }
            return false;
        }
        public void accept(IStorage storage, Object parent)
        {
            System.out.println(storage);
        }

    }

    public class ServletInfo
    {
        String name;
        String classname;
        Map parameters = new HashMap();
        boolean isServletSubclass;
        public String toString()
        {
            StringBuffer buffer = new StringBuffer("ServletInfo(");
            buffer.append(name);
            buffer.append(")::");
            buffer.append("classname = ");
            buffer.append(classname);
            buffer.append(", params = ");
            buffer.append(parameters);
            return buffer.toString();
        }
    }

}
