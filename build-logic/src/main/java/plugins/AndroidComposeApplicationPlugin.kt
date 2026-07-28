package plugins

import com.android.build.api.dsl.ApplicationExtension
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import plugins.ext.alias
import plugins.ext.buildLogic
import plugins.ext.configureCommonAndroidSetting
import plugins.ext.getBundle
import plugins.ext.getLibrary
import plugins.ext.getPlugin
import plugins.ext.getVersion
import plugins.ext.implementation
import plugins.ext.kotlinAndroid
import plugins.ext.libs
import plugins.ext.testImplementation

class AndroidComposeApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                alias(libs.getPlugin("android.application"))
                alias(libs.getPlugin("compose.compiler"))
                alias(libs.getPlugin("roborazzi"))

                alias(libs.getPlugin("ktlint"))
            }

            extensions.configure<ApplicationExtension> {
                configureCommonAndroidSetting(this)
                defaultConfig {
                    targetSdk = buildLogic.getVersion("android.targetSdk").toInt()
                }
                buildFeatures {
                    compose = true
                }

                kotlinAndroid {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_11)
                    }
                }

                testOptions {
                    unitTests {
                        isIncludeAndroidResources = true
                        all {
                            it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
                        }
                    }
                }
            }

            roborazzi {
                generateComposePreviewRobolectricTests {
                    enable.set(true)
                    packages.set(listOf("com.wsr.k.a.row"))
                    includePrivatePreviews.set(true)
                }
            }

            dependencies {
                implementation(platform(libs.getLibrary("androidx.compose.bom")))
                implementation(libs.getBundle("androidx.compose"))

                testImplementation(libs.getBundle("vrt"))
            }
        }
    }
}

private fun Project.roborazzi(configure: Action<RoborazziExtension>): Unit =
    (this as ExtensionAware).extensions.configure("roborazzi", configure)
