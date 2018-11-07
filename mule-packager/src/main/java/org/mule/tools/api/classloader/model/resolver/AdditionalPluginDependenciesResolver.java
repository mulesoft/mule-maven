/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.api.classloader.model.resolver;

import static java.lang.System.lineSeparator;
import static java.util.Optional.of;
import static org.apache.commons.io.FileUtils.toFile;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.tools.api.classloader.model.ArtifactCoordinates.DEFAULT_ARTIFACT_TYPE;
import static org.mule.tools.api.classloader.model.util.ArtifactUtils.toBundleDescriptor;
import static org.mule.tools.api.packager.packaging.Classifier.MULE_PLUGIN;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.util.Reference;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * Resolves additional plugin libraries for all plugins declared.
 *
 * @since 3.2.0
 */
public class AdditionalPluginDependenciesResolver {

  protected static final String MULE_EXTENSIONS_PLUGIN_GROUP_ID = "org.mule.runtime.plugins";
  protected static final String MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID = "mule-extensions-maven-plugin";
  protected static final String MULE_MAVEN_PLUGIN_GROUP_ID = "org.mule.tools.maven";
  protected static final String MULE_MAVEN_PLUGIN_ARTIFACT_ID = "mule-maven-plugin";
  protected static final String ADDITIONAL_PLUGIN_DEPENDENCIES_ELEMENT = "additionalPluginDependencies";
  protected static final String ADDITIONAL_DEPENDENCIES_ELEMENT = "additionalDependencies";
  protected static final String GROUP_ID_ELEMENT = "groupId";
  protected static final String ARTIFACT_ID_ELEMENT = "artifactId";
  protected static final String VERSION_ELEMENT = "version";
  protected static final String PLUGIN_ELEMENT = "plugin";
  protected static final String DEPENDENCY_ELEMENT = "dependency";
  private AetherMavenClient aetherMavenClient;
  private List<Plugin> pluginsWithAdditionalDependencies;
  private File temporaryFolder;

  public AdditionalPluginDependenciesResolver(AetherMavenClient muleMavenPluginClient,
                                              List<Plugin> additionalPluginDependencies,
                                              File temporaryFolder) {
    this.aetherMavenClient = muleMavenPluginClient;
    this.pluginsWithAdditionalDependencies = new ArrayList<>(additionalPluginDependencies);
    this.temporaryFolder = temporaryFolder;
  }

  public Map<BundleDependency, List<BundleDependency>> resolveDependencies(List<BundleDependency> applicationDependencies,
                                                                           Collection<ClassLoaderModel> mulePluginsClassLoaderModels) {
    addPluginDependenciesAdditionalLibraries(applicationDependencies);
    Map<BundleDependency, List<BundleDependency>> pluginsWithAdditionalDeps = new LinkedHashMap<>();
    for (Plugin pluginWithAdditionalDependencies : pluginsWithAdditionalDependencies) {
      BundleDependency pluginBundleDependency =
          getPluginBundleDependency(pluginWithAdditionalDependencies, applicationDependencies);
      ClassLoaderModel pluginClassLoaderModel =
          getPluginClassLoaderModel(pluginWithAdditionalDependencies, mulePluginsClassLoaderModels);
      List<BundleDependency> additionalDependencies = new ArrayList<>();
      pluginWithAdditionalDependencies.getAdditionalDependencies().stream()
          .filter(additionalDep -> !isPresentInClassLoaderModel(pluginClassLoaderModel, additionalDep))
          .forEach(dep -> resolveDependency(dep)
              .forEach(resolvedDependency -> updateAdditionalDependencyOrFail(additionalDependencies, resolvedDependency)));
      if (!additionalDependencies.isEmpty()) {
        pluginsWithAdditionalDeps.put(pluginBundleDependency,
                                      additionalDependencies);
      }
    }
    return pluginsWithAdditionalDeps;
  }

  private void updateAdditionalDependencyOrFail(List<BundleDependency> additionalDependencies,
                                                BundleDependency bundleDependency) {
    Reference<BundleDependency> replace = new Reference<>();
    additionalDependencies.stream()
        .filter(additionalBundleDependency -> StringUtils.equals(additionalBundleDependency.getDescriptor().getGroupId(),
                                                                 bundleDependency.getDescriptor().getGroupId())
            &&
            StringUtils.equals(additionalBundleDependency.getDescriptor().getArtifactId(),
                               bundleDependency.getDescriptor().getArtifactId()))
        .findFirst()
        .map(additionalBundleDependency -> {
          String additionalBundleDependencyVersion = additionalBundleDependency.getDescriptor().getVersion();
          String bundleDependencyVersion = bundleDependency.getDescriptor().getVersion();
          if (areSameMajor(bundleDependencyVersion, additionalBundleDependencyVersion)) {
            if (isNewerVersion(bundleDependencyVersion, additionalBundleDependencyVersion)) {
              replace.set(additionalBundleDependency);
            }
          } else {
            throw new MuleRuntimeException(createStaticMessage("Attempting to add different major versions of the same dependency as additional plugin dependency. If this is not explicitly defined, check transitive dependencies."
                +
                lineSeparator() +
                "These are: " +
                lineSeparator() +
                additionalBundleDependency.toString() +
                lineSeparator() +
                bundleDependency.toString()));
          }
          return true;
        }).orElseGet(
                     () -> additionalDependencies.add(bundleDependency));
    if (replace.get() != null) {
      additionalDependencies.remove(replace.get());
      additionalDependencies.add(bundleDependency);
    }
  }

