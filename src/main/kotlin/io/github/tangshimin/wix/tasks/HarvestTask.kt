package io.github.tangshimin.wix.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import io.github.tangshimin.wix.WixComposeExtension
import java.io.File

abstract class HarvestTask : DefaultTask() {

    @get:Internal
    abstract val extension: Property<WixComposeExtension>

    @TaskAction
    fun harvest() {
        val appDir = project.layout.projectDirectory.dir("build/compose/binaries/main/app/")

        var heatFile = project.layout.projectDirectory.file("build/wix311/heat.exe").asFile
        if (!heatFile.exists()) {
            heatFile = project.layout.projectDirectory.file("wix311/heat.exe").asFile
        }

        project.exec {
            workingDir(appDir)
            val heat = heatFile.absolutePath
            commandLine(
                heat,
                "dir",
                "./${project.name}",
                "-nologo",
                "-cg",
                "DefaultFeature",
                "-gg",
                "-sfrag",
                "-sreg",
                "-template",
                "product",
                "-out",
                "${project.name}.wxs",
                "-var",
                "var.SourceDir"
            )
        }
    }
}
