package plugins

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import plugins.ext.alias
import plugins.ext.configureCommonAndroidSetting
import plugins.ext.getBundle
import plugins.ext.getLibrary
import plugins.ext.getPlugin
import plugins.ext.implementation
import plugins.ext.kotlinAndroid
import plugins.ext.libs

class AndroidComposeLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                alias(libs.getPlugin("android.library"))
                alias(libs.getPlugin("compose.compiler"))

                alias(libs.getPlugin("ktlint"))
            }

            extensions.configure<LibraryExtension> {
                configureCommonAndroidSetting(this)
                buildFeatures {
                    compose = true
                }

                kotlinAndroid {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_11)
                    }
                }
            }

            dependencies {
                implementation(platform(libs.getLibrary("androidx.compose.bom")))
                implementation(libs.getBundle("androidx.compose"))
            }
        }
    }
}
