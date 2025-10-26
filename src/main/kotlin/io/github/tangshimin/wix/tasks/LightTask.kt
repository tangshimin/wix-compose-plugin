package io.github.tangshimin.wix.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import io.github.tangshimin.wix.WixComposeExtension

abstract class LightTask : DefaultTask() {

    @get:Internal
    abstract val extension: Property<WixComposeExtension>

    @TaskAction
    fun light() {
        val appDir = project.layout.projectDirectory.dir("build/compose/binaries/main/app/")

        var lightFile = project.layout.projectDirectory.file("build/wix311/light.exe").asFile
        if (!lightFile.exists()) {
            lightFile = project.layout.projectDirectory.file("wix311/light.exe").asFile
        }

        project.exec {
            workingDir(appDir)
            val light = lightFile.absolutePath
                commandLine(
                    light,
                    "-ext",
                    "WixUIExtension",
                    "-cultures:zh-CN",
                    "-spdb",
                    "-nologo",
                    "${project.name}.wixobj",
                    "-o",
                    "${project.name}-${project.version}.msi")

        }
    }
}
