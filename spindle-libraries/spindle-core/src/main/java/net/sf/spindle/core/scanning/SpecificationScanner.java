package net.sf.spindle.core.scanning;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.spindle.core.CoreMessages;
import net.sf.spindle.core.TapestryCore;
import net.sf.spindle.core.messages.ParseMessages;
import net.sf.spindle.core.parser.IDOMModel;
import net.sf.spindle.core.parser.validator.DOMValidator;
import net.sf.spindle.core.source.IProblem;
import net.sf.spindle.core.spec.IPluginDescribable;
import net.sf.spindle.core.spec.IPluginPropertyHolder;
import net.sf.spindle.core.spec.PluginDescriptionDeclaration;
import net.sf.spindle.core.spec.PluginPropertyDeclaration;
import net.sf.spindle.core.util.Assert;
import net.sf.spindle.core.util.W3CAccess;
import net.sf.spindle.core.util.XMLPublicIDUtil;

import org.apache.hivemind.Resource;
import org.apache.tapestry.engine.IPropertySource;
import org.apache.tapestry.spec.BeanLifecycle;
import org.apache.tapestry.util.IPropertyHolder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Scanner for building Tapestry Specs - this is the base class for Component, Library, and
 * Application scanners. This class handles some setup and validates the document by default.
 * 
 * @author glongman@gmail.com
 */
public abstract class SpecificationScanner extends AbstractDOMScanner
{

    protected Resource fResourceLocation;

    protected String fPublicId;

    protected int fPublicIdCode;

    protected boolean fIsTapestry_4_0;

    protected Node fRootNode;

    protected IPropertySource fPropertySource;

    protected boolean fValidating = true;

    /*
     * (non-Javadoc)
     * 
     * @see core.scanning.AbstractScanner#beforeScan(java.lang.Object)
     */
    protected Object beforeScan() throws ScannerException
    {
        checkPropertySource();

        IDOMModel model = getDOMModel();
        if (!isDocumentModel(model))
            return null;

        Document document = (Document) model.getDocument();
        if (document == null)
            return null;

        setPublicId(W3CAccess.getPublicId(document));
        if (fPublicId == null)
            throw new ScannerException(CoreMessages.invalidPublicID(), false,
                    IProblem.SPINDLE_MISSING_PUBLIC_ID);
        if (!checkPublicId())
        {
            throw new ScannerException(CoreMessages.invalidPublicID(), false,
                    IProblem.SPINDLE_INVALID_PUBLIC_ID);
        }

        fRootNode = document.getDocumentElement();
        return document;
    }

    protected void checkPropertySource()
    {
        // by default it's ok to have a null property source.
    }

