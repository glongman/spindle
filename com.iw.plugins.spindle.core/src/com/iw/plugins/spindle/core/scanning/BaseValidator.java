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

package com.iw.plugins.spindle.core.scanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.tapestry.IResourceLocation;
import org.apache.tapestry.spec.IAssetSpecification;
import org.apache.tapestry.spec.IComponentSpecification;
import org.apache.tapestry.spec.IContainedComponent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IType;

import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.builder.Build;
import com.iw.plugins.spindle.core.builder.IDependencyListener;
import com.iw.plugins.spindle.core.resources.IResourceWorkspaceLocation;
import com.iw.plugins.spindle.core.source.IProblem;
import com.iw.plugins.spindle.core.source.IProblemCollector;
import com.iw.plugins.spindle.core.source.ISourceLocation;
import com.iw.plugins.spindle.core.source.ISourceLocationInfo;

/**
 * TODO Add Type comment
 * 
 * @author glongman@gmail.com
 */
public class BaseValidator implements IScannerValidator
{

    static class SLocation implements ISourceLocation
    {
        public int getCharEnd()
        {
            return 1;
        }

        public int getCharStart()
        {
            return 0;
        }

        public int getLineNumber()
        {
            return 1;
        }

        public int getLength()
        {
            return getCharEnd() - getCharStart() + 1;
        }

        public boolean contains(int cursorPosition)
        {
            return cursorPosition == 0 || cursorPosition == 1;
        }

        public ISourceLocation getLocationOffset(int cursorPosition)
        {
            return this;
        }

    }

    private List fDeferred;

    public static final String DefaultDummyString = "1~dummy<>";

    public static final ISourceLocation DefaultSourceLocation = new SLocation();

    /**
     * Map of compiled Patterns, keyed on pattern string. Patterns are lazily compiled as needed.
     */

    protected Map fCompiledPatterns;

    protected String fDummyString = DefaultDummyString;

    /**
     * Matcher used to match patterns against input strings.
     */

    protected PatternMatcher fMatcher;

    /**
     * Compiler used to convert pattern strings into Patterns instances.
     */

    protected PatternCompiler fPatternCompiler;

    protected IProblemCollector fProblemCollector;

    private List fListeners;

