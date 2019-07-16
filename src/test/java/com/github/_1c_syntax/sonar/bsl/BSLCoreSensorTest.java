package com.github._1c_syntax.sonar.bsl;

import org.junit.Test;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;

public class BSLCoreSensorTest {

    private static final File BASE_DIR = new File("src/test/files").getAbsoluteFile();
    private SensorContextTester context = SensorContextTester.create(BASE_DIR);

    @Test
    public void test_descriptor() {
        BSLCoreSensor sensor = new BSLCoreSensor(context);
        DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
        sensor.describe(sensorDescriptor);
    }

    @Test
    public void test_execute() {

        // TODO: реализовать BSLCoreSensor -> execute

    }


}
