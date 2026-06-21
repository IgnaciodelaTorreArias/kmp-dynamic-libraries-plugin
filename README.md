# klib-native-extractor

Plugin de Gradle que extrae los archivos `.a` embebidos dentro de los `.klib`
de un proyecto Kotlin Multiplatform.

## Por qué funciona así

Un `.klib` es internamente un archivo ZIP. La mayoría de los klib no traen
binarios nativos, pero algunos (típicamente klibs de cinterop que empaquetan
una librería estática prebuilt) sí incluyen archivos `.a` bajo
`targets/<target>/native/`. Este plugin abre cada `.klib` —tanto el que
genera tu propio módulo como los que llegan como dependencia— y copia afuera
cualquier entry que termine en `.a`.

## Instalación

### Opción A: como proyecto incluido (composite build)

En el `settings.gradle.kts` del proyecto consumidor:

```kotlin
includeBuild("ruta/a/klib-native-extractor")
```

### Opción B: publicarlo a un repositorio (mavenLocal, etc.)

```bash
./gradlew publishToMavenLocal
```

Y en el consumidor:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

## Uso en el proyecto consumidor

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("com.example.klib-native-extractor") version "1.0.0"
}

klibNativeExtractor {
    outputDir.set(layout.buildDirectory.dir("nativeStaticLibs")) // opcional
    includeDependencies.set(true) // opcional, default true
}
```

Esto registra, por cada compilación nativa (`iosX64/main`, `macosArm64/main`, etc.):

```
extract<Target><Compilation>KlibNativeLibs
```

y una task agregadora:

```bash
./gradlew extractAllKlibNativeLibs
```

Los `.a` quedan en:

```
build/klibNativeLibs/<target>/<compilation>/<nombreDelKlib>/*.a
```

## Notas

- Si ningún klib trae `.a` embebido, la task simplemente no copia nada
  (no falla).
- `compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:...")` en el
  `build.gradle.kts` del plugin debe alinearse con la versión de Kotlin que
  use el proyecto consumidor; si usás una versión de Kotlin distinta,
  actualizá esa dependencia.
