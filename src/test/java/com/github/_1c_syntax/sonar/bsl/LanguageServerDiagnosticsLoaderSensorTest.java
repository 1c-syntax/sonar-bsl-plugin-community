package com.github._1c_syntax.sonar.bsl;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageServerDiagnosticsLoaderSensorTest {

    final String BASE_PATH = "src/test/files";

    @Test
    public void test_describe(){

        File baseDir = new File(BASE_PATH);
        SensorContextTester context = SensorContextTester.create(baseDir);
        LanguageServerDiagnosticsLoaderSensor diagnosticsLoaderSensor = new LanguageServerDiagnosticsLoaderSensor(context);
        DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
        diagnosticsLoaderSensor.describe(sensorDescriptor);

        assertThat(sensorDescriptor.name()).containsIgnoringCase("BSL Language Server diagnostics loader");

    }

    @Test
    public void test_execute() {

        File baseDir = new File(BASE_PATH);
        SensorContextTester context = SensorContextTester.create(baseDir);
        LanguageServerDiagnosticsLoaderSensor diagnosticsLoaderSensor = new LanguageServerDiagnosticsLoaderSensor(context);
        diagnosticsLoaderSensor.execute(context);

    }

}
