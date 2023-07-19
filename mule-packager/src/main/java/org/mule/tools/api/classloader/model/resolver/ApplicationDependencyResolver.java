/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.tools.api.classloader.model.resolver;

import static java.util.Optional.empty;
import org.mule.maven.client.api.MavenReactorResolver;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleScope;
import org.mule.maven.client.internal.AetherMavenClient;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationDependencyResolver {

  protected static final String MULE_DOMAIN_CLASSIFIER = "mule-domain";

  private final AetherMavenClient muleMavenPluginClient;

  public ApplicationDependencyResolver(AetherMavenClient muleMavenPluginClient) {
    this.muleMavenPluginClient = muleMavenPluginClient;
  }

  /**
   * Resolve the application dependencies, excluding mule domains.
   *
   * @param pomFile pom file
   */
  @Deprecated
  public List<BundleDependency> resolveApplicationDependencies(File pomFile) {
    return resolveApplicationDependencies(pomFile, false, empty());
  }

  /**
   * Resolve the application dependencies, excluding mule domains.
   *
   * @param pomFile pom file
   * @param includeTestDependencies true if the test dependencies must be included, false otherwise.
   */
  @Deprecated
  public List<BundleDependency> resolveApplicationDependencies(File pomFile, boolean includeTestDependencies) {
    return resolveApplicationDependencies(pomFile, includeTestDependencies, empty());
  }

  /**
   * Resolve the application dependencies, excluding mule domains.
   *
   * @param pomFile pom file
   * @param includeTestDependencies true if the test dependencies must be included, false otherwise.
   * @param mavenReactorResolver {@link MavenReactorResolver}
   */
  public List<BundleDependency> resolveApplicationDependencies(File pomFile, boolean includeTestDependencies,
                                                               Optional<MavenReactorResolver> mavenReactorResolver) {
    List<BundleDependency> resolvedApplicationDependencies =
        muleMavenPluginClient
            .resolveArtifactDependencies(pomFile, includeTestDependencies, true, empty(), mavenReactorResolver, empty())
            .stream()
            .filter(d -> !(d.getScope() == BundleScope.PROVIDED) || (d.getDescriptor().getClassifier().isPresent()
                && d.getDescriptor().getClassifier().get().equals(MULE_DOMAIN_CLASSIFIER)))
            .collect(Collectors.toList());

    return resolvedApplicationDependencies;
  }

}
