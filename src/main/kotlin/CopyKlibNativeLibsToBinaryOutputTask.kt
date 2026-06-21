package com.example.klibextractor

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Copia los binarios runtime (.so/.dll/.dylib) ya extraídos del klib hacia
 * la carpeta donde el linker de Kotlin/Native deja el ejecutable/librería
 * final, para que el loader del SO los encuentre (al lado del .exe, o vía
 * rpath "$ORIGIN" en Linux/macOS).
 */
abstract class CopyKlibNativeLibsToBinaryOutputTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val runtimeLibFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val binaryOutputDirectory: DirectoryProperty

    @TaskAction
    fun copy() {
        val target = binaryOutputDirectory.get().asFile
        if (!target.exists()) {
            logger.warn("[klibNativeExtractor] El directorio de salida del binario no existe todavía: $target")
            return
        }

        runtimeLibFiles.files.filter { it.isFile }.forEach { lib ->
            val dest = target.resolve(lib.name)
            lib.copyTo(dest, overwrite = true)
            logger.lifecycle("[klibNativeExtractor] Copiado ${lib.name} -> ${dest.absolutePath}")
        }
    }
}
