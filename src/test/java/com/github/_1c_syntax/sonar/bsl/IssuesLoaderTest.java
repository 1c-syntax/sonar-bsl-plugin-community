package com.github._1c_syntax.sonar.bsl;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;

public class IssuesLoaderTest {

    @Test
    public void test_createIssue(){

        String basePath = "src/test/files";
        File baseDir = new File(basePath);
        SensorContextTester context = SensorContextTester.create(baseDir);

        IssuesLoader issuesLoader = new IssuesLoader(context);

    }

}
