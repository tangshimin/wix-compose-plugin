
package io.github.tangshimin.wix

import org.gradle.api.provider.Property

abstract class WixComposeExtension {
    abstract val licenseFile: Property<String>
    abstract val iconFile: Property<String>
    abstract val manufacturer: Property<String>
    abstract val shortcutName: Property<String>
    abstract val cultures: Property<String>

    init {
        manufacturer.convention("未知")
        shortcutName.convention("应用程序")
        licenseFile.convention("")
        iconFile.convention("")
        cultures.convention("zh-CN")
    }
}
