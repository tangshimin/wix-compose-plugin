plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "io.github.tangshimin"
version = "1.0.0"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

gradlePlugin {
    plugins {
        create("wixCompose") {
            id = "io.github.tangshimin.wix-compose"
            implementationClass = "io.github.tangshimin.wix.WixComposePlugin"
            displayName = "WiX Compose Desktop Plugin"
            description = "A plugin to package Compose Desktop applications using WiX toolset"
        }
    }
}

publishing {
    publications {
        // 使用 java-gradle-plugin 自动生成的 pluginMaven 发布
        // 不需要手动创建 maven 发布，避免重复
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = project.findProperty("ossrhUsername")?.toString()
                password = project.findProperty("ossrhPassword")?.toString()
            }
        }
    }
}

// 配置自动生成的 pluginMaven 发布的 POM
afterEvaluate {
    publishing.publications.named<MavenPublication>("pluginMaven") {
        pom {
            name.set("WiX Compose Desktop Plugin")
            description.set("A Gradle plugin to package Compose Desktop applications using WiX toolset")
            url.set("https://github.com/tangshimin/wix-package")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("tangshimin")
                    name.set("Tang Shimin")
                    email.set("tang_shimin@qq.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/tangshimin/wix-package.git")
                developerConnection.set("scm:git:ssh://github.com/tangshimin/wix-package.git")
                url.set("https://github.com/tangshimin/wix-package")
            }
        }
    }
}

signing {
    // 只在有密钥配置时才签名
    isRequired = project.hasProperty("signing.keyId")
    if (isRequired) {
        sign(publishing.publications["pluginMaven"])
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
