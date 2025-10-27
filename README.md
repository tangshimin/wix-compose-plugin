# WiX Compose Desktop Plugin

一个 Gradle 插件，用于使用 WiX 工具集打包 Compose Desktop 应用程序，生成专业的 Windows MSI 安装包。

## 特性

- 🚀 **自动依赖管理** - 自动在 `createDistributable` 任务之后执行 WiX 打包流程
- 🎯 **专业安装包** - 生成符合 Windows 标准的 MSI 安装包
- 🔧 **高度可配置** - 支持自定义厂商信息、快捷方式、许可证文件等
- 🛡️ **安全卸载** - 避免误删文件的安全卸载机制
- 📦 **完整任务链** - 提供完整的打包流程，从应用镜像到最终安装包

## 解决的问题

Compose Desktop 默认的打包插件存在以下问题：

1. **快捷方式名称限制** - 无法设置独立的快捷方式名称，某些 Windows 用户安装目录不能有中文字符，但需要中文快捷方式
2. **安全卸载问题** - 卸载时可能误删文件，如果安装到错误目录会删除整个父目录
3. **自定义程度低** - 缺乏专业的安装包定制选项

本插件通过 WiX 工具集解决了这些问题，提供更专业和安全的打包体验。

## 安装

### 方法一：手动构建插件（当前推荐）

由于插件尚未发布到 Maven Central，您需要先手动构建插件：

1. **克隆并构建插件**

    ```bash
    git clone https://github.com/tangshimin/wix-compose-plugin.git
    cd wix-compose-plugin
    ./gradlew publishToMavenLocal
    ```
2. **在项目中使用插件**

    在项目的 `build.gradle.kts` 中添加：
    
    ```kotlin
    plugins {
        id("io.github.tangshimin.wix-compose") version "1.0.0"
    }
    ```


### 配置插件

在 `build.gradle.kts` 中添加配置：

```kotlin
wixCompose {
    manufacturer.set("你的公司名")
    shortcutName.set("应用程序名称")
    licenseFile.set("license.rtf")  // 可选，许可证文件路径
    iconFile.set("src/main/resources/logo/logo.ico")  // 可选，图标文件路径
}
```

## 使用方法

### 完整打包流程

执行以下命令生成 MSI 安装包：

```bash
./gradlew light
```

这个命令会自动执行完整的打包流程：
1. `createDistributable` - 生成应用镜像
2. `harvest` - 使用 WiX heat 工具收集文件
3. `editWxs` - 编辑 WiX 脚本文件
4. `compileWxs` - 编译 WiX 脚本
5. `light` - 生成最终 MSI 安装包

### 单独执行任务

你也可以单独执行各个任务进行调试：

```bash
# 只生成应用镜像
./gradlew createDistributable

# 只收集文件生成 WXS 文件
./gradlew harvest

# 只编辑 WXS 文件
./gradlew editWxs

# 只编译 WXS 文件
./gradlew compileWxs
```

## 任务说明

### 主要任务

- **`harvest`** - 依赖 `createDistributable` 任务，使用 WiX heat 命令收集应用镜像中的文件，生成 WXS 文件
- **`editWxs`** - 编辑 WXS 文件，填充产品信息，设置快捷方式
- **`compileWxs`** - 编译 WXS 文件，生成 WIXOBJ 文件
- **`light`** - 链接 WIXOBJ 文件，生成最终的 MSI 安装包

### 任务依赖关系

```
createDistributable (compose 插件)
    ↓
harvest (本插件)
    ↓
editWxs (本插件)
    ↓
compileWxs (本插件)
    ↓
light (本插件)
```

## 配置选项

### 必需配置

- `manufacturer` - 厂商名称
- `shortcutName` - 快捷方式显示名称

### 可选配置

- `licenseFile` - 许可证文件路径（RTF 格式）
- `iconFile` - 应用程序图标文件路径（ICO 格式）

## 示例配置

```kotlin
compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += listOf(
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
            "-Dsun.stdout.encoding=UTF-8"
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)
            packageVersion = "1.0.0"
            packageName = "sample"
        }
    }
}

wixCompose {
    manufacturer.set("测试公司")
    shortcutName.set("测试应用")
}
```

## 输出文件

- **MSI 安装包**: `build/wix311/sample-1.0.0.msi`
- **WiX 源文件**: `build/compose/binaries/main/app/sample.wxs`
- **中间文件**: `build/wix311/sample.wixobj`

## 技术原理

本插件基于以下技术栈：

1. **Compose Desktop** - 生成跨平台桌面应用
2. **WiX Toolset** - Windows 安装包制作工具
3. **Gradle Plugin** - 提供自动化构建流程

打包流程：
1. 使用 Compose Desktop 的 `createDistributable` 任务生成应用镜像
2. 使用 WiX heat 工具扫描应用镜像目录，生成 WXS 文件
3. 编辑 WXS 文件，添加产品信息、快捷方式等
4. 使用 WiX candle 工具编译 WXS 文件为 WIXOBJ 文件
5. 使用 WiX light 工具链接 WIXOBJ 文件，生成 MSI 安装包

## 系统要求

- **操作系统**: Windows（WiX 工具集仅支持 Windows）
- **Gradle**: 8.5+

## 许可证

本项目采用 Apache License 2.0 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request！

## 支持

如果在使用过程中遇到问题，请：
1. 查看本文档
2. 检查 Gradle 构建日志
3. 提交 Issue 并提供详细的重现步骤

---

**注意**: 本插件依赖 WiX 工具集，Compose Desktop 在执行 `createDistributable` 任务时会自动下载 WiX 到 `build/wix311` 目录。