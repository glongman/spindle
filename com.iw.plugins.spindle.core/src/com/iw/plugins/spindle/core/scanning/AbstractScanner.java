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

package com.iw.plugins.spindle.core.scanning;

import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry.IResourceLocation;
import org.apache.tapestry.spec.IAssetSpecification;
import org.apache.tapestry.spec.IComponentSpecification;
import org.apache.tapestry.spec.IContainedComponent;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.iw.plugins.spindle.core.ITapestryMarker;
import com.iw.plugins.spindle.core.TapestryCore;
import com.iw.plugins.spindle.core.builder.TapestryBuilder;
import com.iw.plugins.spindle.core.parser.DefaultProblem;
import com.iw.plugins.spindle.core.parser.IProblem;
import com.iw.plugins.spindle.core.parser.IProblemCollector;
import com.iw.plugins.spindle.core.parser.ISourceLocation;
import com.iw.plugins.spindle.core.parser.ISourceLocationInfo;
import com.iw.plugins.spindle.core.parser.Parser;
import com.iw.plugins.spindle.core.parser.ParserRuntimeException;
import com.iw.plugins.spindle.core.util.Assert;

/**
 *  Base Class for Node processors
 * <p>
 *  Node Processors can find problems, but these problems do not
 *  represent a list of *all* the problems with this document.
 * </p>
 * <p>
 *  i.e.The Parser will hold problems for things like well-formedness and dtd validation!
 * </p>
 * 
 * @author glongman@intelligentworks.com
 * @version $Id$
 */
public abstract class AbstractScanner implements IProblemCollector
{

    private Object resultObject;

    protected List problems = new ArrayList();
    protected IScannerValidator validator;
    protected Parser parser;

    public final Object scan(final Parser parser, IScannerValidator validator, Node node) throws ScannerException
    {
        Assert.isNotNull(node);
        problems.clear();
        this.parser = parser;
        if (validator == null)
        {
            this.validator = new BaseValidator();
        } else
        {
            this.validator = validator;
        }
        this.validator.setProblemCollector(this);
        resultObject = beforeScan(node);
        if (resultObject == null)
        {
            return null;
        }
        try
        {
            doScan(resultObject, node);

        } catch (ParserRuntimeException e)
        {
            // do nothing - return what we have so far
            // this could only happen when pull parsing!
            e.printStackTrace();
        }
        return afterScan(resultObject);
    }

    protected abstract void doScan(Object resultObject, Node rootNode) throws ScannerException;

    protected abstract Object beforeScan(Node rootNode) throws ScannerException;

    protected Object afterScan(Object scanResults) throws ScannerException
    {
        return scanResults;
    }

    public void addProblem(IProblem problem)
    {

        problems.add(problem);
    }

    public void addProblem(int severity, ISourceLocation location, String message)
    {
        addProblem(
            new DefaultProblem(
                ITapestryMarker.TAPESTRY_PROBLEM_MARKER,
                severity,
                message,
                location.getLineNumber(),
                location.getCharStart(),
                location.getCharEnd()));
    }

    public IProblem[] getProblems()
    {
        return (IProblem[]) problems.toArray(new IProblem[problems.size()]);
    }

    public boolean isElement(Node node, String elementName)
    {
        if (node.getNodeType() != Node.ELEMENT_NODE)
        {
            return false;
        }
        return node.getNodeName().equals(elementName);

    }

    public String getValue(Node node)
    {
        StringBuffer buffer = new StringBuffer();
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
        {
            Text text = (Text) child;
            buffer.append(text.getData());
        }

        String result = buffer.toString().trim();
        if (result == null || "".equals(result))
        {
            return null;
        }

        return result;
    }

    protected String getAttribute(Node node, String attributeName, boolean returnDummyIfNull)
    {
        String result = null;
        NamedNodeMap map = node.getAttributes();

        if (map != null)
        {
            Node attributeNode = map.getNamedItem(attributeName);

            if (attributeNode != null)
            {
                result = attributeNode.getNodeValue();
            }
        }
        if (TapestryCore.isNull(result) && returnDummyIfNull)
        {
            result = getNextDummyString();
        }
        return result;
    }
    
    protected boolean isDummyString(String value) {
        if (value != null) {
            return value.startsWith(validator.getDummyStringPrefix());
        }
        return false;
    }

