package io.github.tangshimin.wix.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import io.github.tangshimin.wix.WixComposeExtension

abstract class CompileWxsTask : DefaultTask() {

    @get:Internal
    abstract val extension: Property<WixComposeExtension>

    @TaskAction
    fun compileWxs() {
        val appDir = project.layout.projectDirectory.dir("build/compose/binaries/main/app/")

        var candleFile = project.layout.projectDirectory.file("build/wix311/candle.exe").asFile
        // 有的版本的 wix311 在 build 目录下，有的版本在项目根目录下
        if (!candleFile.exists()) {
            candleFile = project.layout.projectDirectory.file("wix311/candle.exe").asFile
        }

        project.exec {
            workingDir(appDir)
            val candle = candleFile.absolutePath
            commandLine(
                candle,
                "${project.name}.wxs",
                "-nologo",
                "-dSourceDir=.\\${project.name}"
            )
        }
    }
}
