import java.net.URI
import java.util.*

plugins {
    jacoco
    java
    `maven-publish`
    id("org.sonarqube") version "4.0.0.2929"
    id("org.cadixdev.licenser") version "0.6.1"
    id("com.github.johnrengelman.shadow") version("7.0.0")
    id("com.github.ben-manes.versions") version "0.46.0"
    id("com.github.gradle-git-version-calculator") version "1.1.0"
    id("io.freefair.lombok") version "6.6.3"
}

group = "io.github.1c-syntax"
version = gitVersionCalculator.calculateVersion("v")

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = URI("https://s01.oss.sonatype.org/content/repositories/snapshots")   
    }
    maven {
        url = URI("https://jitpack.io")
    }
}

val sonarQubeVersion = "8.9.0.43852"

dependencies {
    implementation("org.sonarsource.sonarqube", "sonar-plugin-api", sonarQubeVersion)

    // в jitpack лежат в группе com.github.1c-syntax, в централе - io.github.1c-syntax
    implementation("com.github.1c-syntax", "bsl-language-server", "v0.21.0-ra.5") {
        exclude("com.github.1c-syntax", "utils")
    }
    implementation("com.github.1c-syntax", "utils", "f1694d9c")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

    // https://mvnrepository.com/artifact/org.sonarsource.analyzer-commons/sonar-analyzer-commons
    implementation("org.sonarsource.analyzer-commons:sonar-analyzer-commons:2.4.0.1317")

    // MD to HTML converter of BSL LS rule descriptions
    implementation("org.commonmark", "commonmark", "0.21.0")
    implementation("org.commonmark", "commonmark-ext-gfm-tables", "0.21.0")
    implementation("org.commonmark", "commonmark-ext-autolink", "0.21.0")
    implementation("org.commonmark", "commonmark-ext-heading-anchor", "0.21.0")

    implementation("me.tongfei:progressbar:0.9.5")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.0")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.0")

    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:5.1.1")

    testImplementation("org.sonarsource.sonarqube", "sonar-testing-harness", sonarQubeVersion)
    testImplementation("org.sonarsource.sonarqube", "sonar-core", sonarQubeVersion)
    testImplementation("org.reflections", "reflections", "0.9.12")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
        xml.outputLocation.set(File("$buildDir/reports/jacoco/test/jacoco.xml"))
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
        property("sonar.coverage.jacoco.xmlReportPaths", "$buildDir/reports/jacoco/test/jacoco.xml")
    }
}

tasks.jar {
    manifest {
        attributes["Plugin-Key"] = "communitybsl"
        attributes["Plugin-Description"] = "Code Analyzer for 1C (BSL)"
        attributes["Plugin-Class"] = "com.github._1c_syntax.bsl.sonar.BSLPlugin"
        attributes["Plugin-Name"] = "1C (BSL) Community Plugin"
        attributes["Plugin-Version"] = "${project.version}"

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
    project.configurations.implementation.get().isCanBeResolved = true
    configurations = listOf(project.configurations["implementation"])
    archiveClassifier.set("")
}