    /**
     *  
     */
    public BaseValidator()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#addListener(com.iw.plugins.spindle.core.scanning.IScannerValidatorListener)
     */
    public void addListener(IScannerValidatorListener listener)
    {
        if (fListeners == null)
            fListeners = new ArrayList();

        if (!fListeners.contains(listener))
            fListeners.add(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#removeListener(com.iw.plugins.spindle.core.scanning.IScannerValidatorListener)
     */
    public void removeListener(IScannerValidatorListener listener)
    {
        if (fListeners != null)
            fListeners.remove(listener);
    }

    /**
     * Notify listeners that a type check occured.
     * 
     * @param fullyQulaifiedName
     *            the name of the type
     * @param result
     *            the resolved IType, if any
     */
    protected void fireTypeDependency(IResourceWorkspaceLocation dependant,
            String fullyQualifiedName, IType result)
    {
        if (fListeners == null)
            return;

        for (Iterator iter = fListeners.iterator(); iter.hasNext();)
        {
            IScannerValidatorListener listener = (IScannerValidatorListener) iter.next();
            listener.typeChecked(fullyQualifiedName, result); //TODO
            // remove
            // eventually
        }

        IDependencyListener depListener = Build.getDependencyListener();
        if (depListener != null && fullyQualifiedName != null
                && fullyQualifiedName.trim().length() > 0)
            depListener.foundTypeDependency(dependant, fullyQualifiedName);

    }

    /**
     * Returns a pattern compiled for single line matching
     */
    protected Pattern compilePattern(String pattern)
    {
        if (fPatternCompiler == null)
            fPatternCompiler = new Perl5Compiler();

        try
        {
            return fPatternCompiler.compile(pattern, Perl5Compiler.SINGLELINE_MASK);
        }
        catch (MalformedPatternException ex)
        {

            throw new Error(ex);
        }
    }

    /**
     * Base Implementation always fails!
     * 
     * @param fullyQualifiedName
     * @return
     */
    public IType findType(IResourceWorkspaceLocation dependant, String fullyQualifiedName)
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#getDummyStringPrefix()
     */
    public String getDummyStringPrefix()
    {
        return fDummyString;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#getNextDummyString()
     */
    public String getNextDummyString()
    {
        return fDummyString + System.currentTimeMillis();
    }

    public IProblemCollector getProblemCollector()
    {
        return fProblemCollector;
    }

    public void setProblemCollector(IProblemCollector collector)
    {
        fProblemCollector = collector;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#validateAsset(org.apache.tapestry.spec.IComponentSpecification,
     *      org.apache.tapestry.IAsset, com.iw.plugins.spindle.core.parser.ISourceLocationInfo)
     */
    public boolean validateAsset(IComponentSpecification specification, IAssetSpecification asset,
            ISourceLocationInfo sourceLocation) throws ScannerException
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#validateContainedComponent(org.apache.tapestry.spec.IComponentSpecification,
     *      org.apache.tapestry.spec.IContainedComponent)
     */
    public boolean validateContainedComponent(IComponentSpecification specification,
            IContainedComponent component, ISourceLocationInfo info) throws ScannerException
    {
        return true;
    }

    public boolean validateExpression(String expression, int severity) throws ScannerException
    {
        return validateExpression(expression, severity, DefaultSourceLocation);
    }

    public boolean validateExpression(String expression, int severity, ISourceLocation location)
            throws ScannerException
    {
        if (!expression.startsWith(fDummyString))
        {
            try
            {
                Ognl.parseExpression(expression);
            }
            catch (OgnlException e)
            {
                addProblem(
                        severity,
                        location,
                        e.getMessage(),
                        false,
                        IProblem.SPINDLE_MALFORMED_OGNL_EXPRESSION);
                return false;
            }
        }
        return true;
    }

    public void addProblem(int severity, ISourceLocation location, String message,
            boolean isTemporary, int code) throws ScannerException
    {
        if (fProblemCollector == null)
        {
            throw new ScannerException(message, isTemporary, code);
        }
        else
        {
            fProblemCollector.addProblem(severity, (location == null ? DefaultSourceLocation
                    : location), message, isTemporary, code);
        }
    }

    public void addProblem(IStatus status, ISourceLocation location, boolean isTemporary)
            throws ScannerException
    {
        if (fProblemCollector == null)
        {
            throw new ScannerException(status.getMessage(), isTemporary, status.getCode());
        }
        else
        {
            fProblemCollector.addProblem(status, (location == null ? DefaultSourceLocation
                    : location), isTemporary);
        }
    }

    public boolean validatePattern(String value, String pattern, String errorKey, int severity, int code)
            throws ScannerException
    {
        return validatePattern(value, pattern, errorKey, severity, DefaultSourceLocation, code);
    }

    public boolean validatePattern(String value, String pattern, String errorKey, int severity,
            ISourceLocation location, int code) throws ScannerException
    {

        if (value != null && value.startsWith(fDummyString))
            return true;

        if (value != null)
        {
            if (fCompiledPatterns == null)
                fCompiledPatterns = new HashMap();

            Pattern compiled = (Pattern) fCompiledPatterns.get(pattern);

            if (compiled == null)
            {
                compiled = compilePattern(pattern);

                fCompiledPatterns.put(pattern, compiled);
            }

            if (fMatcher == null)
                fMatcher = new Perl5Matcher();

            if (!fMatcher.matches(value, compiled))
            {
                addProblem(
                        severity,
                        location,
                        TapestryCore.getTapestryString(errorKey, value),
                        false,
                        code);
                return false;
            }
            return true;
        }
        addProblem(
                severity,
                location,
                TapestryCore.getTapestryString(errorKey, "null value"),
                false,
                -1);
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#validateResourceLocation(java.lang.String,
     *      java.lang.String, com.iw.plugins.spindle.core.parser.ISourceLocation)
     */
    public boolean validateLibraryResourceLocation(IResourceLocation parentSpecLocation, String path,
            String errorKey, ISourceLocation source) throws ScannerException
    {
        return false;
    }

    public boolean validateResourceLocation(IResourceLocation location, String relativePath,
            String errorKey, ISourceLocation source) throws ScannerException
    {
        return validateResourceLocation(location, relativePath, errorKey, source, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#validateResourceLocation(org.apache.tapestry.IResourceLocation,
     *      java.lang.String)
     */
    public boolean validateResourceLocation(IResourceLocation location, String relativePath,
            String errorKey, ISourceLocation source, boolean accountForI18N)
            throws ScannerException
    {
        if (relativePath == null || relativePath.startsWith(getDummyStringPrefix()))
            return false;

        if (!resourceLocationExists(location, relativePath))
        {
            IResourceWorkspaceLocation relative = (IResourceWorkspaceLocation) location
                    .getRelativeLocation(relativePath);
            addProblem(
                    IProblem.ERROR,
                    source,
                    TapestryCore.getString(errorKey, relative.toString()),
                    true,
                    IProblem.SPINDLE_RESOURCE_LOCATION_DOES_NOT_EXIST);

            return false;
        }
        return true;
    }

    protected boolean resourceLocationExists(IResourceLocation location, String relativePath)
    {
        IResourceWorkspaceLocation real = (IResourceWorkspaceLocation) location;
        IResourceWorkspaceLocation relative = (IResourceWorkspaceLocation) real
                .getRelativeLocation(relativePath);
        return relative.getStorage() != null;
    }

    public IType validateTypeName(IResourceWorkspaceLocation dependant, String fullyQualifiedType,
            int severity) throws ScannerException
    {
        return validateTypeName(dependant, fullyQualifiedType, severity, DefaultSourceLocation);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#validateTypeName(java.lang.String)
     */
    public IType validateTypeName(IResourceWorkspaceLocation dependant, String fullyQualifiedType,
            int severity, ISourceLocation location) throws ScannerException
    {

        if (fullyQualifiedType == null)
        {
            addProblem(severity, location, TapestryCore.getTapestryString(
                    "unable-to-resolve-class",
                    "null value"), true, -1);
            return null;
        }

        IType type = findType(dependant, fullyQualifiedType);
        if (type == null)
        {
            addProblem(severity, location, TapestryCore.getTapestryString(
                    "unable-to-resolve-class",
                    fullyQualifiedType), true, IProblem.SPINDLE_MISSING_TYPE);
            return null;
        }
        return type;
    }

}