import java.net.URI
import java.util.*

plugins {
    jacoco
    java
    maven
    id("org.sonarqube") version "2.8"
    id("com.github.hierynomus.license") version "0.15.0"
    id("com.github.johnrengelman.shadow") version("5.2.0")
    id("com.github.ben-manes.versions") version "0.27.0"
    id("com.github.gradle-git-version-calculator") version "1.1.0"
}

group = "com.github.1c-syntax"
version = gitVersionCalculator.calculateVersion("v")

repositories {
    mavenCentral()
    maven {
        url = URI("https://jitpack.io")
    }
}

val commonmarkVersion = "0.14.0"
val junitVersion = "5.6.0"

dependencies {
    implementation("org.sonarsource.sonarqube:sonar-plugin-api:7.9")

    compile("com.github.1c-syntax:bsl-language-server:v0.13.1")
    compile("com.fasterxml.jackson.core:jackson-databind:2.10.2")
    compile("com.google.code.findbugs:jsr305:3.0.2")
    // https://mvnrepository.com/artifact/org.sonarsource.analyzer-commons/sonar-analyzer-commons
    compile("org.sonarsource.analyzer-commons:sonar-analyzer-commons:1.11.0.541")

    // MD to HTML converter of BSL LS rule descriptions
    compile("com.atlassian.commonmark", "commonmark", commonmarkVersion)
    compile("com.atlassian.commonmark", "commonmark-ext-gfm-tables", commonmarkVersion)
    compile("com.atlassian.commonmark", "commonmark-ext-autolink", commonmarkVersion)
    compile("com.atlassian.commonmark", "commonmark-ext-heading-anchor", commonmarkVersion)

    compile("me.tongfei:progressbar:0.8.0")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
    
    testCompile("org.assertj:assertj-core:3.14.0")
    testCompile("org.mockito:mockito-core:3.2.4")
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
        html.isEnabled = true
    }
}

jacoco {
    toolVersion = "0.8.2"
}

tasks.check {
    dependsOn(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        xml.destination = File("$buildDir/reports/jacoco/test/jacoco.xml")
    }
}

license {
    header = rootProject.file("license/HEADER.txt")
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
    ext["name"] = "Nikita Gryzlov <nixel2007@gmail.com>"
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
        attributes["Plugin-Developers"] = "Nikita Gryzlov"

        attributes["SonarLint-Supported"] = true
        attributes["Sonar-Version"] = "7.9"

        attributes["Plugin-Organization"] = "1c-syntax"
        attributes["Plugin-OrganizationUrl"] = "https://github.com/1c-syntax"
    }
    configurations["compile"].forEach {
        from(zipTree(it.absoluteFile)) {
            exclude("META-INF/MANIFEST.MF")
            exclude("META-INF/*.SF")
            exclude("META-INF/*.DSA")
            exclude("META-INF/*.RSA")
        }
    }
}
