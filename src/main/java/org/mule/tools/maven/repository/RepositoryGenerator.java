/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.maven.repository;

import static java.lang.String.format;
import static org.mule.tools.api.packager.PackagerFolders.REPOSITORY;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.tools.api.classloader.model.ClassLoaderModel;
import org.mule.tools.maven.util.FileUtils;

public class RepositoryGenerator {

  private final RepositorySystemSession aetherRepositorySystemSession;
  private final org.eclipse.aether.RepositorySystem aetherRepositorySystem;
  private Log log;

  private MavenProject project;
  private List<ArtifactRepository> remoteArtifactRepositories;

  protected File outputDirectory;

  public RepositoryGenerator(MavenProject project, List<ArtifactRepository> remoteArtifactRepositories, File outputDirectory,
                             Log log, org.eclipse.aether.RepositorySystem aetherRepositorySystem,
                             RepositorySystemSession aetherRepositorySystemSession) {
    this.log = log;
    this.project = project;
    this.remoteArtifactRepositories = remoteArtifactRepositories;
    this.outputDirectory = outputDirectory;
    this.aetherRepositorySystem = aetherRepositorySystem;
    this.aetherRepositorySystemSession = aetherRepositorySystemSession;
  }


  public ClassLoaderModel generate() throws MojoExecutionException, MojoFailureException {
    log.info(format("Mirroring repository for [%s]", project.toString()));
    ClassLoaderModelAssembler classLoaderModelAssembler = buildClassLoaderModelAssembler();
    File pomFile = project.getFile();
    ClassLoaderModel model = classLoaderModelAssembler.getClassLoaderModel(pomFile, outputDirectory);
    Set<Artifact> artifacts = model.getArtifacts();
    File repositoryFolder = getRepositoryFolder();
    ArtifactInstaller artifactInstaller = buildArtifactInstaller(remoteArtifactRepositories);
    installArtifacts(repositoryFolder, artifacts, artifactInstaller);
    return model;
  }

  protected ArtifactInstaller buildArtifactInstaller(List<ArtifactRepository> remoteArtifactRepositories) {
    return new ArtifactInstaller(log, remoteArtifactRepositories, aetherRepositorySystem,
                                 aetherRepositorySystemSession);
  }

  protected ClassLoaderModelAssembler buildClassLoaderModelAssembler() {
    List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(remoteArtifactRepositories);
    return new ClassLoaderModelAssembler((AetherMavenClient) new MuleMavenPluginClientProvider(remoteRepositories, log)
        .buildMavenClient());
  }


  protected File getRepositoryFolder() {
    File repositoryFolder = new File(outputDirectory, REPOSITORY);
    if (!repositoryFolder.exists()) {
      repositoryFolder.mkdirs();
    }
    return repositoryFolder;
  }

  protected void installArtifacts(File repositoryFile, Set<Artifact> artifacts, ArtifactInstaller installer)
      throws MojoExecutionException {
    TreeSet<Artifact> sortedArtifacts = new TreeSet<>(artifacts);
    if (sortedArtifacts.isEmpty()) {
      generateMarkerFileInRepositoryFolder(repositoryFile);
    }
    for (Artifact artifact : sortedArtifacts) {
      installer.installArtifact(repositoryFile, artifact);
    }
  }

  protected void generateMarkerFileInRepositoryFolder(File repositoryFile) throws MojoExecutionException {
    File markerFile = new File(repositoryFile, ".marker");
    log.info(format("No artifacts to add, adding marker file <%s/%s>", REPOSITORY, markerFile.getName()));
    try {
      FileUtils.checkReadOnly(repositoryFile);
      markerFile.createNewFile();
    } catch (IOException e) {
      throw new MojoExecutionException(format("The current repository has no artifacts to install, and trying to create [%s] failed",
                                              markerFile.toString()),
                                       e);
    }
  }
}
