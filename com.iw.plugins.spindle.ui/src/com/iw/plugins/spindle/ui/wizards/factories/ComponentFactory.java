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
package com.iw.plugins.spindle.ui.wizards.factories;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.tapestry.parse.SpecificationParser;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IType;

import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.core.spec.PluginComponentSpecification;
import com.iw.plugins.spindle.core.util.XMLUtil;

public class ComponentFactory
{
    public static IFile createComponent(
        IResourceWorkspaceLocation namespaceLocation,
        String componentName,
        IType specClass,
        IProgressMonitor monitor)
        throws CoreException, InterruptedException
    {

        monitor.beginTask(UIPlugin.getString("ApplicationFactory.operationdesc", componentName), 3);
        String fileName = componentName + ".jwc";
        IFile namespaceFile = (IFile) namespaceLocation.getStorage();
        IContainer container = (IContainer) namespaceFile.getParent();
        IFile newFile = container.getFile(new Path("/" + fileName));

        monitor.worked(1);

        String qualifiedSpecClassname = specClass.getFullyQualifiedName();
        InputStream contents = new ByteArrayInputStream(getComponentContent(qualifiedSpecClassname).getBytes());
        monitor.worked(1);
        newFile.create(contents, false, new SubProgressMonitor(monitor, 1));
        monitor.worked(1);
        monitor.done();
        return newFile;
    }

    static private String getComponentContent(String qualifiedSpecClassname)
    {
        PluginComponentSpecification newSpec = new PluginComponentSpecification();
        newSpec.setPublicId(SpecificationParser.TAPESTRY_DTD_3_0_PUBLIC_ID);
        newSpec.setAllowBody(true);
        newSpec.setAllowInformalParameters(true);
        newSpec.setComponentClassName(qualifiedSpecClassname);
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        XMLUtil.writeComponentSpecification(pwriter, newSpec, 0);
        return swriter.toString();
    }
}
