import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.2.0"

    // jetbrainsCompose
    id("org.jetbrains.compose") version "1.9.0"
    // compose-compiler
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
    //
    id("io.github.tangshimin.wix-compose") version "1.0.0"
    // task-tree
    id("com.dorongold.task-tree") version "2.1.1"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenLocal()
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += listOf(
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
            "-Dsun.stdout.encoding=UTF-8",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageVersion = "1.0.0"
            packageName = "sample"
        }
    }
}



wixCompose {
    manufacturer.set("测试公司")
    shortcutName.set("测试应用")
}
