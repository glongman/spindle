package net.sf.spindle.core.messages;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.hivemind.HiveMind;
import org.apache.hivemind.Resource;
import org.apache.tapestry.IComponent;
import org.apache.tapestry.INamespace;
import org.apache.tapestry.engine.IEngineService;
import org.apache.tapestry.services.Infrastructure;
import org.apache.tapestry.spec.IComponentSpecification;
import org.apache.tapestry.spec.IContainedComponent;

public class ImplMessages
{
    protected static MessageFormatter _formatter = new MessageFormatter(
            "org.apache.tapestry.impl.ImplMessages", "ImplStrings");

    public static String initializerContribution()
    {
        return _formatter.getMessage("initializer-contribution");
    }

    // public static String noApplicationSpecification(HttpServlet servlet)
    // {
    // return _formatter.format("no-application-specification", servlet.getServletName());
    // }

    public static String errorInstantiatingEngine(Class engineClass, Throwable cause)
    {
        return _formatter.format("error-instantiating-engine", engineClass.getName(), cause);
    }

    public static String noTemplateForComponent(String componentId, Locale locale)
    {
        return _formatter.format("no-template-for-component", componentId, locale);
    }

    public static String noTemplateForPage(String pageName, Locale locale)
    {
        return _formatter.format("no-template-for-page", pageName, locale);
    }

    public static String unableToReadTemplate(Object template)
    {
        return _formatter.format("unable-to-read-template", template);
    }

    public static String unableToParseTemplate(Resource resource)
    {
        return _formatter.format("unable-to-parse-template", resource);
    }

    public static String unableToParseSpecification(Resource resource)
    {
        return _formatter.format("unable-to-parse-specification", resource);
    }

    public static String unableToReadInfrastructureProperty(String propertyName,
            Infrastructure service, Throwable cause)
    {
        return _formatter.format(
                "unable-to-read-infrastructure-property",
                propertyName,
                service,
                cause);
    }

    public static String multipleComponentReferences(String componentName, String id)
    {
        return _formatter.format("multiple-component-references", componentName, id);
    }

    public static String dupeComponentId(String id, IContainedComponent containedComponent)
    {
        return _formatter.format("dupe-component-id", id, HiveMind
                .getLocationString(containedComponent));
    }

    public static String unbalancedCloseTags()
    {
        return _formatter.getMessage("unbalanced-close-tags");
    }

    public static String templateBindingForInformalParameter(IComponent loadComponent,
            String parameterName, IComponent component)
    {
        return _formatter.format("template-binding-for-informal-parameter", loadComponent
                .getExtendedId(), parameterName, component.getExtendedId());
    }

    public static String templateBindingForReservedParameter(IComponent loadComponent,
            String parameterName, IComponent component)
    {
        return _formatter.format("template-binding-for-reserved-parameter", loadComponent
                .getExtendedId(), parameterName, component.getExtendedId());
    }

    public static String missingComponentSpec(IComponent component, Collection<String> ids)
    {
        StringBuffer buffer = new StringBuffer();
        List<String> idList = new ArrayList<String>(ids);
        int count = idList.size();

        for (int i = 0; i < count; i++)
        {
            if (i > 0)
                buffer.append(", ");

            buffer.append(idList.get(i));
        }

        return _formatter.format("missing-component-spec", component.getExtendedId(), new Integer(
                count), buffer.toString());
    }

    public static String bodylessComponent()
    {
        return _formatter.getMessage("bodyless-component");
    }

    public static String dupeTemplateBinding(String name, IComponent component,
            IComponent loadComponent)
    {
        return _formatter.format(
                "dupe-template-binding",
                name,
                component.getExtendedId(),
                loadComponent.getExtendedId());
    }

    public static String unableToLoadProperties(URL url, Throwable cause)
    {
        return _formatter.format("unable-to-load-properties", url, cause);
    }

    public static String noSuchService(String name)
    {
        return _formatter.format("no-such-service", name);
    }

    // public static String dupeService(String name, EngineServiceContribution existing)
    // {
    // return _formatter.format("dupe-service", name, HiveMind.getLocationString(existing));
    // }

    public static String unableToParseExpression(String expression, Throwable cause)
    {
        return _formatter.format("unable-to-parse-expression", expression, cause);
    }

    public static String parsedExpression()
    {
        return _formatter.getMessage("parsed-expression");
    }

    public static String unableToReadExpression(String expression, Object target, Throwable cause)
    {
        return _formatter.format("unable-to-read-expression", expression, target, cause);
    }

    public static String unableToWriteExpression(String expression, Object target, Object value,
            Throwable cause)
    {
        return _formatter.format("unable-to-write-expression", new Object[]
        { expression, target, value, cause });
    }

    public static String isConstantExpressionError(String expression, Exception ex)
    {
        return _formatter.format("is-constant-expression-error", expression, ex);
    }

    public static String templateParameterName(String name)
    {
        return _formatter.format("template-parameter-name", name);
    }

    public static String componentPropertySourceDescription(IComponentSpecification spec)
    {
        return _formatter.format("component-property-source-description", spec
                .getSpecificationLocation());
    }

    public static String namespacePropertySourceDescription(INamespace namespace)
    {
        return _formatter
                .format("namespace-property-source-description", namespace.getExtendedId());
    }

    public static String invalidEncoding(String encoding, Throwable cause)
    {
        return _formatter.format("invalid-encoding", encoding, cause);
    }

    public static String errorResetting(Throwable cause)
    {
        return _formatter.format("error-resetting", cause);
    }

    public static String engineServiceInnerProxyToString(String serviceName)
    {
        return _formatter.format("engine-service-inner-proxy-to-string", serviceName);
    }

    public static String engineServiceOuterProxyToString(String serviceName)
    {
        return _formatter.format("engine-service-outer-proxy-to-string", serviceName);
    }

    public static String serviceNameMismatch(IEngineService service, String expectedName,
            String actualName)
    {
        return _formatter.format("service-name-mismatch", service, expectedName, actualName);
    }

    public static String infrastructureAlreadyInitialized(String newMode, String initializedMode)
    {
        return _formatter.format("infrastructure-already-initialized", newMode, initializedMode);
    }

    // public static String duplicateInfrastructureContribution(InfrastructureContribution conflict,
    // Location existingLocation)
    // {
    // return _formatter.format(
    // "duplicate-infrastructure-contribution",
    // conflict.getProperty(),
    // conflict.getMode(),
    // existingLocation);
    // }

    public static String infrastructureNotInitialized()
    {
        return _formatter.getMessage("infrastructure-not-initialized");
    }

    public static String missingInfrastructureProperty(String propertyName)
    {
        return _formatter.format("missing-infrastructure-property", propertyName);
    }
}