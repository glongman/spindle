/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.iw.plugins.spindle.editors;

import java.util.Iterator;

/**
 * Interface of annotations representing markers and problems.
 *  
 */
public interface IProblemAnnotation
{

  String getType();
  /**
   * @see org.eclipse.jface.text.source.Annotation#isMarkedDeleted()
   */
  boolean isMarkedDeleted();

  boolean isTemporary();

  String getMessage();

  String[] getArguments();

  int getId();

  //  Image getImage(Display display);

  /**
   * Returns whether this annotation is relavant.
   * <p>
   * If the annotation is overlaid then it is not relevant. After all overlays
   * have been removed the annotation might either become relevant again or stay
   * irrelevant.
   * </p>
   * 
   * @return <code>true</code> if relevant
   * @see #hasOverlay()
   */
  boolean isRelevant();

  /**
   * Returns whether this annotation is overlaid.
   * 
   * @return <code>true</code> if overlaid
   */
  boolean hasOverlay();

  IProblemAnnotation getOverlay();

  /**
   * Returns an iterator for iterating over the annotation which are overlaid by
   * this annotation.
   * 
   * @return an iterator over the overlaid annotaions
   */
  Iterator getOverlaidIterator();

  /**
   * Adds the given annotation to the list of annotations which are overlaid by
   * this annotations.
   * 
   * @param annotation the problem annoation
   */
  void addOverlaid(IProblemAnnotation annotation);

  /**
   * Removes the given annotation from the list of annotations which are
   * overlaid by this annotation.
   * 
   * @param annotation the problem annoation
   */
  void removeOverlaid(IProblemAnnotation annotation);

  /**
   * Tells whether this annotation is a problem annotation.
   * 
   * @return <code>true</code> if it is a problem annotation
   */
  boolean isProblem();
}