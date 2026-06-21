package com.example.klibextractor

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

/**
 * Un .klib es, en el fondo, un archivo ZIP. Algunas klib (sobre todo las
 * generadas vía cinterop con binarios nativos embebidos, sea `staticLibraries`
 * o `dynamicLibraries` declarado a mano) contienen binarios dentro de
 * `targets/<target>/native/`. Esta task abre cada klib y copia afuera
 * cualquier entry cuya extensión esté en [extensions].
 *
 * Importante: aunque la entry sea un .dll/.so "import library" usado solo
 * para resolver símbolos en link time, normalmente lo que necesitás en
 * runtime es el binario real con el mismo nombre — asegurate de que el
 * .def también lo declare/incluya si querés que termine acá.
 */
abstract class ExtractKlibNativeLibsTask : DefaultTask() {

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val klibFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun extract() {
        val outDir = outputDirectory.get().asFile
        outDir.mkdirs()

        val klibs = klibFiles.files.filter { it.isFile && it.extension == "klib" }

        if (klibs.isEmpty()) {
            logger.lifecycle("[klibNativeExtractor] No se encontraron archivos .klib para ${path}")
            return
        }

        klibs.forEach { klib ->
            runCatching {
                ZipFile(klib).use { zip ->
                    val wantedExtensions = listOf("so", "dll", "dylib")
                    val matchedEntries = zip.entries().asSequence()
                        .filter { entry -> !entry.isDirectory && wantedExtensions.any { entry.name.endsWith(".$it") } }
                        .toList()

                    if (matchedEntries.isEmpty()) return@use

                    val klibOutDir = outDir.resolve(klib.nameWithoutExtension).apply { mkdirs() }

                    matchedEntries.forEach { entry ->
                        // Nos quedamos solo con el nombre del archivo para evitar
                        // colisiones de path raras dentro del klib.
                        val fileName = entry.name.substringAfterLast('/')
                        val targetFile = klibOutDir.resolve(fileName)

                        zip.getInputStream(entry).use { input ->
                            targetFile.outputStream().use { output -> input.copyTo(output) }
                        }

                        logger.lifecycle(
                            "[klibNativeExtractor] ${klib.name} -> ${targetFile.relativeTo(project.rootDir)}"
                        )
                    }
                }
            }.onFailure { e ->
                logger.warn("[klibNativeExtractor] No se pudo procesar ${klib.name}: ${e.message}")
            }
        }
    }
}
