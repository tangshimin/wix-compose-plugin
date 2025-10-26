# wix-package
使用 wix 打包 compose-desktop 生成的 application image 生成 msi 安装包。

compose desktop 默认的打包插件有一些问题：
- 不能设置单独快捷方式的名称，有一些 windows 用户的安装目录不能有中文字符，但是又需要把快捷方式设置成中文的。
- 比如卸载软件的时候可能会误删文件，如果操作失误把软件安装到了 `D:\Program Files` 而不是 `D:\Program Files\productName` 卸载的时候会把 `D:\Program Files` 下的所有文件都删除掉。

compose desktop 的打包插件使用的是 jpackage, jpackage 在 windows 系统底层依赖 wix，编写 Wix 脚本可以解决上述问题。
compose desktop 插件在执行 `createDistributable` 任务时会自动下载 wix 的安装包到 `build\wix311` 目录下。

这个脚本的打包思路是：
1. 先使用 compose desktop 的 `createDistributable` 任务生成 app-image
2. 然后使用 wix 的 heat 命令收集 app-image 文件夹里的文件，生成一个 wxs 文件
3. 编辑 wxs 文件,填充一些产品信息，设置快捷方式
4. 编译 wxs 文件,生成 wixobj 文件
5. 链接 wixobj文件 生成 msi 安装包


现在还只是一个脚本，后续可能会封装成一个插件。
## 使用方法
1. 先把`build.gradle.kts`里的 packageName 设置成英文，不设置也可以（因为还要打包 macOS 版），但是需要再写一个 Task 在 `createDistributable` 创建 app-image 之后把文件夹重命名为英语。
2. 把 `wix.gradle.kts` 复制到项目根目录
3. 在 `build.gradle.kts` 添加
    ```kotlin
      apply(from = "wix.gradle.kts")
    ```
4. 设置 `wix.gradle.kts` 中的 manufacturer、shortcutName、licenseFile 和 iconFile 等参数

5. 执行 `light` 任务，就可以生成 msi 安装包了。这个脚本会创建 2 个快捷方式，分别是桌面快捷方式、开始菜单快捷方式。
6. 这段脚本有 4 个任务，分别是 `harvest`、`editWxs`、`compileWxs`、`light`，可以单独执行，方便调试。

## Task 说明
- `harvest` 依赖 `createDistributable` Task 创建的 app-image 文件夹,然后使用 Wix 的 heat 命令收集 app-image 文件夹里的文件，生成一个 wxs 文件
- `editWxs` 编辑 wxs 文件,填充一些产品信息，设置快捷键
- `compileWxs` 编译 wxs 文件,生成 wixobj 文件
- `light` 链接 wixobj文件 生成 msi 安装包
