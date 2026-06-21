plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.example.klibextractor"
version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Solo necesario para acceder a los tipos del plugin de Kotlin Multiplatform.
    // Ajustá la versión a la que use el proyecto donde se aplicará el plugin.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
}

gradlePlugin {
    plugins {
        create("klibNativeExtractor") {
            id = "com.example.klibextractor.klib-native-extractor"
            implementationClass = "com.example.klibextractor.KlibNativeExtractorPlugin"
            displayName = "Klib Native Libs Extractor"
            description = "Extrae los archivos de librerias dinamicas embebidas en los .klib de un proyecto Kotlin Multiplatform"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "com.example.klibextractor"
            artifactId = "klib-native-extractor"
            version = "1.0.0"
        }
    }
}

//            from(components["java"])
kotlin {
    jvmToolchain(17)
}
