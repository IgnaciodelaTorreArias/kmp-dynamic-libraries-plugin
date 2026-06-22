plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.36.0"
}

group = "io.github.ignaciodelatorrearias"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
}

gradlePlugin {
    plugins {
        create("klibNativeExtractor") {
            id = "$group.${rootProject.name}"
            implementationClass = "$group.klibextractor.KlibNativeExtractorPlugin"
            displayName = "Klib Native dynamic libraries extractor"
            description = "Extracts dynamic libraries from .klib dependencies in Kotlin Multiplatform projects"
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), rootProject.name, version.toString())
    pom {
        name.set("kmp dynamic")
        description.set("A plugin to test a way to use dynamic libraries ")
        inceptionYear.set("2026")
        url.set("https://github.com/IgnaciodelaTorreArias/kmp-dynamic-libraries-plugin/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("IgnaciodelaTorreArias")
                name.set("Ignacio de la Torre Arias")
                url.set("https://github.com/IgnaciodelaTorreArias/")
            }
        }
        scm {
            url.set("https://github.com/IgnaciodelaTorreArias/kmp-dynamic-libraries-plugin/")
            connection.set("scm:git:git://github.com/IgnaciodelaTorreArias/kmp-dynamic-libraries-plugin.git")
            developerConnection.set("scm:git:ssh://git@github.com/IgnaciodelaTorreArias/kmp-dynamic-libraries-plugin.git")
        }
    }
    signAllPublications()
}

kotlin {
    jvmToolchain(17)
}
