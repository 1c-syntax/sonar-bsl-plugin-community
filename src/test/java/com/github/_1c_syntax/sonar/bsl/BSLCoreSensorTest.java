package com.github._1c_syntax.sonar.bsl;

import com.github._1c_syntax.sonar.bsl.language.BSLLanguage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BSLCoreSensorTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final File moduleBaseDir = new File("src/test/files").getAbsoluteFile();
    private File workDir = null;

    @Before
    public void setup() throws Exception {
        if (workDir == null) {
            setupWorkDir();
        }
    }

    public void setupWorkDir() throws Exception {

        workDir = testFolder.newFolder("sensor");
        Path file1SourcePath = new File(moduleBaseDir, "src/UsingCancelParameterDiagnostic.bsl").toPath();
        Path file1TargetPath = new File(workDir, "test.bsl").toPath();

        Files.copy(new File(moduleBaseDir, "src/sonar-project.properties").toPath(),
                new File(workDir, "sonar-project.properties").toPath());
        Files.copy(file1SourcePath, file1TargetPath);
    }

    @Test
    public void test_execute() throws Exception {

        SensorContextTester context = SensorContextTester.create(workDir);
        context.fileSystem().setWorkDir(workDir.toPath());
        BSLCoreSensor sensor = new BSLCoreSensor(context);

        DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
        sensor.describe(sensorDescriptor);
        sensor.execute(context);

        // TODO: Проверить
    }

}
