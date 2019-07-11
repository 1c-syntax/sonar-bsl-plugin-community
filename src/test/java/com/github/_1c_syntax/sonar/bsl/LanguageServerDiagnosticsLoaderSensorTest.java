package com.github._1c_syntax.sonar.bsl;

import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageServerDiagnosticsLoaderSensorTest {

    @Test
    public void test_run(){

        SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(Version.create(7, 8));

        String basePath = "src/test/files";
        File baseDir = new File(basePath);

        SensorContextTester context = SensorContextTester.create(baseDir);
        LanguageServerDiagnosticsLoaderSensor diagnosticsLoaderSensor = new LanguageServerDiagnosticsLoaderSensor(context);
        DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
        diagnosticsLoaderSensor.describe(sensorDescriptor);
        diagnosticsLoaderSensor.execute(context);
        // TODO: проверку

    }


}
