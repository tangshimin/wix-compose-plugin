package io.github.tangshimin.wix

import org.gradle.api.Plugin
import org.gradle.api.Project
import io.github.tangshimin.wix.tasks.*

class WixComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("wixCompose", WixComposeExtension::class.java)

        project.tasks.register("harvest", HarvestTask::class.java).configure {
            group = "wix compose"
            description = "Generates WiX authoring from application image"
            this.extension.set(extension)
        }

        // 在 harvest 任务配置中直接设置依赖关系
        project.tasks.named("harvest").configure {
            // 尝试设置依赖关系，如果任务存在
            try {
                dependsOn("createDistributable")
                logger.info("DEBUG: Successfully set harvest dependsOn createDistributable")
            } catch (e: Exception) {
                logger.info("DEBUG: Failed to set dependency: ${e.message}")
            }
        }

        project.tasks.register("editWxs", EditWxsTask::class.java).configure {
            group = "wix compose"
            description = "Edit the WXS File"
            this.extension.set(extension)
            dependsOn("harvest")
        }

        project.tasks.register("compileWxs", CompileWxsTask::class.java).configure {
            group = "wix compose"
            description = "Compile WXS file to WIXOBJ"
            this.extension.set(extension)
            dependsOn("editWxs")
        }

        project.tasks.register("light", LightTask::class.java).configure {
            group = "wix compose"
            description = "Linking the .wixobj file and creating a MSI"
            this.extension.set(extension)
            dependsOn("compileWxs")
        }
    }
}