  private List<BundleDependency> resolveDependency(Dependency dependency) {
    BundleDescriptor bundleDescriptor = toBundleDescriptor(dependency);
    List<BundleDependency> resolvedDependencies = new ArrayList<>();
    resolvedDependencies.add(aetherMavenClient.resolveBundleDescriptor(bundleDescriptor));
    resolvedDependencies.addAll(aetherMavenClient.resolveBundleDescriptorDependencies(false, false, bundleDescriptor));
    return resolvedDependencies;
  }

  private BundleDependency getPluginBundleDependency(Plugin plugin, List<BundleDependency> mulePlugins) {
    return mulePlugins.stream()
        .filter(mulePlugin -> StringUtils.equals(mulePlugin.getDescriptor().getArtifactId(), plugin.getArtifactId())
            && StringUtils.equals(mulePlugin.getDescriptor().getGroupId(), plugin.getGroupId()))
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Declared additional dependencies for a plugin not present: "
            + plugin)));
  }

  private ClassLoaderModel getPluginClassLoaderModel(Plugin plugin, Collection<ClassLoaderModel> mulePluginsClassLoaderModels) {
    return mulePluginsClassLoaderModels.stream().filter(
                                                        pluginClassLoaderModel -> StringUtils
                                                            .equals(pluginClassLoaderModel.getArtifactCoordinates().getGroupId(),
                                                                    plugin.getGroupId())
                                                            && StringUtils.equals(pluginClassLoaderModel.getArtifactCoordinates()
                                                                .getArtifactId(), plugin.getArtifactId()))
        .findFirst()
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find ClassLoaderModel resolved for plugin: "
            + plugin)));
  }

  private boolean areSameArtifact(Dependency dependency, Artifact artifact) {
    return StringUtils.equals(dependency.getArtifactId(), artifact.getArtifactCoordinates().getArtifactId())
        && StringUtils.equals(dependency.getGroupId(), artifact.getArtifactCoordinates().getGroupId())
        && StringUtils.equals(dependency.getVersion(), artifact.getArtifactCoordinates().getVersion());
  }

  private boolean isPresentInClassLoaderModel(ClassLoaderModel classLoaderModel, Dependency dep) {
    return classLoaderModel.getDependencies().stream().anyMatch(artifactDependency -> areSameArtifact(dep, artifactDependency));
  }

  private String getChildParameterValue(Xpp3Dom element, String childName, boolean validate) {
    Xpp3Dom child = element.getChild(childName);
    String childValue = child != null ? child.getValue() : null;
    if (StringUtils.isEmpty(childValue) && validate) {
      throw new IllegalArgumentException("Expecting child element with not null value " + childName);
    }
    return childValue;
  }

  private void addPluginDependenciesAdditionalLibraries(List<BundleDependency> applicationDependencies) {
    List<BundleDependency> mulePlugins = applicationDependencies
        .stream()
        .filter(bundleDependency -> MULE_PLUGIN
            .equals(bundleDependency.getDescriptor().getClassifier().orElse(null)))
        .collect(Collectors.toList());

    Collection<Plugin> additionalDependenciesFromMulePlugins = resolveAdditionalDependenciesFromMulePlugins(mulePlugins);

    pluginsWithAdditionalDependencies.addAll(additionalDependenciesFromMulePlugins.stream()
        .filter(isNotRedefinedAtApplicationLevel())
        .collect(Collectors.toList()));
  }

  protected Collection<Plugin> resolveAdditionalDependenciesFromMulePlugins(List<BundleDependency> mulePlugins) {
    Map<String, Plugin> additionalDependenciesFromMulePlugins = new HashMap<>();

    mulePlugins.forEach(mulePlugin -> {
      try {
        Model pomModel =
            aetherMavenClient.getEffectiveModel(toFile(mulePlugin.getBundleUri().toURL()), of(temporaryFolder));

        Build build = pomModel.getBuild();
        if (build != null) {
          org.apache.maven.model.Plugin packagerPlugin =
              build.getPluginsAsMap().get(MULE_EXTENSIONS_PLUGIN_GROUP_ID + ":" + MULE_EXTENSIONS_PLUGIN_ARTIFACT_ID);
          if (packagerPlugin == null) {
            packagerPlugin =
                build.getPluginsAsMap().get(MULE_MAVEN_PLUGIN_GROUP_ID + ":" + MULE_MAVEN_PLUGIN_ARTIFACT_ID);
          }
          if (packagerPlugin != null) {
            Object configurationObject =
                packagerPlugin.getConfiguration();
            if (configurationObject != null) {
              Xpp3Dom additionalPluginDependenciesDom = ((Xpp3Dom) configurationObject)
                  .getChild(ADDITIONAL_PLUGIN_DEPENDENCIES_ELEMENT);
              if (additionalPluginDependenciesDom != null) {
                Xpp3Dom[] additionalPluginDependencies =
                    additionalPluginDependenciesDom.getChildren(PLUGIN_ELEMENT);
                if (additionalPluginDependencies != null) {
                  Arrays.stream(additionalPluginDependencies)
                      .forEach(additonalPluginDependencyDom -> {
                        String pluginGroupId = getChildParameterValue(additonalPluginDependencyDom, GROUP_ID_ELEMENT, true);
                        String pluginArtifactId =
                            getChildParameterValue(additonalPluginDependencyDom, ARTIFACT_ID_ELEMENT, true);
                        Plugin alreadyDefinedPluginAdditionalDependencies =
                            additionalDependenciesFromMulePlugins.get(pluginGroupId + ":" + pluginArtifactId);
                        List<Dependency> additionalDependencyDependencies = Arrays
                            .stream(additonalPluginDependencyDom.getChild(ADDITIONAL_DEPENDENCIES_ELEMENT)
                                .getChildren(DEPENDENCY_ELEMENT))
                            .map(dependencyDom -> {
                              Dependency dependency = new Dependency();
                              dependency.setGroupId(getChildParameterValue(dependencyDom, GROUP_ID_ELEMENT, true));
                              dependency
                                  .setArtifactId(getChildParameterValue(dependencyDom, ARTIFACT_ID_ELEMENT, true));
                              dependency.setVersion(getChildParameterValue(dependencyDom, VERSION_ELEMENT, true));
                              String type = getChildParameterValue(dependencyDom, "type", false);
                              dependency.setType(type == null ? DEFAULT_ARTIFACT_TYPE : type);
                              dependency.setClassifier(getChildParameterValue(dependencyDom, "classifier", false));
                              dependency.setSystemPath(getChildParameterValue(dependencyDom, "systemPath", false));
                              return dependency;
                            })
                            .collect(Collectors.toList());
                        if (alreadyDefinedPluginAdditionalDependencies != null) {
                          LinkedList<Dependency> effectiveDependencies =
                              new LinkedList<>(alreadyDefinedPluginAdditionalDependencies.getAdditionalDependencies());
                          additionalDependencyDependencies.forEach(additionalDependenciesDependency -> {
                            boolean addDependency = true;
                            for (int i = 0; i < effectiveDependencies.size(); i++) {
                              Dependency effectiveDependency = effectiveDependencies.get(i);
                              if (effectiveDependency.getGroupId().equals(additionalDependenciesDependency.getGroupId()) &&
                                  effectiveDependency.getArtifactId().equals(additionalDependenciesDependency.getArtifactId())
                                  &&
                                  effectiveDependency.getType().equals(additionalDependenciesDependency.getType()) &&
                                  ObjectUtils.compare(effectiveDependency.getClassifier(),
                                                      additionalDependenciesDependency.getClassifier()) == 0) {
                                if (isNewerVersion(additionalDependenciesDependency.getVersion(),
                                                   effectiveDependency.getVersion())) {
                                  effectiveDependencies.remove(i);
                                  break;
                                } else {
                                  addDependency = false;
                                  break;
                                }
                              }
                            }
                            if (addDependency) {
                              effectiveDependencies.add(additionalDependenciesDependency);
                            }
                          });
                          alreadyDefinedPluginAdditionalDependencies.setAdditionalDependencies(effectiveDependencies);
                        } else {
                          Plugin plugin = new Plugin();
                          plugin.setGroupId(pluginGroupId);
                          plugin.setArtifactId(pluginArtifactId);
                          plugin.setAdditionalDependencies(additionalDependencyDependencies);
                          additionalDependenciesFromMulePlugins.put(plugin.getGroupId() + ":" + plugin.getArtifactId(),
                                                                    plugin);
                        }
                      });
                }
              }
            }
          }
        }
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    });
    return additionalDependenciesFromMulePlugins.values();
  }

  private boolean isNewerVersion(String dependencyA, String dependencyB) {
    try {
      return new MuleVersion(dependencyA).newerThan(dependencyB);
    } catch (IllegalArgumentException e) {
      // If not using semver lets just compare the strings.
      return dependencyA.compareTo(dependencyB) > 0;
    }
  }

  private boolean areSameMajor(String dependencyA, String dependencyB) {
    try {
      return new MuleVersion(dependencyA).getMajor() == new MuleVersion(dependencyB).getMajor();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private Predicate<Plugin> isNotRedefinedAtApplicationLevel() {
    return dependencyPluginAdditionalDependencies -> !pluginsWithAdditionalDependencies.stream()
        .filter(applicationPluginAdditionalDependency -> (dependencyPluginAdditionalDependencies.getGroupId()
            .equals(applicationPluginAdditionalDependency.getGroupId())
            && dependencyPluginAdditionalDependencies.getArtifactId()
                .equals(applicationPluginAdditionalDependency.getArtifactId())))
        .findAny().isPresent();
  }

}