    protected String getAttribute(Node node, String attributeName)
    {
        return getAttribute(node, attributeName, false);
    }

    protected boolean getBooleanAttribute(Node node, String attributeName)
    {
        String attributeValue = getAttribute(node, attributeName);

        return attributeValue.equals("yes");
    }

    protected ISourceLocationInfo getSourceLocationInfo(Node node)
    {
        return parser.getSourceLocationInfo(node);
    }

    protected ISourceLocation getBestGuessSourceLocation(Node node, boolean forNodeContent)
    {
        ISourceLocationInfo info = getSourceLocationInfo(node);
        if (TapestryBuilder.DEBUG)
        {
            System.out.println(node.getNodeName());
            System.out.println(info);
        }
        if (info != null)
        {
            if (forNodeContent)
            {
                if (!info.isEmptyTag())
                {
                    return info.getContentSourceLocation();
                } else
                {
                    return info.getStartTagSourceLocation();
                }
            } else
            {
                return info.getStartTagSourceLocation();
            }
        }
        return null;
    }

    protected ISourceLocation getNodeStartSourceLocation(Node node)
    {
        ISourceLocationInfo info = getSourceLocationInfo(node);
        ISourceLocation result = null;
        if (info != null)
        {
            result = info.getStartTagSourceLocation();

        }
        return result;
    }

    protected ISourceLocation getNodeEndSourceLocation(Node node)
    {
        ISourceLocationInfo info = getSourceLocationInfo(node);
        ISourceLocation result = null;
        if (info != null)
        {
            result = info.getEndTagSourceLocation();
            if (result == null)
            {
                result = info.getStartTagSourceLocation();
            }
        }
        return result;
    }

    protected ISourceLocation getAttributeSourceLocation(Node node, String rawname)
    {
        ISourceLocationInfo info = getSourceLocationInfo(node);
        ISourceLocation result = null;
        if (info != null)
        {
            result = info.getAttributeSourceLocation(rawname);
            if (result == null)
            {
                result = info.getStartTagSourceLocation();
            }
        }
        return result;
    }

    protected boolean validatePattern(String value, String pattern, String errorKey, int severity) throws ScannerException
    {
        return validator.validatePattern(value, pattern, errorKey, severity);
    }

    protected boolean validatePattern(String value, String pattern, String errorKey, int severity, ISourceLocation location)
        throws ScannerException
    {
        return validator.validatePattern(value, pattern, errorKey, severity, location);
    }

    protected boolean validateExpression(String expression, int severity) throws ScannerException
    {
        return validator.validateExpression(expression, severity);
    }

    protected boolean validateExpression(String expression, int severity, ISourceLocation location) throws ScannerException
    {
        return validator.validateExpression(expression, severity, location);
    }

    protected boolean validateTypeName(String fullyQualifiedType, int severity) throws ScannerException
    {
        return validator.validateTypeName(fullyQualifiedType, severity);
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#validateTypeName(java.lang.String)
     */
    protected boolean validateTypeName(String fullyQualifiedType, int severity, ISourceLocation location) throws ScannerException
    {
        return validator.validateTypeName(fullyQualifiedType, severity, location);
    }

    protected boolean validateResourceLocation(
        IResourceLocation location,
        String relativePath,
        String errorKey,
        ISourceLocation source)
        throws ScannerException
    {
        return validator.validateResourceLocation(location, relativePath, errorKey, source);

    }

    protected boolean validateContainedComponent(
        IComponentSpecification specification,
        IContainedComponent component,
        ISourceLocationInfo sourceLocation)
        throws ScannerException
    {
        return validator.validateContainedComponent(specification, component, sourceLocation);
    }

    protected boolean validateAsset(
        IComponentSpecification specification,
        IAssetSpecification asset,
        ISourceLocationInfo sourceLocation)
        throws ScannerException
    {
        return validator.validateAsset(specification, asset, sourceLocation);
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#getNextDummyString()
     */
    protected String getNextDummyString()
    {
        return validator.getDummyStringPrefix();
    }

    /* (non-Javadoc)
     * @see com.iw.plugins.spindle.core.scanning.IScannerValidator#getDummyStringPrefix()
     */
    protected String getDummyStringPrefix()
    {
        return validator.getDummyStringPrefix();
    }

}