package io.github.ignaciodelatorrearias.klibextractor

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.util.zip.ZipFile

class KlibNativeExtractorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Maybe the process could be more efficient by analyzing KonanTarget's and the dependencies?
//        project.afterEvaluate {
//            val kotlinExtension = extensions.getByType(KotlinMultiplatformExtension::class.java)
//            println("TARGETS = ${kotlinExtension.targets.size}")
//            kotlinExtension.targets.forEach {
//                if (it.platformType != KotlinPlatformType.native) return@forEach
//                println("${it.targetName}")
//            }
//        }
        project.tasks.withType(KotlinNativeLink::class.java).configureEach {
            val nativeLibrariesDir = project.layout.buildDirectory
                .dir("bin/libraries/${binary.target.konanTarget.name}").get().asFile
            val linkTimeExtensions = listOf("so", "dll.lib", "dll", "dylib")
            val runTimeExtensions = listOf("so", "dll", "dylib")
            doFirst {
                // Clear native libraries since we can't know which ones should be present
                // since user can always delete a dependency that was bringing a library
                nativeLibrariesDir.deleteRecursively()
                // Here we get all .klib's necessary for this link task
                // We only need the {package}-{KonanTarget name}cinterop-{CInteropSettings name}.klib
                libraries.filter { it.isFile && it.extension == "klib" }.forEach { file ->
                    // Read the contents of the klib
                    ZipFile(file).use { zip ->
                        zip.entries().asSequence()
                            // Filter only the dynamic libraries
                            .filter { entry ->
                                (!entry.isDirectory)
                                        &&
                                linkTimeExtensions.any { entry.name.endsWith(".$it") }
                            }
                            .forEach { entry ->
                                // Create folder to store required dynamic libraries for linker
                                nativeLibrariesDir.mkdirs()
                                val fileName = entry.name.substringAfterLast('/')
                                val targetFile = nativeLibrariesDir.resolve(fileName)
                                // copy required libraries from inside the .klib
                                zip.getInputStream(entry).use { input ->
                                    targetFile.outputStream().use { output -> input.copyTo(output) }
                                }
                            }
                    }
                }
            }
            doLast {
                destinationDirectory.files()
                    .filter { file -> !file.isDirectory && runTimeExtensions.any { file.name.endsWith(".$it") } }
                    .forEach { it.delete() }
                nativeLibrariesDir.listFiles()
                    ?.filter { file -> runTimeExtensions.any { file.name.endsWith(".$it") } } // .dll.lib no hace falta en runtime
                    ?.forEach { it.copyTo(destinationDirectory.file(it.name).get().asFile, overwrite = true) }
            }
        }
    }
}
