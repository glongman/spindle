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
import net.sf.spindle.core.source.ISourceLocation;

/**
 * Exception type thrown by Processors
 * 
 * @author glongman@gmail.com
 */
public class ScannerException extends Exception
{
    private static final long serialVersionUID = 1L;

    ISourceLocation fLocation;

    int fCode = -1;

    boolean fTemporary = false;

    /**
     * @param arg0
     */
    public ScannerException(String message, boolean temporary, int code)
    {
        super(message);
        fCode = code;
        fTemporary = temporary;
    }

    /**
     * @param arg0
     * @param arg1
     */
    public ScannerException(String message, Throwable exception, boolean temporary, int code)
    {
        super(message, exception);
        fCode = code;
        fTemporary = temporary;
    }

    public ScannerException(String message, ISourceLocation location, boolean temporary, int code)
    {
        super(message);
        this.fLocation = location;
        fCode = code;
        fTemporary = temporary;
    }

    public int getCode()
    {
        return fCode;
    }

    public ISourceLocation getLocation()
    {
        return this.fLocation;
    }

    public boolean isTemporary()
    {
        return fTemporary;
    }

}