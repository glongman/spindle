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
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * 
 *  glongman@intelligentworks.com
 *
 * ***** END LICENSE BLOCK ***** */

package com.iw.plugins.spindle.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

/**
 * @author gwl
 * @version $Id$
 *
 * Copyright 2002, Intelligent Works Inc.
 * All Rights Reserved.
 */
public class SpindleMultiStatus extends SpindleStatus {

  private List subStatii = new ArrayList();

  /**
   * Constructor for SpindleMultiStatus.
   */
  public SpindleMultiStatus() {
    super(-1, null);
  }

  /**
   * Constructor for SpindleMultiStatus.
   * @param severity
   * @param message
   */
  public SpindleMultiStatus(int severity, String message) {
    super(severity, message);
  }

  /**
   * @see org.eclipse.core.runtime.IStatus#getChildren()
   */
  public IStatus[] getChildren() {

    return (IStatus[]) subStatii.toArray(new IStatus[subStatii.size()]);

  }

  public void addStatus(IStatus status) {

    if (!subStatii.contains(status)) {
      subStatii.add(status);
    }

  }

  /**
   * @see org.eclipse.core.runtime.IStatus#isMultiStatus()
   */
  public boolean isMultiStatus() {
    return true;
  }

  /**
   * @see org.eclipse.core.runtime.IStatus#getSeverity()
   */
  public int getSeverity() {
    int builtin = getSeverity();
    if (getSeverity() >= 0) {
      return builtin;
    }
    if (!subStatii.isEmpty()) {
      IStatus max = null;
      int size = subStatii.size();
      if (size == 1) {
        max = (IStatus) subStatii.get(0);
      } else {
        max = SpindleStatus.getMostSevere((IStatus[]) subStatii.toArray(new IStatus[size]));
      }
      return max.getSeverity();
    } else {
      return OK;
    }
  }

}
