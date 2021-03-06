package net.sf.spindle.core.parser.template;

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
import net.sf.spindle.core.source.SourceLocation;

public class TagEventHandler
{

    private TagEventInfo fInfo;

    private SourceLocation fCurrentAttribute;

    public TagEventHandler()
    {
    }

    public void tagBegin(int lineNumber, int startOffset)
    {
        fInfo = new TagEventInfo();
        fInfo.fStartTagLocation = new SourceLocation(lineNumber, startOffset);
    }

    public void tagEnd(int endOffset)
    {
        fInfo.fStartTagLocation.setCharEnd(endOffset);
    }

    public void attributeBegin(String rawname, int lineNumber, int startOffset)
    {
        fCurrentAttribute = new SourceLocation(lineNumber, startOffset);
        fInfo.getAttributeMap().put(rawname, fCurrentAttribute);

    }

    public void attributeEnd(int endOffset)
    {
        if (fCurrentAttribute != null)
        {
            fCurrentAttribute.setCharEnd(endOffset);
            fCurrentAttribute = null;
        }
        else
        {
            throw new RuntimeException("end recieved with no start!");
        }
    }

    public TagEventInfo getEventInfo()
    {
        return fInfo;
    }

}