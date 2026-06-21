package com.example.klibextractor

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

class KlibNativeExtractorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create<KlibNativeExtractorExtension>("klibNativeExtractor").apply {
            outputDir.convention(project.layout.buildDirectory.dir("klibNativeLibs"))
            copyToBinaryOutput.convention(true)
            runtimeExtensions.convention(listOf("so", "dll", "dylib"))
        }

        project.plugins.withType(KotlinMultiplatformPluginWrapper::class.java) {
            val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            val aggregateTask = project.tasks.register("extractAllKlibNativeLibs") {
                group = "klib"
                description = "Extrae los binarios nativos de los klib de dependencias de todos los targets nativos"
            }

            kotlinExtension.targets.configureEach {
                val target = this
                target.compilations.configureEach {
                    val compilation = this
                    if (compilation !is KotlinNativeCompilation) return@configureEach

                    val targetName = target.name.replaceFirstChar { it.uppercase() }
                    val compilationName = compilation.name.replaceFirstChar { it.uppercase() }
                    val taskName = "extract${targetName}${compilationName}KlibNativeLibs"

                    val extractTask = project.tasks.register<ExtractKlibNativeLibsTask>(taskName) {
                        group = "klib"
                        description = "Extrae binarios nativos de los klib de las dependencias de ${target.name}/${compilation.name}"

                        // Únicamente los .klib que llegan como dependencia (incluye
                        // transitivas). NO se toca el .klib que genera este mismo
                        // proyecto, así esta task no depende de compileKotlin<Target>
                        // ni de linkTask y puede correr antes que ambas.
                        klibFiles.from(project.provider {
                            compilation.compileDependencyFiles.filter { it.extension == "klib" }
                        })

                        outputDirectory.set(extension.outputDir.dir("${target.name}/${compilation.name}"))
                    }

                    aggregateTask.configure { dependsOn(extractTask) }

                    // Solo tiene sentido copiar al lado del binario para targets nativos
                    // ejecutables/test (no para compilaciones de metadata, por ejemplo).
                    if (target is KotlinNativeTarget && extension.copyToBinaryOutput.get()) {
                        wireCopyToBinaries(project, target, compilation, extractTask, extension)
                    }
                }
            }
        }
    }

    private fun wireCopyToBinaries(
        project: Project,
        target: KotlinNativeTarget,
        compilation: KotlinNativeCompilation,
        extractTask: org.gradle.api.tasks.TaskProvider<ExtractKlibNativeLibsTask>,
        extension: KlibNativeExtractorExtension,
    ) {
        target.binaries.configureEach {
            val binary = this
            // Solo nos interesan los binarios que linkean justo esta compilation.
            if (binary.compilation != compilation) return@configureEach

            val linkTaskProvider = binary.linkTaskProvider
            val copyTaskName = "copy${target.name.replaceFirstChar { it.uppercase() }}" +
                    "${binary.name.replaceFirstChar { it.uppercase() }}KlibNativeLibs"

            if (project.tasks.findByName(copyTaskName) != null) return@configureEach

            val copyTask = project.tasks.register<CopyKlibNativeLibsToBinaryOutputTask>(copyTaskName) {
                group = "klib"
                description = "Copia .so/.dll/.dylib extraídos junto al binario ${binary.name} de ${target.name}"

                dependsOn(extractTask, linkTaskProvider)

                runtimeLibFiles.from(project.provider {
                    val wantedExtensions = extension.runtimeExtensions.get()
                    project.fileTree(extractTask.get().outputDirectory) {
                        include(wantedExtensions.map { "**/*.$it" })
                    }
                })

                binaryOutputDirectory.set(project.layout.dir(linkTaskProvider.map { it.outputFile.get().parentFile }))
            }

            linkTaskProvider.configure { finalizedBy(copyTask) }
        }
    }
}
