package com.iw.plugins.spindle.core.parser;
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



public class SourceLocation implements ISourceLocation {

  private int lineNumber;
  private int charStart;
  private int charEnd;

  SourceLocation(int lineNumber, int charStart, int charEnd) {
    this.lineNumber = lineNumber;
    this.charStart = charStart;
    this.charEnd = charEnd;
  }
  /**
   * @see com.iw.plugins.spindle.core.parser.ISourceLocation#getStartLine()
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * @see com.iw.plugins.spindle.core.parser.ISourceLocation#getCharStart()
   */
  public int getCharStart() {
    return charStart;
  }

  /**
   * @see com.iw.plugins.spindle.core.parser.ISourceLocation#getCharEnd()
   */
  public int getCharEnd() {
    return charEnd;
  }
  
  public String toString() {
  	StringBuffer buffer = new StringBuffer("line:charStart:charEnd[");
  	buffer.append(lineNumber);buffer.append(", ");
  	buffer.append(charStart);buffer.append(", ");
  	buffer.append(charEnd);
  	buffer.append("]");
  	return buffer.toString();
  }

}
