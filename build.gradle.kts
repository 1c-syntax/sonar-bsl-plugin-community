import java.net.URI
import java.util.*

plugins {
    jacoco
    java
    `maven-publish`
    id("org.sonarqube") version "6.1.0.5360"
    id("org.cadixdev.licenser") version "0.6.1"
    id("com.github.johnrengelman.shadow") version ("7.0.0")
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.github.gradle-git-version-calculator") version "1.1.0"
    id("io.freefair.lombok") version "8.13.1"
}

group = "io.github.1c-syntax"
version = gitVersionCalculator.calculateVersion("v")

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = URI("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

val sonarQubeVersion = "9.9.0.65466"

dependencies {
    implementation("org.sonarsource.api.plugin", "sonar-plugin-api", "9.14.0.375")

    implementation("io.github.1c-syntax", "bsl-language-server", "0.24.1") {
        exclude("com.contrastsecurity", "java-sarif")
        exclude("io.sentry", "sentry-logback")
        exclude("org.springframework.boot", "spring-boot-starter-websocket")
    }

    implementation("org.sonarsource.analyzer-commons", "sonar-analyzer-commons", "2.5.0.1358")

    // MD to HTML converter of BSL LS rule descriptions
    implementation("org.commonmark", "commonmark", "0.24.0")
    implementation("org.commonmark", "commonmark-ext-gfm-tables", "0.24.0")
    implementation("org.commonmark", "commonmark-ext-autolink", "0.24.0")
    implementation("org.commonmark", "commonmark-ext-heading-anchor", "0.24.0")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.11.4")
    testImplementation("org.assertj", "assertj-core", "3.27.0")
    testImplementation("org.mockito", "mockito-core", "5.14.2")
    testImplementation("org.sonarsource.sonarqube", "sonar-testing-harness", sonarQubeVersion)
    testImplementation("org.sonarsource.sonarqube", "sonar-core", sonarQubeVersion)
    testImplementation("org.reflections", "reflections", "0.10.2")

    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.11.4")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }

    reports {
        html.required.set(true)
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(File("${layout.buildDirectory.get()}/reports/jacoco/test/jacoco.xml"))
    }
}

license {
    header(rootProject.file("license/HEADER.txt"))
    newLine(false)
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
    ext["name"] = "Alexey Sosnoviy <labotamy@gmail.com>, Nikita Fedkin <nixel2007@gmail.com>"
    ext["project"] = "SonarQube 1C (BSL) Community Plugin"
    exclude("**/*.properties")
    exclude("**/*.bsl")
    exclude("**/*.json")
}

sonarqube {
    properties {
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "1c-syntax")
        property("sonar.projectKey", "1c-syntax_sonar-bsl-plugin-community")
        property("sonar.projectName", "SonarQube 1C (BSL) Community Plugin")
        property("sonar.exclusions", "**/gen/**/*.*")
        property("sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get()}/reports/jacoco/test/jacoco.xml")
    }
}

tasks.jar {
    manifest {
        attributes["Plugin-Key"] = "communitybsl"
        attributes["Plugin-Description"] = "Code Analyzer for 1C (BSL)"
        attributes["Plugin-Class"] = "com.github._1c_syntax.bsl.sonar.BSLPlugin"
        attributes["Plugin-Name"] = "1C (BSL) Community Plugin"
        attributes["Plugin-Version"] = "${project.version}"
        attributes["Plugin-RequiredForLanguages"] = "bsl"

        attributes["Plugin-License"] = "GNU LGPL v3"

        attributes["Plugin-Homepage"] = "https://1c-syntax.github.io/sonar-bsl-plugin-community"
        attributes["Plugin-IssueTrackerUrl"] = "https://github.com/1c-syntax/sonar-bsl-plugin-community/issues"
        attributes["Plugin-SourcesUrl"] = "https://github.com/1c-syntax/sonar-bsl-plugin-community"
        attributes["Plugin-Developers"] = "Alexey Sosnoviy, Nikita Fedkin"

        attributes["SonarLint-Supported"] = false
        attributes["Sonar-Version"] = sonarQubeVersion

        attributes["Plugin-Organization"] = "1c-syntax"
        attributes["Plugin-OrganizationUrl"] = "https://github.com/1c-syntax"
    }

    enabled = false
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    configurations = listOf(project.configurations["runtimeClasspath"])
    archiveClassifier.set("")
}
