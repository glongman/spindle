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
package com.iw.plugins.spindle.model.manager;

import java.util.List;

import org.eclipse.core.resources.IStorage;
import org.eclipse.pde.core.IModel;

import com.iw.plugins.spindle.model.BaseTapestryModel;
import com.iw.plugins.spindle.model.ITapestryModel;

/**
 * @author gwl
 * @version $Id$
 *
 */
public interface ITapestryModelManagerDelegate  {


  public ITapestryModel getFirstLoadedModel();

  /**
   * Method addModel.
   * @param model
   */
  public void addModel(ITapestryModel model);

  /**
   * Method removeModel.
   * @param nuked
   */
  public void removeModel(IModel nuked);
  
  /**
   * Method createModel.
   * @param storage
   * @return BaseTapestryModel
   */
  public BaseTapestryModel createModel(IStorage storage);

  /**
   * Method getAllModels.
   * @return List
   */
  public List getAllModels();
  
  
  public void registerParserFor(String extension);
  
  public void clear();
  	



}
