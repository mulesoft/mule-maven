/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.api.validation.deployment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.mule.tools.api.packager.packaging.Classifier.MULE_APPLICATION;
import static org.mule.tools.api.packager.packaging.Classifier.MULE_APPLICATION_EXAMPLE;
import static org.mule.tools.api.packager.packaging.Classifier.MULE_APPLICATION_TEMPLATE;
import static org.mule.tools.api.packager.packaging.Classifier.MULE_DOMAIN_BUNDLE;
import static org.mule.tools.api.packager.packaging.Classifier.MULE_POLICY;
import static org.mule.tools.api.packager.packaging.PackagingType.MULE_DOMAIN;
import static org.mule.tools.api.validation.project.AbstractProjectValidator.isClassifierValid;
import static org.mule.tools.api.validation.project.AbstractProjectValidator.isPackagingTypeValid;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.mule.tools.api.exception.ValidationException;
import org.mule.tools.api.packager.ProjectInformation;
import org.mule.tools.api.packager.packaging.Classifier;
import org.mule.tools.model.Deployment;

/**
 * @author Mulesoft Inc.
 * @since 2.0.0
 */
public class ProjectDeploymentValidator {

  private static final List<String> NON_DEPLOYABLE_ARTIFACTS = newArrayList(MULE_POLICY.toString(),
                                                                            MULE_DOMAIN_BUNDLE.toString(),
                                                                            MULE_APPLICATION_TEMPLATE.toString());
  // TODO maybe we need only specific things of the project information
  // TODO we should validate exchange here
  // TODO we should validate that if artifact is present in configuration then the file should exits

  private ProjectInformation projectInformation;

  public ProjectDeploymentValidator(ProjectInformation projectInformation) {
    checkArgument(projectInformation != null, "The project information must not be null");

    this.projectInformation = projectInformation;
  }

  public ProjectInformation getProjectInformation() {
    return projectInformation;
  }

  /**
   * Validates if it can be deployed to an environment
   * 
   * @throws ValidationException if it can not be deployed
   */
  public void isDeployable() throws ValidationException {
    if (projectInformation.isDeployment()) {
      validateIsDeployable();
    }
  }

  private void validateIsDeployable() throws ValidationException {
    String artifactType = getArtifactType();
    for (String nonDeployableType : NON_DEPLOYABLE_ARTIFACTS) {
      if (artifactType.contains(nonDeployableType)) {
        throw new ValidationException("Deployment of " + nonDeployableType + " projects is not supported");
      }
    }
    isClassifierValid(artifactType);
  }

  private String getArtifactType() {
    return projectInformation.getClassifier();
  }

}
