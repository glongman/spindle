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
package com.iw.plugins.spindle.ui.wizards.fields;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import com.iw.plugins.spindle.UIPlugin;
import com.iw.plugins.spindle.core.util.SpindleStatus;

public class ComponentNameField extends AbstractNameField
{

    /**
     * Constructor for ComponentNameField.
     * @param fieldName
     * @param labelWidth
     */
    public ComponentNameField(String fieldName, int labelWidth)
    {
        super(fieldName, labelWidth);
    }

    /**
     * Constructor for ApplicationNameField
     */
    public ComponentNameField(String fieldName)
    {
        super(fieldName);
    }

    protected IStatus nameChanged()
    {
        SpindleStatus status = new SpindleStatus();
        String name = getTextValue();
        if ("".equals(name))
        {
            status.setError("");
            return status;
        }
        if (name.indexOf('.') != -1)
        {
            status.setError(UIPlugin.getString(fName + ".error.QualifiedName"));
            return status;
        }

        IStatus val = JavaConventions.validateJavaTypeName(name);
        if (!val.isOK())
        {
            if (val.getSeverity() == IStatus.ERROR)
            {
                status.setError(UIPlugin.getString(fName + ".error.InvalidComponentName", val.getMessage()));
                return status;
            } else if (val.getSeverity() == IStatus.WARNING)
            {
                String message = val.getMessage();
                message = message.replaceAll("Java", "Tapestry");
                message = message.replaceAll("type", "component/page");
                status.setWarning(
                    UIPlugin.getString(fName + ".warning.ComponentNameDiscouraged", name,  message));
                return status;
            }
        }
        if (fPackageField != null && fPackageField.getPackageFragment() != null)
        {
            try
            {
                IContainer folder = (IContainer) fPackageField.getPackageFragment().getUnderlyingResource();
                IFile file = folder.getFile(new Path(name + ".jwc"));
                if (file.exists())
                {
                    status.setError(UIPlugin.getString(fName + ".error.ComponentAlreadyExists", name));
                }
            } catch (JavaModelException e)
            {
                // do nothing
            }
        }
        char first = name.charAt(0);
        if (Character.isLowerCase(first))
        {
            status.setWarning(
                UIPlugin.getString(fName + ".warning.ComponentNameDiscouraged", "first character is lowercase"));
        }

        return status;

    }
}