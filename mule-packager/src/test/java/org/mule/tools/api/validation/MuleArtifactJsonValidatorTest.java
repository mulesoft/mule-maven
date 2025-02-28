/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.api.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tools.api.packager.structure.PackagerFiles.MULE_ARTIFACT_JSON;
import static org.mule.tools.api.validation.MuleArtifactJsonValidator.checkClassLoaderModelDescriptor;
import static org.mule.tools.api.validation.MuleArtifactJsonValidator.checkMinMuleVersionValue;
import static org.mule.tools.api.validation.MuleArtifactJsonValidator.checkName;
import static org.mule.tools.api.validation.MuleArtifactJsonValidator.isMuleArtifactJsonPresent;
import static org.mule.tools.api.validation.MuleArtifactJsonValidator.isMuleArtifactJsonValid;
import static org.mule.tools.api.validation.MuleArtifactJsonValidator.validateMuleArtifactMandatoryFields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.persistence.MuleApplicationModelJsonSerializer;
import org.mule.tools.api.exception.ValidationException;
import org.mule.tools.model.Deployment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

public class MuleArtifactJsonValidatorTest {

  @TempDir
  public Path projectBaseFolder;

  private File muleArtifactJsonFile;
  private List<String> missingFields;
  private Deployment deploymentConfigurationMock;

  @BeforeEach
  public void setUp() throws IOException {
    projectBaseFolder.toFile();
    Files.createFile(projectBaseFolder.resolve(MULE_ARTIFACT_JSON));
    muleArtifactJsonFile = projectBaseFolder.resolve(MULE_ARTIFACT_JSON).toFile();
    projectBaseFolder.resolve(muleArtifactJsonFile.toString());
    missingFields = new ArrayList<>();
    deploymentConfigurationMock = mock(Deployment.class);
    when(deploymentConfigurationMock.getMuleVersion()).thenReturn(Optional.of("4.0.0"));
  }

  @Test
  void validateTest(@TempDir Path projectBaseDir) throws IOException, ValidationException {
    Path muleArtifactJsonPath = projectBaseDir.resolve("mule-artifact.json");
    String validMuleArtifactJson = "{ \"name\": \"test-project\", \"minMuleVersion\": \"4.0.0\", \"requiredProduct\": \"MULE\" }";

    FileUtils.writeStringToFile(muleArtifactJsonPath.toFile(), validMuleArtifactJson);
    assertThatCode(() -> MuleArtifactJsonValidator.validate(projectBaseDir, Optional.empty()))
        .doesNotThrowAnyException();
  }

