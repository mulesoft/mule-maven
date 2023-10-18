/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.api.util;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import org.mule.maven.pom.parser.api.model.BundleDependency;
import org.mule.maven.pom.parser.api.model.BundleDescriptor;
import org.mule.maven.pom.parser.api.model.BundleScope;
import org.mule.tools.api.muleclassloader.model.ArtifactCoordinates;

import java.util.List;
import java.util.stream.Collectors;

public class DependencyProject implements Project {

  private final MavenProject mavenProject;

  public DependencyProject(MavenProject mavenProject) {
    this.mavenProject = mavenProject;
  }

  @Override
  public List<ArtifactCoordinates> getDirectDependencies() {
    return mavenProject.getDependencies().stream().map(ArtifactUtils::toArtifactCoordinates).collect(Collectors.toList());
  }

  @Override
  public List<ArtifactCoordinates> getDependencies() {
    return mavenProject.getArtifacts().stream().map(ArtifactUtils::toArtifactCoordinates).collect(Collectors.toList());
  }

  @Override
  public List<BundleDependency> getBundleDependencies() {
    return mavenProject.getArtifacts().stream().map(this::toBundleDependency).collect(Collectors.toList());
  }

  private BundleDependency toBundleDependency(Artifact artifact) {
    BundleDescriptor.Builder descriptorBuilder = new BundleDescriptor.Builder();
    BundleDescriptor descriptor = descriptorBuilder.setArtifactId(artifact.getArtifactId())
        .setGroupId(artifact.getGroupId())
        .setVersion(artifact.getVersion())
        .setBaseVersion(artifact.getBaseVersion())
        .setClassifier(artifact.getClassifier())
        .setType(artifact.getType()).build();
    BundleDependency.Builder dependencyBuilder = new BundleDependency.Builder();
    return dependencyBuilder.setBundleUri(artifact.getFile().toURI())
        .setDescriptor(descriptor)
        .setScope(BundleScope.valueOf(artifact.getScope().toUpperCase()))
        .build();
  }
}
