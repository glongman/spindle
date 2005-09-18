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
 * Geoffrey Longman.
 * Portions created by the Initial Developer are Copyright (C) 2001-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@gmail.com
 *
 * ***** END LICENSE BLOCK ***** */
package com.iw.plugins.spindle.core.builder;

import java.io.IOException;
import java.util.Iterator;

import com.iw.plugins.spindle.core.CoreMessages;
import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.namespace.ICoreNamespace;
import com.iw.plugins.spindle.core.parser.dom.IDOMModel;
import com.iw.plugins.spindle.core.resources.ICoreResource;
import com.iw.plugins.spindle.core.scanning.ScannerException;
import com.iw.plugins.spindle.core.source.DefaultProblem;
import com.iw.plugins.spindle.core.source.IProblem;
import com.iw.plugins.spindle.core.source.SourceLocation;

/**
 * Builds a Tapestry project from scratch.
 * 
 * @author glongman@gmail.com
 */
public class FullBuild extends AbstractBuild
{

    protected ServletInfo fApplicationServlet;

    protected WebAppDescriptor fWebAppDescriptor;

    /**
     * Constructor for FullBuilder.
     */
    public FullBuild(AbstractBuildInfrastructure infrastructure)
    {
        super(infrastructure);
    }

    /**
     * Use the parser to find declared applications in web.xml
     * 
     * @param parser
     * @throws CoreException
     */
    protected void preBuild()
    {
        setDependencyListener(new BuilderDependencyListener());
        findDeclaredApplication();
    }

    protected void postBuild()
    {
        BuilderDependencyListener listener = (BuilderDependencyListener) getDependencyListener();
        if (fInfrastructure.DEBUG)
        {
            listener.dump();
        }
    }

    /**
     * Resolve the Tapesty framework namespace
     */
    protected void resolveFramework()
    {
        ICoreResource frameworkLocation = (ICoreResource) fInfrastructure.fClasspathRoot
                .getRelativeResource("/org/apache/tapestry/Framework.library");
        FrameworkResolver resolver = new FrameworkResolver(this, frameworkLocation);
        fFrameworkNamespace = resolver.resolve();
        // fFrameworkNamespace =
        // fNSResolver.resolveFrameworkNamespace(frameworkLocation);
    }

    /**
     * Resolve the application namespace
     */
    protected void doBuild()
    {
        ApplicationResolver resolver = new ApplicationResolver(this, fFrameworkNamespace,
                fApplicationServlet);
        fApplicationNamespace = resolver.resolve();
    }

    public void saveState()
    {
        State newState = new State(fInfrastructure);
        // newState.fLibraryLocation = fTapestryBuilder.fTapestryProject.getLibrarySpecPath();
        newState.fLastKnownClasspath = fInfrastructure.getClasspathMemento();
        newState.fJavaDependencies = fFoundTypes;
        newState.fMissingJavaTypes = fMissingTypes;
        newState.fTemplateMap = fTemplateMap;
        newState.fFileSpecificationMap = fFileSpecificationMap;
        newState.fBinarySpecificationMap = fBinarySpecificationMap;
        newState.fSeenTemplateExtensions = fSeenTemplateExtensions;
        newState.fApplicationServlet = fApplicationServlet;
        newState.fWebAppDescriptor = fWebAppDescriptor;
        newState.fPrimaryNamespace = fApplicationNamespace;
        newState.fFrameworkNamespace = fFrameworkNamespace;
        newState.fCleanTemplates = fCleanTemplates;

        // save the processed binary libraries
        saveBinaryLibraries(fFrameworkNamespace, fApplicationNamespace, newState);
        fInfrastructure.persistState(newState);
    }

    protected void saveBinaryLibraries(ICoreNamespace framework, ICoreNamespace namespace,
            State state)
    {
        saveBinaryLibraries(framework, state);
        saveBinaryLibraries(namespace, state);
    }

    private void saveBinaryLibraries(ICoreNamespace namespace, State state)
    {
        ICoreResource location = (ICoreResource) namespace.getSpecificationLocation();
        if (location.isBinaryResource())
            state.fBinaryNamespaces.put(location, namespace);

        for (Iterator iter = namespace.getChildIds().iterator(); iter.hasNext();)
        {
            String id = (String) iter.next();
            ICoreNamespace child = (ICoreNamespace) namespace.getChildNamespace(id);
            if (child != null)
                saveBinaryLibraries(child, state);
        }
    }

    public void cleanUp()
    {
        super.cleanUp();
    }

    protected void findDeclaredApplication()
    {
        ICoreResource webXML = (ICoreResource) fInfrastructure.fContextRoot
                .getRelativeResource("WEB-INF/web.xml");

        if (webXML.exists())
        {
            IDOMModel model = null;
            try
            {
                fInfrastructure.fNotifier.subTask(CoreMessages.format(
                        AbstractBuildInfrastructure.STRING_KEY + "scanning",
                        webXML.toString()));
                fInfrastructure.fProblemPersister.removeAllProblemsFor(webXML);

                try
                {
                    model = getDOMModel(webXML, null, fInfrastructure.fValidateWebXML);
                }
                catch (IOException e)
                {
                    TapestryCore.log(e);
                }

                if (model == null)
                {
                    // fInfrastructure.fProblemPersister.recordProblems(webXML,
                    // model.getProblems());
                    throw new BrokenWebXMLException(
                            "Tapestry AbstractBuild failed: could not parse web.xml. ");
                }

                WebAppDescriptor descriptor = null;

                WebXMLScanner wscanner = fInfrastructure.createWebXMLScanner();
                try
                {
                    descriptor = wscanner.scanWebAppDescriptor(model);
                }
                catch (ScannerException e)
                {
                    TapestryCore.log(e);
                }
                fInfrastructure.fProblemPersister.recordProblems(webXML, wscanner.getProblems());

                if (descriptor == null)
                    throw new BrokenWebXMLException(CoreMessages
                            .format(AbstractBuildInfrastructure.STRING_KEY
                                    + "abort-no-valid-application-servlets-found"));

                ServletInfo[] servletInfos = descriptor.getServletInfos();
                if (servletInfos == null || servletInfos.length == 0)

                    throw new BrokenWebXMLException(CoreMessages
                            .format(AbstractBuildInfrastructure.STRING_KEY
                                    + "abort-no-valid-application-servlets-found"));

                if (servletInfos.length > 1)
                    throw new BrokenWebXMLException(CoreMessages
                            .format(AbstractBuildInfrastructure.STRING_KEY
                                    + "abort-too-many-valid-servlets-found"));

                fApplicationServlet = servletInfos[0];
                fWebAppDescriptor = descriptor;
                fInfrastructure.installBasePropertySource(fWebAppDescriptor);
            }
            finally
            {
                if (model != null)
                    model.release();
            }
        }
        else
        {
            ICoreResource definedWebRoot = (ICoreResource) fInfrastructure.fTapestryProject
                    .getWebContextLocation();
            if (definedWebRoot != null || !definedWebRoot.exists())

                fInfrastructure.fProblemPersister.recordProblem(
                        fInfrastructure.fTapestryProject,
                        new DefaultProblem(IProblem.WARNING, CoreMessages.format(
                                AbstractBuildInfrastructure.STRING_KEY + "missing-context",
                                definedWebRoot.toString()), SourceLocation.FOLDER_LOCATION, false,
                                IProblem.NOT_QUICK_FIXABLE));
        }
    }

}