  @Test
  public void isMuleArtifactJsonPresentFailTest() {
    assertThatThrownBy(() -> {
      muleArtifactJsonFile.delete();

      isMuleArtifactJsonPresent(projectBaseFolder.toAbsolutePath());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("Invalid Mule project. Missing mule-artifact.json file, it must be present in the root of application");
  }

  @Test
  public void isMuleArtifactJsonPresentTest() throws ValidationException {
    isMuleArtifactJsonPresent(projectBaseFolder.toAbsolutePath());
  }

  @Test
  public void isMuleArtifactJsonValidEmptyFileTest() {
    assertThatThrownBy(() -> isMuleArtifactJsonValid(projectBaseFolder.toAbsolutePath(), Optional.empty()))
        .isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("The mule-artifact.json file is empty");
  }

  @Test
  public void isMuleArtifactJsonValidInvalidJsonSyntaxTest() {
    assertThatThrownBy(() -> {
      FileUtils.writeStringToFile(muleArtifactJsonFile, "{}}}", (String) null);
      isMuleArtifactJsonValid(projectBaseFolder.toAbsolutePath(), Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("JsonSyntaxException");
  }

  @Test
  public void isMuleArtifactJsonValidArbitraryIOExceptionTest() {
    assertThatThrownBy(() -> {
      muleArtifactJsonFile.delete();
      muleArtifactJsonFile = projectBaseFolder.resolve(MULE_ARTIFACT_JSON).toFile();
      isMuleArtifactJsonValid(projectBaseFolder.toAbsolutePath(), Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("java.io.FileNotFoundException");
  }

  @Test
  public void checkNameMissingTest() {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{}");

    checkName(muleArtifact, missingFields);

    assertThat(missingFields).describedAs("Missing fields should contain the name field name").contains("name");
  }

  @Test
  public void checkNameTest() {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ name:aaa }");

    checkName(muleArtifact, missingFields);

    assertThat(missingFields.isEmpty()).describedAs("Missing fields should be empty").isTrue();
  }

  @Test
  public void checkMinMuleVersionValueMissingTest() throws ValidationException {
    MuleApplicationModel muleArtifact = new MuleApplicationModelJsonSerializer().deserialize("{ }");

    checkMinMuleVersionValue(muleArtifact, missingFields, Optional.empty());

    assertThat(missingFields).describedAs("Missing fields should contain the minMuleVersion field name")
        .contains("minMuleVersion");
  }

  @Test
  public void checkMinMuleVersionValueTest() throws ValidationException {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.0.0 }");

    checkMinMuleVersionValue(muleArtifact, missingFields, Optional.empty());

    assertThat(missingFields.isEmpty()).describedAs("Missing fields should be empty").isTrue();
  }

  @Test
  public void checkMinMuleVersionCompatibleVersionTest() throws ValidationException {
    Optional<String> deployMuleVersion = Optional.of("4.4.0");

    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.0.0 }");

    checkMinMuleVersionValue(muleArtifact, missingFields, deployMuleVersion);

    assertThat(missingFields.isEmpty()).describedAs("Missing fields should be empty").isTrue();
  }

  @Test
  public void checkClassLoaderModelDescriptorMissingTest() {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ }");

    checkClassLoaderModelDescriptor(muleArtifact, missingFields);

    assertThat(missingFields)
        .describedAs("Missing fields should contain the classLoaderModelLoaderDescriptor and classLoaderModelLoaderDescriptor.id fields names")
        .contains("classLoaderModelLoaderDescriptor", "classLoaderModelLoaderDescriptor.id");
  }

  @Test
  public void checkClassLoaderModelDescriptorMissingIdTest() {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ classLoaderModelLoaderDescriptor: {} }");

    checkClassLoaderModelDescriptor(muleArtifact, missingFields);

    assertThat(missingFields.size()).describedAs("There should be just one missing field").isEqualTo(1);
    assertThat(missingFields).describedAs("Missing fields should contain the classLoaderModelLoaderDescriptor.id fields names")
        .contains("classLoaderModelLoaderDescriptor.id");
  }

  @Test
  public void checkClassLoaderModelDescriptorTest() {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ classLoaderModelLoaderDescriptor: { id:mule } }");

    checkClassLoaderModelDescriptor(muleArtifact, missingFields);

    assertThat(missingFields.isEmpty()).describedAs("Missing fields should be empty").isTrue();
  }

  @Test
  public void validateMuleArtifactMandatoryFieldsMissingTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer()
              .deserialize("{ }");

      validateMuleArtifactMandatoryFields(muleArtifact, Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("The following mandatory fields in the mule-artifact.json are missing or invalid: [name, minMuleVersion, requiredProduct]");
  }

  @Test
  public void validateMuleArtifactMandatoryFieldsMissingNameTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer()
              .deserialize("{ minMuleVersion:4.0.0, classLoaderModelLoaderDescriptor: { id:mule }, requiredProduct: MULE }");

      validateMuleArtifactMandatoryFields(muleArtifact, Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("The following mandatory fields in the mule-artifact.json are missing or invalid: [name]");
  }

  @Test
  public void validateMuleArtifactMandatoryFieldsMissingMinMuleVersionTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer()
              .deserialize("{ name:lala, classLoaderModelLoaderDescriptor: { id:mule }, requiredProduct: MULE }");

      validateMuleArtifactMandatoryFields(muleArtifact, Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("The following mandatory fields in the mule-artifact.json are missing or invalid: [minMuleVersion]");
  }

  @Test
  public void validateMuleArtifactMandatoryFieldsMissingclassLoaderModelLoaderDescriptorTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer()
              .deserialize("{ name:lala, minMuleVersion:4.0.0, requiredProduct: MULE_EEa }");

      validateMuleArtifactMandatoryFields(muleArtifact, Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("The following mandatory fields in the mule-artifact.json are missing or invalid: [requiredProduct]. requiredProduct valid values are: MULE, MULE_EE");
  }

  @Test
  public void validateMuleArtifactMandatoryFieldsMissingNameAndClassLoaderModelLoaderDescriptorIdTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer()
              .deserialize("{ minMuleVersion:4.0.0, classLoaderModelLoaderDescriptor: {  }, requiredProduct: MULE }");

      validateMuleArtifactMandatoryFields(muleArtifact, Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("The following mandatory fields in the mule-artifact.json are missing or invalid: [name]");
  }

  @Test
  public void validateMuleArtifactMandatoryFieldsMissingRequiredProductTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer()
              .deserialize("{ name:lala, minMuleVersion:4.0.0, classLoaderModelLoaderDescriptor: { id:mule } }");

      validateMuleArtifactMandatoryFields(muleArtifact, Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("The following mandatory fields in the mule-artifact.json are missing or invalid: [requiredProduct]");
  }

  @Test
  public void checkMinMuleVersionInvalidValueTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.0 }");

      checkMinMuleVersionValue(muleArtifact, missingFields, Optional.empty());
    }).isExactlyInstanceOf(ValidationException.class)
        .hasMessageContaining("Version 4.0 does not comply with semantic versioning specification");
  }

  @Test
  public void checkMinMuleVersionAgainstDeploymentInvalidValueTest() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.0.0 }");

      checkMinMuleVersionValue(muleArtifact, missingFields, Optional.of("3.8.0"));
    }).isExactlyInstanceOf(ValidationException.class);
  }

  @Test
  public void checkMinMuleVersionAgainstDeploymentInvalidValue2Test() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.1.0 }");

      checkMinMuleVersionValue(muleArtifact, missingFields, Optional.of("4.0.0"));
    }).isExactlyInstanceOf(ValidationException.class);
  }

  @Test
  public void checkMinMuleVersionAgainstDeploymentInvalidValue3Test() {
    assertThatThrownBy(() -> {
      MuleApplicationModel muleArtifact =
          new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.0.1 }");

      checkMinMuleVersionValue(muleArtifact, missingFields, Optional.of("4.0.0"));
    }).isExactlyInstanceOf(ValidationException.class);
  }

  @Test
  public void checkMinMuleVersionAgainstDeploymentValidValueTest() throws IOException, ValidationException {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.0.1 }");

    when(deploymentConfigurationMock.getMuleVersion()).thenReturn(Optional.of("4.1.0"));
    checkMinMuleVersionValue(muleArtifact, missingFields, Optional.empty());
  }

  @Test
  public void checkMinMuleVersionAgainstDeploymentValidValue2Test() throws IOException, ValidationException {
    MuleApplicationModel muleArtifact =
        new MuleApplicationModelJsonSerializer().deserialize("{ minMuleVersion:4.10.10 }");

    when(deploymentConfigurationMock.getMuleVersion()).thenReturn(Optional.of("5.0.0"));
    checkMinMuleVersionValue(muleArtifact, missingFields, Optional.empty());
  }

}
