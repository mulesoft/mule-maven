/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.api.classloader;

import static org.mule.tools.api.classloader.Constants.CLASSLOADER_MODEL_FILE_NAME;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.io.*;

public class ClassLoaderModelJsonSerializer {

  /**
   * Creates a {@link ClassLoaderModel} from the JSON representation
   *
   * @param classLoaderModelDescriptor file containing the classloader model in JSON format
   * @return a non null {@link ClassLoaderModel} matching the provided JSON content
   */
  public static ClassLoaderModel deserialize(File classLoaderModelDescriptor) {
    try {
      Gson gson = new GsonBuilder()
          .enableComplexMapKeySerialization()
          .setPrettyPrinting()
          .create();

      Reader reader = new FileReader(classLoaderModelDescriptor);
      ClassLoaderModel classLoaderModel = gson.fromJson(reader, ClassLoaderModel.class);
      reader.close();

      return classLoaderModel;
    } catch (IOException e) {
      throw new RuntimeException("Could not create classloader-model.json", e);
    }
  }

  /**
   * Serializes the classloader model to a string
   *
   * @param classLoaderModel the classloader model of the application being packaged
   * @return string containing the classloader model's JSON representation
   */
  public static String serialize(ClassLoaderModel classLoaderModel) {
    Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
        .registerTypeAdapter(Artifact.class, new ArtifactCustomJsonSerializer())
        .registerTypeAdapter(AppClassLoaderModel.class,
                             new AppClassLoaderModelJsonSerializer.AppClassLoaderModelCustomJsonSerializer())
        .create();
    ClassLoaderModel parameterizedClassloaderModel = classLoaderModel.getParametrizedUriModel();
    return gson.toJson(parameterizedClassloaderModel);
  }

  /**
   * Serializes the classloader model to the classloader-model.json file in the destination folder
   *
   * @param classLoaderModel the classloader model of the application being packaged
   * @param destinationFolder the directory model where the file is going to be written
   * @return the created File containing the classloader model's JSON representation
   */
  public static File serializeToFile(ClassLoaderModel classLoaderModel, File destinationFolder) {
    File destinationFile = new File(destinationFolder, CLASSLOADER_MODEL_FILE_NAME);
    try {
      destinationFile.createNewFile();
      Writer writer = new FileWriter(destinationFile.getAbsolutePath());
      writer.write(serialize(classLoaderModel));
      writer.close();
      return destinationFile;
    } catch (IOException e) {
      throw new RuntimeException("Could not create classloader-model.json", e);
    }
  }
}
