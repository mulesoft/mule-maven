/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.util.ArrayList;



public class DeploymentConfigurator {

  private final DeployerLog log;
  private DeploymentConfiguration configuration;

  public DeploymentConfigurator(DeploymentConfiguration configuration, DeployerLog log) {
    this.configuration = configuration;
    this.log = log;
  }

  public void initializeApplication(ArtifactFactory factory, MavenProject project, ArtifactResolver resolver,
                                    ArtifactRepository localRepository)
      throws MojoFailureException {
    if (configuration.getApplication() == null) {
      Artifact artifact = resolveMavenProjectArtifact(factory, project, resolver, localRepository);
      configuration.setApplication(artifact.getFile());
      log.info("No application configured. Using project artifact: " + configuration.getApplication());

      if (configuration.getApplicationName() == null) {
        configuration.setApplicationName(artifact.getArtifactId());
      }
    } else {
      // If an application is defined but no application name is provided, use the name of the file instead of
      // the artifact ID (expected behavior in standalone deploymentConfiguration for example).
      if (StringUtils.isBlank(configuration.getApplicationName())) {
        configuration.setApplicationName(configuration.getApplication().getName());
      }
    }
  }

  protected Artifact resolveMavenProjectArtifact(ArtifactFactory factory, MavenProject project, ArtifactResolver resolver,
                                                 ArtifactRepository localRepository)
      throws MojoFailureException {
    Artifact artifact = factory.createArtifactWithClassifier(project.getGroupId(),
                                                             project.getArtifactId(),
                                                             project.getVersion(),
                                                             "jar",
                                                             project.getPackaging());
    try {
      resolver.resolve(artifact, new ArrayList<>(), localRepository);
    } catch (ArtifactResolutionException | ArtifactNotFoundException e) {
      throw new MojoFailureException("Couldn't resolve artifact [" + artifact + "]");
    }

    return artifact;
  }


  public void initializeEnvironment(Settings settings, SettingsDecrypter decrypter) throws MojoExecutionException {
    if (configuration.getServer() != null) {
      Server serverObject = settings.getServer(configuration.getServer());
      if (serverObject == null) {
        log.error("Server [" + configuration.getServer() + "] not found in settings file.");
        throw new MojoExecutionException("Server [" + configuration.getServer() + "] not found in settings file.");
      }
      // Decrypting Maven server, in case of plain text passwords returns the same
      serverObject = decrypter.decrypt(new DefaultSettingsDecryptionRequest(serverObject)).getServer();
      if (StringUtils.isNotEmpty(configuration.getUsername())
          || StringUtils.isNotEmpty(configuration.getPassword())) {
        log.warn("Both server and credentials are configured. Using plugin configuration credentials.");
      } else {
        configuration.setUsername(serverObject.getUsername());
        configuration.setPassword(serverObject.getPassword());
      }
    }

    String ibmJdkSupport = System.getProperty("ibm.jdk.support");
    if ("true".equals(ibmJdkSupport)) {
      log.debug("Attempting to provide support for IBM JDK...");
      try {
        Field methods = HttpURLConnection.class.getDeclaredField("methods");
        methods.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);

        modifiers.setInt(methods, methods.getModifiers() & ~Modifier.FINAL);

        String[] actualMethods = {"GET", "POST", "HEAD", "OPTIONS", "PUT", "PATCH", "DELETE", "TRACE"};
        methods.set(null, actualMethods);

      } catch (NoSuchFieldException | IllegalAccessException e) {
        log.error("Fail to provide support for IBM JDK", e);
      }
    }
  }
}
