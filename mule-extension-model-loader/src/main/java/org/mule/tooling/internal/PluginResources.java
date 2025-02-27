/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tooling.internal;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.mule.runtime.api.meta.model.ExtensionModel;

public class PluginResources {

  private Set<ExtensionModel> extensionModels;
  private List<URL> exportedResources;
  private Set<String> dwlFiles;

  public PluginResources(Set<ExtensionModel> extensionModels,  List<URL> exportedResources) {
    super();
    this.extensionModels = extensionModels;
    this.exportedResources = exportedResources;
  }

  public List<URL> getExportedResources() {
    return exportedResources;
  }


  public Set<ExtensionModel> getExtensionModels() {
    return extensionModels;
  }

  public  Set<String> getDwlFiles(){
    return dwlFiles;
  }

  public  PluginResources setDwlFiles(Set<String> dwlFiles){
     this.dwlFiles=dwlFiles;
     return this;
  }
}
