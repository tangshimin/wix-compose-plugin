package io.github.tangshimin.wix.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import io.github.tangshimin.wix.WixComposeExtension
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.OutputKeys
import java.nio.charset.StandardCharsets

abstract class EditWxsTask : DefaultTask() {

    @get:Internal
    abstract val extension: Property<WixComposeExtension>

    @TaskAction
    fun editWxs() {
        val ext = extension.get()
        editWixTask(
            shortcutName = ext.shortcutName.get(),
            iconPath = ext.iconFile.get(),
            licensePath = ext.licenseFile.get(),
            manufacturer = ext.manufacturer.get(),
            cultures = ext.cultures.get()
        )
    }

    private fun editWixTask(
        shortcutName: String,
        iconPath: String,
        licensePath: String,
        manufacturer: String,
        cultures: String
    ) {
        val wixFile = project.layout.projectDirectory.dir("build/compose/binaries/main/app/${project.name}.wxs").asFile

        val dbf = DocumentBuilderFactory.newInstance()
        val doc = dbf.newDocumentBuilder().parse(wixFile)
        doc.documentElement.normalize()

        // 设置 Product 节点
        val productElement = doc.documentElement.getElementsByTagName("Product").item(0) as Element

        // 设置升级码, 用于升级,大版本更新时，可能需要修改这个值
        val upgradeCode = createNameUUID("v1")
        val languageCode = getLanguageCode(cultures)
        productElement.apply {
            setAttribute("Manufacturer", manufacturer)
            setAttribute("Codepage", getCodePage(cultures))
            setAttribute("Name", shortcutName)
            setAttribute("Version", "${project.version}")
            setAttribute("UpgradeCode", upgradeCode)
            setAttribute("Language", languageCode)
        }

        // 设置 Package 节点
        val packageElement = productElement.getElementsByTagName("Package").item(0) as Element
        packageElement.apply {
            setAttribute("Compressed", "yes")
            setAttribute("InstallerVersion", "200")
            setAttribute("Languages", languageCode)
            setAttribute("Manufacturer", manufacturer)
            setAttribute("Platform", "x64")
        }

        // 更新现有的 ProductLanguage 属性（如果存在）
        val properties = productElement.getElementsByTagName("Property")
        for (i in 0 until properties.length) {
            val property = properties.item(i) as Element
            if (property.getAttribute("Id") == "ProductLanguage") {
                property.setAttribute("Value", languageCode)
                break
            }
        }

        val targetDirectory = doc.documentElement.getElementsByTagName("Directory").item(0) as Element

        // 桌面文件夹
        val desktopFolderElement = directoryBuilder(doc, id = "DesktopFolder").apply {
            setAttributeNode(doc.createAttribute("Name").also { it.value = "Desktop" })
        }
        val desktopGuid = createNameUUID("DesktopShortcutComponent")
        val desktopComponent = componentBuilder(doc, id = "DesktopShortcutComponent", guid = desktopGuid)
        val desktopReg = registryBuilder(doc, id = "DesktopShortcutReg", productCode = "[ProductCode]", manufacturer = manufacturer)
        val desktopShortcut = shortcutBuilder(
            doc,
            id = "DesktopShortcut",
            directory = "DesktopFolder",
            workingDirectory = "INSTALLDIR",
            name = shortcutName,
            target = "[INSTALLDIR]${project.name}.exe",
            icon = if(iconPath.isNotEmpty()) "icon.ico" else ""
        )
        val removeDesktopShortcut = doc.createElement("RemoveFile").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "DesktopShortcut" })
            setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
            setAttributeNode(doc.createAttribute("Name").also { it.value = "$shortcutName.lnk" })
            setAttributeNode(doc.createAttribute("Directory").also { it.value = "DesktopFolder" })
        }
        desktopComponent.appendChild(desktopShortcut)
        desktopComponent.appendChild(desktopReg)
        desktopComponent.appendChild(removeDesktopShortcut)
        desktopFolderElement.appendChild(desktopComponent)
        targetDirectory.appendChild(desktopFolderElement)

        // 开始菜单文件夹
        val programMenuFolderElement = directoryBuilder(doc, id = "ProgramMenuFolder", name = "Programs")
        val programeMenuDir = directoryBuilder(doc, id = "ProgramMenuDir", name = shortcutName)
        val menuGuid = createNameUUID("programMenuDirComponent")
        val programMenuDirComponent = componentBuilder(doc, id = "programMenuDirComponent", guid = menuGuid)
        val startMenuShortcut = shortcutBuilder(
            doc,
            id = "startMenuShortcut",
            directory = "ProgramMenuDir",
            workingDirectory = "INSTALLDIR",
            name = shortcutName,
            target = "[INSTALLDIR]${project.name}.exe",
            icon = if(iconPath.isNotEmpty()) "icon.ico" else ""
        )
        val uninstallShortcut = shortcutBuilder(
            doc,
            id = "uninstallShortcut",
            name = getUninstallShortcutName(shortcutName, cultures),
            directory = "ProgramMenuDir",
            target = "[System64Folder]msiexec.exe",
            arguments = "/x [ProductCode]"
        )
        val removeFolder = removeFolderBuilder(doc, id = "CleanUpShortCut", directory = "ProgramMenuDir")
        val pRegistryValue = registryBuilder(doc, id = "ProgramMenuShortcutReg", productCode = "[ProductCode]", manufacturer = manufacturer)

        programMenuFolderElement.appendChild(programeMenuDir)
        programeMenuDir.appendChild(programMenuDirComponent)
        programMenuDirComponent.appendChild(startMenuShortcut)
        programMenuDirComponent.appendChild(uninstallShortcut)
        programMenuDirComponent.appendChild(removeFolder)
        programMenuDirComponent.appendChild(pRegistryValue)

        val removeShortcutComponent = componentBuilder(doc, id = "RemoveShortcutComponent", guid = createNameUUID("RemoveShortcutComponent"))
        val removeMenuShortcut = doc.createElement("RemoveFile").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "RemoveMenuShortcut" })
            setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
            setAttributeNode(doc.createAttribute("Name").also { it.value = "*.lnk" })
            setAttributeNode(doc.createAttribute("Directory").also { it.value = "ProgramMenuDir" })
        }
        val removeMenuShortcutReg = registryBuilder(doc, id = "RemoveMenuShortcutReg", productCode = "[ProductCode]", manufacturer = manufacturer)
        removeShortcutComponent.appendChild(removeMenuShortcut)
        removeShortcutComponent.appendChild(removeMenuShortcutReg)

        targetDirectory.appendChild(programMenuFolderElement)
        targetDirectory.appendChild(removeShortcutComponent)

        // 设置所有组件的架构为 64 位
        val components = doc.documentElement.getElementsByTagName("Component")
        for (i in 0 until components.length) {
            val component = components.item(i) as Element
            val win64 = doc.createAttribute("Win64")
            win64.value = "yes"
            component.setAttributeNode(win64)
        }

        // 添加 ProgramFiles64Folder 节点
        val programFilesElement = doc.createElement("Directory")
        val idAttr = doc.createAttribute("Id")
        idAttr.value = "ProgramFiles64Folder"
        programFilesElement.setAttributeNode(idAttr)
        targetDirectory.appendChild(programFilesElement)
        val installDir = targetDirectory.getElementsByTagName("Directory").item(0)
        val removedNode = targetDirectory.removeChild(installDir)
        programFilesElement.appendChild(removedNode)
        val installDirElement = programFilesElement.getElementsByTagName("Directory").item(0) as Element
        installDirElement.setAttribute("Id", "INSTALLDIR")

        // 设置 Feature 节点
        val featureElement = doc.getElementsByTagName("Feature").item(0) as Element
        featureElement.setAttribute("Id", "Complete")
        featureElement.setAttribute("Title", project.name)

        // 设置 UI
        doc.createElement("Property").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "WIXUI_INSTALLDIR" })
            setAttributeNode(doc.createAttribute("Value").also { it.value = "INSTALLDIR" })
        }.also { productElement.appendChild(it) }

        val uiElement = doc.createElement("UI")
        productElement.appendChild(uiElement)
        doc.createElement("UIRef").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUI_InstallDir" })
        }.also { uiElement.appendChild(it) }

        if (licensePath.isEmpty()) {
            doc.createElement("Publish").apply {
                setAttributeNode(doc.createAttribute("Dialog").also { it.value = "WelcomeDlg" })
                setAttributeNode(doc.createAttribute("Control").also { it.value = "Next" })
                setAttributeNode(doc.createAttribute("Event").also { it.value = "NewDialog" })
                setAttributeNode(doc.createAttribute("Value").also { it.value = "InstallDirDlg" })
                setAttributeNode(doc.createAttribute("Order").also { it.value = "2" })
                appendChild(doc.createTextNode("1"))
            }.also { uiElement.appendChild(it) }

            doc.createElement("Publish").apply {
                setAttributeNode(doc.createAttribute("Dialog").also { it.value = "InstallDirDlg" })
                setAttributeNode(doc.createAttribute("Control").also { it.value = "Back" })
                setAttributeNode(doc.createAttribute("Event").also { it.value = "NewDialog" })
                setAttributeNode(doc.createAttribute("Value").also { it.value = "WelcomeDlg" })
                setAttributeNode(doc.createAttribute("Order").also { it.value = "2" })
                appendChild(doc.createTextNode("1"))
            }.also { uiElement.appendChild(it) }
        }

        // 添加 UIRef Id="WixUI_ErrorProgressText"
        doc.createElement("UIRef").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUI_ErrorProgressText" })
        }.also { productElement.appendChild(it) }

        // 添加 Icon
        if (iconPath.isNotEmpty()) {
            doc.createElement("Icon").apply {
                setAttributeNode(doc.createAttribute("Id").also { it.value = "icon.ico" })
                setAttributeNode(doc.createAttribute("SourceFile").also { it.value = iconPath })
            }.also { productElement.appendChild(it) }

            doc.createElement("Property").apply {
                setAttributeNode(doc.createAttribute("Id").also { it.value = "ARPPRODUCTICON" })
                setAttributeNode(doc.createAttribute("Value").also { it.value = "icon.ico" })
            }.also { productElement.appendChild(it) }
        }

        // 设置 license file
        if (licensePath.isNotEmpty()) {
            doc.createElement("WixVariable").apply {
                setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUILicenseRtf" })
                setAttributeNode(doc.createAttribute("Value").also { it.value = licensePath })
            }.also { productElement.appendChild(it) }
        }

        // MajorUpgrade
        doc.createElement("MajorUpgrade").apply {
            val errorMessage = getDowngradeErrorMessage(cultures)
            setAttributeNode(doc.createAttribute("AllowSameVersionUpgrades").also { it.value = "yes" })
            setAttributeNode(doc.createAttribute("DowngradeErrorMessage").also { it.value = errorMessage })
        }.also { productElement.appendChild(it) }

        // 设置 fragment 节点
        val fragmentElement = doc.getElementsByTagName("Fragment").item(0) as Element
        val componentGroup = fragmentElement.getElementsByTagName("ComponentGroup").item(0) as Element
        val programMenuDirRef = componentRefBuilder(doc, "programMenuDirComponent")
        val desktopShortcuRef = componentRefBuilder(doc, "DesktopShortcutComponent")
        val removeShortcutRef = componentRefBuilder(doc, "RemoveShortcutComponent")
        componentGroup.appendChild(desktopShortcuRef)
        componentGroup.appendChild(programMenuDirRef)
        componentGroup.appendChild(removeShortcutRef)

        generateXml(doc, wixFile)
    }

    private fun generateXml(doc: Document, file: File) {
        val transformerFactory = TransformerFactory.newInstance()
        transformerFactory.setAttribute("indent-number", 4)
        val transformer = transformerFactory.newTransformer()

        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

        val source = DOMSource(doc)
        val result = StreamResult(file)
        transformer.transform(source, result)
    }

    private fun directoryBuilder(doc: Document, id: String, name: String = ""): Element {
        val directory = doc.createElement("Directory")
        val attrId = doc.createAttribute("Id")
        attrId.value = id
        directory.setAttributeNode(attrId)
        if (name.isNotEmpty()) {
            val attrName = doc.createAttribute("Name")
            attrName.value = name
            directory.setAttributeNode(attrName)
        }
        return directory
    }

    private fun componentBuilder(doc: Document, id: String, guid: String): Element {
        val component = doc.createElement("Component")
        val scAttrId = doc.createAttribute("Id")
        scAttrId.value = id
        component.setAttributeNode(scAttrId)
        val scGuid = doc.createAttribute("Guid")
        scGuid.value = guid
        component.setAttributeNode(scGuid)
        return component
    }

    private fun registryBuilder(doc: Document, id: String, productCode: String, manufacturer: String): Element {
        val regComponentElement = doc.createElement("RegistryValue")
        val regAttrId = doc.createAttribute("Id")
        regAttrId.value = id
        val regAttrRoot = doc.createAttribute("Root")
        regAttrRoot.value = "HKCU"
        val regKey = doc.createAttribute("Key")
        regKey.value = "Software\\${manufacturer}\\${project.name}"
        val regType = doc.createAttribute("Type")
        regType.value = "string"
        val regName = doc.createAttribute("Name")
        regName.value = "ProductCode"
        val regValue = doc.createAttribute("Value")
        regValue.value = productCode
        val regKeyPath = doc.createAttribute("KeyPath")
        regKeyPath.value = "yes"
        regComponentElement.setAttributeNode(regAttrId)
        regComponentElement.setAttributeNode(regAttrRoot)
        regComponentElement.setAttributeNode(regKey)
        regComponentElement.setAttributeNode(regType)
        regComponentElement.setAttributeNode(regName)
        regComponentElement.setAttributeNode(regValue)
        regComponentElement.setAttributeNode(regKeyPath)
        return regComponentElement
    }

    private fun shortcutBuilder(
        doc: Document,
        id: String,
        directory: String = "",
        workingDirectory: String = "",
        name: String,
        target: String = "",
        description: String = "",
        arguments: String = "",
        icon: String = ""
    ): Element {
        val shortcut = doc.createElement("Shortcut")
        val shortcutId = doc.createAttribute("Id")
        shortcutId.value = id
        val shortcutName = doc.createAttribute("Name")
        shortcutName.value = name
        val advertise = doc.createAttribute("Advertise")
        advertise.value = "no"

        shortcut.setAttributeNode(shortcutId)
        shortcut.setAttributeNode(shortcutName)
        shortcut.setAttributeNode(advertise)

        if (target.isNotEmpty()) {
            val shortcutTarget = doc.createAttribute("Target")
            shortcutTarget.value = target
            shortcut.setAttributeNode(shortcutTarget)
        }

        if (directory.isNotEmpty()) {
            val shortcutDir = doc.createAttribute("Directory")
            shortcutDir.value = directory
            shortcut.setAttributeNode(shortcutDir)
        }

        if (workingDirectory.isNotEmpty()) {
            val shortcutWorkDir = doc.createAttribute("WorkingDirectory")
            shortcutWorkDir.value = workingDirectory
            shortcut.setAttributeNode(shortcutWorkDir)
        }
        if (description.isNotEmpty()) {
            val shortcutDescription = doc.createAttribute("Description")
            shortcutDescription.value = description
            shortcut.setAttributeNode(shortcutDescription)
        }

        if (arguments.isNotEmpty()) {
            val shortcutArguments = doc.createAttribute("Arguments")
            shortcutArguments.value = arguments
            shortcut.setAttributeNode(shortcutArguments)
        }
        if (icon.isNotEmpty()) {
            val shortcutIcon = doc.createAttribute("Icon")
            shortcutIcon.value = icon
            shortcut.setAttributeNode(shortcutIcon)
        }

        return shortcut
    }

    private fun removeFolderBuilder(doc: Document, id: String, directory: String): Element {
        val removeFolder = doc.createElement("RemoveFolder").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = id })
            setAttributeNode(doc.createAttribute("Directory").also { it.value = directory })
            setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
        }
        return removeFolder
    }

    private fun componentRefBuilder(doc: Document, id: String): Element {
        val componentRef = doc.createElement("ComponentRef")
        val attrId = doc.createAttribute("Id")
        attrId.value = id
        componentRef.setAttributeNode(attrId)
        return componentRef
    }

    private fun createNameUUID(str: String): String {
        return "{" + UUID.nameUUIDFromBytes(str.toByteArray(StandardCharsets.UTF_8)).toString().uppercase() + "}"
    }

    private fun getLanguageCode(cultures: String): String {
        // 获取第一个文化代码作为主要语言
        val primaryCulture = cultures.split(",").firstOrNull() ?: "zh-CN"

        // 将文化代码转换为语言ID
        return when (primaryCulture.lowercase()) {
            "zh-cn", "zh-hans" -> "2052"  // 简体中文
            "zh-tw", "zh-hant" -> "1028"  // 繁体中文
            "en-us" -> "1033"  // 英语(美国)
            "en-gb" -> "2057"  // 英语(英国)
            "ja-jp" -> "1041"  // 日语
            "ko-kr" -> "1042"  // 韩语
            "fr-fr" -> "1036"  // 法语
            "de-de" -> "1031"  // 德语
            "es-es" -> "1034"  // 西班牙语
            "ru-ru" -> "1049"  // 俄语
            else -> "1033"  // 默认英语
        }
    }

    private fun getCodePage(cultures: String): String {
        // 获取第一个文化代码作为主要语言
        val primaryCulture = cultures.split(",").firstOrNull() ?: "zh-CN"

        // 将文化代码转换为代码页
        return when (primaryCulture.lowercase()) {
            "zh-cn", "zh-hans" -> "936"   // 简体中文 GB2312
            "zh-tw", "zh-hant" -> "950"   // 繁体中文 Big5
            "ja-jp" -> "932"   // 日文 Shift-JIS
            "ko-kr" -> "949"   // 韩文
            "ru-ru" -> "1251"  // 西里尔文
            "ar-sa", "he-il" -> "1256"  // 阿拉伯文/希伯来文
            "th-th" -> "874"   // 泰文
            "vi-vn" -> "1258"  // 越南文
            else -> "1252"  // 默认西欧语言 (英语、法语、德语、西班牙语等)
        }
    }

    private fun getUninstallShortcutName(shortcutName: String, cultures: String): String {
        // 获取第一个文化代码作为主要语言
        val primaryCulture = cultures.split(",").firstOrNull() ?: "zh-CN"

        // 根据语言环境生成卸载快捷方式名称
        return when (primaryCulture.lowercase()) {
            "zh-cn", "zh-hans", "zh-tw", "zh-hant" -> "卸载$shortcutName"  // 中文
            "ja-jp" -> "アンインストール$shortcutName"  // 日语
            "ko-kr" -> "제거$shortcutName"  // 韩语
            "fr-fr" -> "Désinstaller $shortcutName"  // 法语
            "de-de" -> "Deinstallieren $shortcutName"  // 德语
            "es-es" -> "Desinstalar $shortcutName"  // 西班牙语
            "ru-ru" -> "Удалить $shortcutName"  // 俄语
            else -> "Uninstall $shortcutName"  // 默认英语
        }
    }

    private fun getDowngradeErrorMessage(cultures: String): String {
        // 获取第一个文化代码作为主要语言
        val primaryCulture = cultures.split(",").firstOrNull() ?: "zh-CN"

        // 根据语言环境生成降级错误消息
        return when (primaryCulture.lowercase()) {
            "zh-cn", "zh-hans", "zh-tw", "zh-hant" -> "A newer version of [ProductName] is already installed. If you want to install an older version, please uninstall the newer version first."  // 中文
            "ja-jp" -> "新しいバージョンの[ProductName]が既にインストールされています。古いバージョンをインストールする場合は、まず新しいバージョンをアンインストールしてください。"  // 日语
            "ko-kr" -> "[ProductName]의 최신 버전이 이미 설치되어 있습니다. 이전 버전을 설치하려면 먼저 최신 버전을 제거하십시오."  // 韩语
            "fr-fr" -> "Une version plus récente de [ProductName] est déjà installée. Si vous souhaitez installer une version antérieure, veuillez d'abord désinstaller la version plus récente."  // 法语
            "de-de" -> "Eine neuere Version von [ProductName] ist bereits installiert. Wenn Sie eine ältere Version installieren möchten, deinstallieren Sie bitte zuerst die neuere Version."  // 德语
            "es-es" -> "Ya está instalada una versión más reciente de [ProductName]. Si desea instalar una versión anterior, desinstale primero la versión más reciente."  // 西班牙语
            "ru-ru" -> "Более новая версия [ProductName] уже установлена. Если вы хотите установить более старую версию, сначала удалите более новую версию."  // 俄语
            else -> "A newer version of [ProductName] is already installed. If you want to install an older version, please uninstall the newer version first."  // 默认英语
        }
    }
}