    /**
     * @return
     */
    protected boolean checkPublicId()
    {
        boolean ok = false;
        if (fPublicId != null)
        {
            int version = XMLPublicIDUtil.getDTDVersion(fPublicId);
            if (version != XMLPublicIDUtil.UNKNOWN_DTD)
            {
                for (int i = 0; i < XMLPublicIDUtil.ALLOWED_SPEC_DTDS.length; i++)
                {
                    if (XMLPublicIDUtil.ALLOWED_SPEC_DTDS[i] == version)
                    {
                        ok = true;
                        break;
                    }
                }
            }
        }
        return ok;
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.scanning.AbstractScanner#doScan(java.lang.Object, java.lang.Object)
     */
    protected void doScan() throws ScannerException
    {
        validate(fSource);
    }

    protected void validate(Object source)
    {
        if (!isValidating())
            return;
        IDOMModel model = getDOMModel();
        if (model.getDocument() == null || model.hasFatalProblems())
            return;
        DOMValidator validator = new DOMValidator();
        validator.validate(model);
        addProblems(validator.getProblems());
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.scanning.AbstractScanner#afterScan(java.lang.Object)
     */
    protected Object afterScan() throws ScannerException
    {
        fResourceLocation = null;
        return super.afterScan();
    }

    public void setResourceLocation(Resource location)
    {
        fResourceLocation = location;
    }

    protected void setPublicId(String publicId)
    {
        fPublicId = publicId;
        fPublicIdCode = XMLPublicIDUtil.getDTDVersion(fPublicId);
        fIsTapestry_4_0 = fPublicIdCode == XMLPublicIDUtil.DTD_4_0;
    }

    public static interface IConverter
    {
        public Object convert(String value) throws ScannerException;
    }

    public static class BooleanConverter implements IConverter

    {
        public Object convert(String value) throws ScannerException
        {
            Object result = TYPE_CONVERSION_MAP.get(value.toLowerCase());

            if (result == null || !(result instanceof Boolean))
                throw new ScannerException(ParseMessages.failConvertBoolean(value), false,
                        IProblem.TAP_FAILED_TO_CONVERT_TO_BOOLEAN);

            return result;
        }
    }

    public static class IntConverter implements IConverter
    {
        public Object convert(String value) throws ScannerException
        {
            try
            {
                return new Integer(value);
            }
            catch (NumberFormatException ex)
            {
                throw new ScannerException(ParseMessages.failConvertInt(value), ex, false,
                        IProblem.TAP_FAILED_TO_CONVERT_TO_INT);
            }
        }
    }

    public static class LongConverter implements IConverter
    {
        public Object convert(String value) throws ScannerException
        {
            try
            {
                return new Long(value);
            }
            catch (NumberFormatException ex)
            {
                throw new ScannerException(ParseMessages.failConvertLong(value), ex, false,
                        IProblem.TAP_FAILED_TO_CONVERT_TO_LONG);
            }
        }
    }

    public static class DoubleConverter implements IConverter
    {
        public Object convert(String value) throws ScannerException
        {
            try
            {
                return new Double(value);
            }
            catch (NumberFormatException ex)
            {
                throw new ScannerException(ParseMessages.failConvertDouble(value), ex, false,
                        IProblem.TAP_FAILED_TO_CONVERT_TO_DOUBLE);
            }
        }
    }

    public static class StringConverter implements IConverter
    {
        public Object convert(String value)
        {
            return value.trim();
        }
    }

    static class IgnoreNodeException extends ScannerException
    {
        private static final long serialVersionUID = 1L;

        public IgnoreNodeException()
        {
            super("", false, -1);
        }

    }

    // /**
    // * @deprecated
    // */
    // protected SpecFactory fSpecificationFactory;

    /**
     * We can share a single map for all the XML attribute to object conversions, since the keys are
     * unique.
     */
    public static final Map<String, Object> TYPE_CONVERSION_MAP = new HashMap<String, Object>();

    public static final List<String> TYPE_LIST = new ArrayList<String>();

    // Identify all the different acceptible values.
    // We continue to sneak by with a single map because
    // there aren't conflicts; when we have 'foo' meaning
    // different things in different places in the DTD, we'll
    // need two maps.

    static
    {

        TYPE_CONVERSION_MAP.put("true", Boolean.TRUE);
        TYPE_CONVERSION_MAP.put("t", Boolean.TRUE);
        TYPE_CONVERSION_MAP.put("1", Boolean.TRUE);
        TYPE_CONVERSION_MAP.put("y", Boolean.TRUE);
        TYPE_CONVERSION_MAP.put("yes", Boolean.TRUE);
        TYPE_CONVERSION_MAP.put("on", Boolean.TRUE);
        TYPE_CONVERSION_MAP.put("aye", Boolean.TRUE);

        TYPE_CONVERSION_MAP.put("false", Boolean.FALSE);
        TYPE_CONVERSION_MAP.put("f", Boolean.FALSE);
        TYPE_CONVERSION_MAP.put("0", Boolean.FALSE);
        TYPE_CONVERSION_MAP.put("off", Boolean.FALSE);
        TYPE_CONVERSION_MAP.put("no", Boolean.FALSE);
        TYPE_CONVERSION_MAP.put("n", Boolean.FALSE);
        TYPE_CONVERSION_MAP.put("nay", Boolean.FALSE);

        TYPE_CONVERSION_MAP.put("none", BeanLifecycle.NONE);
        TYPE_CONVERSION_MAP.put("request", BeanLifecycle.REQUEST);
        TYPE_CONVERSION_MAP.put("page", BeanLifecycle.PAGE);
        TYPE_CONVERSION_MAP.put("render", BeanLifecycle.RENDER);

        TYPE_CONVERSION_MAP.put("boolean", new BooleanConverter());
        TYPE_CONVERSION_MAP.put("int", new IntConverter());
        TYPE_CONVERSION_MAP.put("double", new DoubleConverter());
        TYPE_CONVERSION_MAP.put("String", new StringConverter());
        TYPE_CONVERSION_MAP.put("long", new LongConverter());

        TYPE_LIST.add("boolean");
        TYPE_LIST.add("boolean[]");

        TYPE_LIST.add("short");
        TYPE_LIST.add("short[]");

        TYPE_LIST.add("int");
        TYPE_LIST.add("int[]");

        TYPE_LIST.add("long");
        TYPE_LIST.add("long[]");

        TYPE_LIST.add("float");
        TYPE_LIST.add("float[]");

        TYPE_LIST.add("double");
        TYPE_LIST.add("double[]");

        TYPE_LIST.add("char");
        TYPE_LIST.add("char[]");

        TYPE_LIST.add("byte");
        TYPE_LIST.add("byte[]");

        TYPE_LIST.add("java.lang.Object");
        TYPE_LIST.add("java.lang.Object[]");

        TYPE_LIST.add("java.lang.String");
        TYPE_LIST.add("java.lang.String[]");
    }

    protected boolean scanMeta(IPluginPropertyHolder holder, Node node) throws ScannerException
    {
        if (!fIsTapestry_4_0)
            return scanProperty_3_0(holder, node);

        if (!isElement(node, "meta"))
            return false;
        
        return scanProperty(holder, node, "key", "value");

    }

    private boolean scanProperty_3_0(IPluginPropertyHolder holder, Node node)
            throws ScannerException
    {
        if (!isElement(node, "property"))
            return false;
        scanProperty(holder, node, "name", "value");
        return true;
    }

    private boolean scanProperty(IPluginPropertyHolder holder, Node node, String keyAttribute,
            String valueAttribute) throws ScannerException
    {

        String name = getAttribute(node, keyAttribute, false);

        if (holder.getProperty(name) != null)
        {
            addProblem(
                    IProblem.WARNING,
                    getAttributeSourceLocation(node, keyAttribute),
                    "duplicate definition of property: " + name,
                    false,
                    IProblem.SPINDLE_DUPLICATE_PROPERTY_ID);
        }

        // must be done now - not revalidatable
        ExtendedAttributeResult result = null;
        String value = null;
        try
        {
            result = getExtendedAttribute(node, valueAttribute, true);
            value = result.value;
        }
        catch (ScannerException e)
        {
            addProblem(IProblem.ERROR, e.getLocation(), e.getMessage(), false, e.getCode());
        }

        PluginPropertyDeclaration declaration = new PluginPropertyDeclaration(name, value);
        declaration.setLocation(getSourceLocationInfo(node));
        declaration.setValueIsFromAttribute(result == null ? false : result.fromAttribute);

        if (value != null && value.trim().length() == 0)
        {
            addProblem(
                    IProblem.WARNING,
                    result.fromAttribute ? getAttributeSourceLocation(node, valueAttribute)
                            : getBestGuessSourceLocation(node, true),
                    "missing value of property: " + name,
                    false,
                    IProblem.MANUAL_FIX_ONLY);
        }

        holder.addPropertyDeclaration(declaration);

        return true;
    }

    /**
     * Used in several places where an element's only possible children are &lt;property&gt;
     * elements.
     */

    protected void allowMeta(IPropertyHolder holder, Node node) throws ScannerException
    {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling())
        {
            if (scanMeta((IPluginPropertyHolder) holder, node))
                continue;
        }
    }

    /**
     * Used with many elements that allow a value to be specified as either an attribute, or as
     * wrapped character data. This handles that case, and makes it an error to specify both.
     */

    protected ExtendedAttributeResult getExtendedAttribute(Node node, String attributeName,
            boolean required) throws ScannerException
    {

        String attributeValue = getAttribute(node, attributeName);
        boolean nullAttributeValue = TapestryCore.isNull(attributeValue);
        String bodyValue = getValue(node);
        boolean nullBodyValue = TapestryCore.isNull(bodyValue);

        if (!nullAttributeValue && !nullBodyValue)
            throw new ScannerException(ParseMessages.noAttributeAndBody(attributeName, node
                    .getNodeName()), getNodeBodySourceLocation(node), false,
                    IProblem.EXTENDED_ATTRIBUTE_BOTH_VALUE_AND_BODY);

        if (required && nullAttributeValue && nullBodyValue)
            throw new ScannerException(ParseMessages.requiredExtendedAttribute(
                    node.getNodeName(),
                    attributeName), getNodeStartSourceLocation(node), false,
                    IProblem.EXTENDED_ATTRIBUTE_NO_VALUE_OR_BODY);

        ExtendedAttributeResult result = new ExtendedAttributeResult();
        if (nullAttributeValue)
        {

            result.value = bodyValue;
        }
        else
        {
            result.value = attributeValue;
            result.fromAttribute = true;

        }

        return result;
    }

    static public class ExtendedAttributeResult
    {
        public String value;

        public boolean fromAttribute;
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.scanning.AbstractScanner#beforeScan(java.lang.Object)
     */
    protected boolean isDocumentModel(Object source) throws ScannerException
    {
        Assert.isLegal(fSource instanceof IDOMModel);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see core.scanning.AbstractScanner#cleanup()
     */
    protected void cleanup()
    {
    }

    /**
     * @return Returns the propertySource.
     */
    public IPropertySource getPropertySource()
    {
        return fPropertySource;
    }

    /**
     * @param propertySource
     *            The propertySource to set.
     */
    public void setPropertySource(IPropertySource propertySource)
    {
        this.fPropertySource = propertySource;
    }

    protected boolean scanDescription(IPluginDescribable describable, Node node)
    {
        if (!isElement(node, "description"))
            return false;

        String value = getValue(node);
        describable.setDescription(value);
        PluginDescriptionDeclaration declaration = new PluginDescriptionDeclaration(null, value,
                getSourceLocationInfo(node));
        describable.addDescriptionDeclaration(declaration);

        return true;
    }

    public boolean isValidating()
    {
        return fValidating;
    }

    public void setValidating(boolean validating)
    {
        this.fValidating = validating;
    }
}