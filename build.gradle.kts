plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.7.2"
}

group = "com.github.aemtoolkit"
version = "0.7.7"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        bundledPlugin("com.intellij.java")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        name = "AEM Toolkit"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "251"
        }

        vendor {
            name = "cheese-zhang"
            email = "18023108+cheese-zhang@users.noreply.github.com"
            url = "https://github.com/cheese-zhang"
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    named("instrumentCode") {
        enabled = false
    }

    named("instrumentTestCode") {
        enabled = false
    }

    named("buildSearchableOptions") {
        enabled = false
    }

    test {
        useJUnitPlatform()
    }
}
