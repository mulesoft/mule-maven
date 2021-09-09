package org.mule.tooling.internal;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.CompositeModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.container.internal.ModuleDiscoverer;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.impl.internal.artifact.ExtensionModelDiscoverer;
import org.mule.tooling.api.ExtensionModelLoader;
import org.mule.tooling.api.LoadedExtensionInformation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DefaultExtensionModelLoader implements ExtensionModelLoader {

  private final MavenClient mavenClient;
  private final ExtensionModelDiscoverer extensionModelDiscoverer;
  private final ClassLoader parentClassloader;
  private final Path workingDir;
  private final String toolingVersion;

  public DefaultExtensionModelLoader(MavenClient mavenClient, Path workingDir, ClassLoader parentClassloader,
                                     String toolingVersion) {
    this.mavenClient = mavenClient;
    this.extensionModelDiscoverer = new ExtensionModelDiscoverer();
    this.parentClassloader = parentClassloader;
    this.workingDir = workingDir;
    this.toolingVersion = toolingVersion;
  }

  @Override
  public Set<ExtensionModel> getRuntimeExtensionModels() {
    return extensionModelDiscoverer.discoverRuntimeExtensionModels();
  }

  @Override
  public Optional<LoadedExtensionInformation> load(BundleDescriptor artifactDescriptor) {
    return this.load(artifactDescriptor, null);
  }

  @Override
  public Optional<LoadedExtensionInformation> load(BundleDescriptor artifactDescriptor, MuleVersion muleVersion) {
    List<ModuleDiscoverer> result = new ArrayList();
    result.add(new JreModuleDiscoverer());
    result.add(new ClasspathModuleDiscoverer(parentClassloader, workingDir.toFile()));
    final ModuleRepository moduleRepository =
        new DefaultModuleRepository(new CompositeModuleDiscoverer(result.toArray(new ModuleDiscoverer[0])));

    ArtifactClassLoader containerClassLoaderFactory =
        (new ContainerClassLoaderFactory(moduleRepository)).createContainerClassLoader(parentClassloader);
    MuleArtifactResourcesRegistry resourcesRegistry =
        new MuleArtifactResourcesRegistry(toolingVersion, Optional.ofNullable(muleVersion), mavenClient,
                                          moduleRepository, containerClassLoaderFactory, workingDir.toFile());
    DefaultExtensionModelService service = new DefaultExtensionModelService(resourcesRegistry);
    return service.loadExtensionData(artifactDescriptor, muleVersion);
  }
}
