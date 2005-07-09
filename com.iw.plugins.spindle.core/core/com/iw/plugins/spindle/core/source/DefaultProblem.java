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

package com.iw.plugins.spindle.core.source;

/**
 * Default impl of IProblem
 * 
 * @author glongman@gmail.com
 */
public class DefaultProblem implements IProblem
{

    private int fCharEnd;

    private int fCharStart;

    private int fLineNumber;

    private String fMessage;

    private int fSeverity;

    private String fType;

    private boolean fTemporary;

    private int fCode;

    public DefaultProblem(int severity, String message, ISourceLocation location,
            boolean isTemporary, int code)
    {
        this(IProblem.TAPESTRY_PROBLEM_MARKER, severity, message, location != null ? location
                : SourceLocation.FILE_LOCATION, isTemporary, code);

    }

    public DefaultProblem(String type, int severity, String message, ISourceLocation location,
            boolean isTemporary, int code)
    {
        this(type, severity, message, location.getLineNumber(), location.getCharStart(), location
                .getCharEnd(), isTemporary, code);
    }

    protected DefaultProblem(int severity, String message, int lineNumber, int charStart,
            int charEnd, boolean isTemporary, int code)
    {
        this(IProblem.TAPESTRY_PROBLEM_MARKER, severity, message, lineNumber, charStart, charEnd,
                isTemporary, code);
    }

    protected DefaultProblem(String type, int severity, String message, int lineNumber,
            int charStart, int charEnd, boolean isTemporary, int code)
    {
        fType = type;
        fSeverity = severity;
        fMessage = message;
        fLineNumber = lineNumber;
        fCharStart = charStart;
        fCharEnd = charEnd;
        fTemporary = isTemporary;
        fCode = code;
    }

    public String toString()
    {
        String name = getClass().getName();
        int index = name.lastIndexOf(".");
        if (index > 0)
            name = name.substring(index + 1);

        StringBuffer buffer = new StringBuffer(name);
        buffer.append("[");
        switch (fSeverity)
        {
            case ERROR:
                buffer.append("ERROR");
                break;
            case WARNING:
                buffer.append("WARNING");
                break;
            case INFO:
                buffer.append("INFO");
                break;

            default:
                buffer.append("NOT SET");
                break;
        }
        buffer.append(", ");
        buffer.append("L=");
        buffer.append(getLineNumber());
        buffer.append(", ");
        buffer.append("CS=");
        buffer.append(getCharStart());
        buffer.append(", ");
        buffer.append("CE=");
        buffer.append(getCharEnd());
        buffer.append(", ");
        buffer.append(getMessage());
        buffer.append("]");
        return buffer.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.parser.IProblem#getCharEnd()
     */
    public int getCharEnd()
    {
        return fCharEnd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.parser.IProblem#getCharStart()
     */
    public int getCharStart()
    {
        return fCharStart;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.parser.IProblem#getLineNumber()
     */
    public int getLineNumber()
    {
        return fLineNumber;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.parser.IProblem#getMessage()
     */
    public String getMessage()
    {
        return fMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.parser.IProblem#getSeverity()
     */
    public int getSeverity()
    {
        return fSeverity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.parser.IProblem#getType()
     */
    public String getType()
    {
        return fType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.source.IProblem#isTemporary()
     */
    public boolean isTemporary()
    {
        return fTemporary;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iw.plugins.spindle.core.source.IProblem#getCode()
     */
    public int getCode()
    {
        return fCode;
    }

}