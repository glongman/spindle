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
 * The Original Code is TypedPostionWalker.
 *
 * The Initial Developer of the Original Code is
 * Christian Sell <christian.sell@netcologne.de>.
 * Portions created by the Initial Developer are Copyright (C) 2001-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *  christian.sell@netcologne.de
 *  glongman@gmail.com
 * 
 * ***** END LICENSE BLOCK ***** */

package com.iw.plugins.spindle.editors.formatter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TypedPosition;
import org.xmen.xml.XMLNode;

/**
 * Helper class which has the ability to walk over the artifacts in the
 * underlying document in both directions (forward and backwards).
 */
class TypedPositionWalker
{
  private boolean modeForward;
  private TypedPosition[] fTypedPositions;
  private int fLastIndex, fNextIndex, fEndIndex;

  /**
   * create an instance to walk forward for a given length starting at the given
   * offset
   */
  TypedPositionWalker(TypedPosition[] typedPositions, int offset, int length)
      throws BadLocationException
  {
    modeForward = true;
    fTypedPositions = typedPositions;
    fNextIndex = fLastIndex = getArtifactIndexAt(offset);
    fEndIndex = getArtifactIndexAt(offset + length);
  }

  /**
   * create an instance to walk only backward until the document start, starting
   * at the given offset
   */
  TypedPositionWalker(TypedPosition[] documentArtifacts, int offset)
      throws BadLocationException
  {
    modeForward = false;
    fTypedPositions = documentArtifacts;
    fLastIndex = fNextIndex = getArtifactIndexAt(offset);
  }

  public TypedPosition next() throws BadLocationException
  {
    if (!modeForward)
      throw new UnsupportedOperationException(
          "calling next on backward walker not allowed");

    if (fNextIndex > fEndIndex)
      return null;

    XMLNode artifact = (XMLNode) fTypedPositions[fNextIndex];
    fLastIndex = fNextIndex;
    fNextIndex += 1;
    return artifact;
  }

  public boolean hasNext()
  {
    if (!modeForward)
      throw new UnsupportedOperationException(
          "calling hasNext on backward walker not allowed");
    return fNextIndex < fTypedPositions.length;
  }

  public TypedPosition previous() throws BadLocationException
  {
    if (modeForward)
      throw new UnsupportedOperationException(
          "calling previous on forward walker not allowed");

    if (fLastIndex == 0)
      return null;

    XMLNode artifact = (XMLNode) fTypedPositions[fNextIndex];
    fLastIndex = fNextIndex;
    fNextIndex -= 1;
    return artifact;
  }

  public boolean hasPrevious()
  {
    if (modeForward)
      throw new UnsupportedOperationException(
          "calling hasPrevious on forward walker not allowed");
    return fNextIndex >= 0;
  }

  /**
   * get the index of postion found at the offset
   * 
   * @param offset
   * @return the position found at the offset
   * @throws BadLocationException
   */
  private int getArtifactIndexAt(int offset) throws BadLocationException
  {
    for (int i = 0; i < fTypedPositions.length; i++)
    {
      if (offset >= fTypedPositions[i].getOffset()
          && offset <= fTypedPositions[i].getOffset() + fTypedPositions[i].getLength())
        return i;
    }
    throw new BadLocationException();
  }
}