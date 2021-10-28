import java.net.URI
import java.util.*

plugins {
    jacoco
    java
    `maven-publish`
    id("org.sonarqube") version "3.3"
    id("com.github.hierynomus.license") version "0.16.1"
    id("com.github.johnrengelman.shadow") version("7.0.0")
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.github.gradle-git-version-calculator") version "1.1.0"
    id("io.freefair.lombok") version "6.2.0"
}

group = "com.github.1c-syntax"
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

val commonmarkVersion = "0.17.0"
val junitVersion = "5.8.0"
val sonarQubeVersion = "8.9.0.43852"

dependencies {
    implementation("org.sonarsource.sonarqube", "sonar-plugin-api", sonarQubeVersion)

    implementation("io.github.1c-syntax", "bsl-language-server", "0.20.0-ra.1")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")

    // https://mvnrepository.com/artifact/org.sonarsource.analyzer-commons/sonar-analyzer-commons
    implementation("org.sonarsource.analyzer-commons:sonar-analyzer-commons:1.20.0.801")

    // MD to HTML converter of BSL LS rule descriptions
    implementation("com.atlassian.commonmark", "commonmark", commonmarkVersion)
    implementation("com.atlassian.commonmark", "commonmark-ext-gfm-tables", commonmarkVersion)
    implementation("com.atlassian.commonmark", "commonmark-ext-autolink", commonmarkVersion)
    implementation("com.atlassian.commonmark", "commonmark-ext-heading-anchor", commonmarkVersion)

    implementation("me.tongfei:progressbar:0.9.2")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)

    testImplementation("org.assertj:assertj-core:3.21.0")
    testImplementation("org.mockito:mockito-core:4.0.0")

    testImplementation("org.sonarsource.sonarqube", "sonar-plugin-api", sonarQubeVersion)
    testImplementation("org.sonarsource.sonarqube", "sonar-testing-harness", sonarQubeVersion)
    testImplementation("org.sonarsource.sonarqube", "sonar-core", sonarQubeVersion)
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
    header = rootProject.file("license/HEADER.txt")
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
    ext["name"] = "Alexey Sosnoviy <labotamy@gmail.com>, Nikita Fedkin <nixel2007@gmail.com>"
    ext["project"] = "SonarQube 1C (BSL) Community Plugin"
    strictCheck = true
    mapping("java", "SLASHSTAR_STYLE")
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

        attributes["SonarLint-Supported"] = true
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
