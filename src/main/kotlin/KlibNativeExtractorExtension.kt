package com.example.klibextractor

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configuración expuesta al proyecto consumidor vía:
 *
 * ```kotlin
 * klibNativeExtractor {
 *     outputDir.set(layout.buildDirectory.dir("nativeStaticLibs"))
 *     includeDependencies.set(true)
 * }
 * ```
 */
abstract class KlibNativeExtractorExtension {

    /** Directorio raíz donde se copiarán los binarios extraídos. */
    abstract val outputDir: DirectoryProperty

    /**
     * Si es true (default), también revisa los .klib que llegan como
     * dependencia (no solo los que genera el propio proyecto).
     */
    abstract val includeDependencies: Property<Boolean>

    /**
     * Si es true, además de dejar los binarios en [outputDir], los copia
     * junto al ejecutable/librería final de cada `linkTask` (necesario para
     * que el loader de Windows/Linux/macOS encuentre el .dll/.so/.dylib en
     * runtime, ya que el linker de Kotlin/Native no hace esa copia).
     */
    abstract val copyToBinaryOutput: Property<Boolean>

    /**
     * Extensiones que se copian junto al binario final cuando
     * [copyToBinaryOutput] está activo. Default: so, dll, dylib (los .a no
     * hace falta copiarlos, ya quedaron linkeados estáticamente).
     */
    abstract val runtimeExtensions: ListProperty<String>
}
