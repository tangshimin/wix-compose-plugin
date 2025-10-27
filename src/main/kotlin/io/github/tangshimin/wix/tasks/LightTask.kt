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
        val ext = extension.get()

        var lightFile = project.layout.projectDirectory.file("build/wix311/light.exe").asFile
        if (!lightFile.exists()) {
            lightFile = project.layout.projectDirectory.file("wix311/light.exe").asFile
        }

        project.exec {
            workingDir(appDir)
            val light = lightFile.absolutePath
            val commandLineArgs = mutableListOf(
                light,
                "-ext",
                "WixUIExtension",
                "-spdb",
                "-nologo",
                "${project.name}.wixobj",
                "-o",
                "${project.name}-${project.version}.msi"
            )

            // 添加 cultures 参数
            val cultures = ext.cultures.get()
            if (cultures.isNotEmpty()) {
                commandLineArgs.add(3, "-cultures:$cultures")  // 在 -spdb 参数之前添加
            }

            commandLine(commandLineArgs)
        }
    }
}
