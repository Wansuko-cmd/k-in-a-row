package plugins.ext

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun DependencyHandlerScope.implementation(dependencyNotation: Any) =
    add("implementation", dependencyNotation)

fun DependencyHandlerScope.testImplementation(dependencyNotation: Any) =
    add("testImplementation", dependencyNotation)

fun Project.kotlinAndroid(configure: Action<KotlinAndroidExtension>): Unit =
    (this as ExtensionAware).extensions.configure("kotlin", configure)

fun Project.kotlinMultiplatform(configure: Action<KotlinMultiplatformExtension>): Unit =
    (this as ExtensionAware).extensions.configure("kotlin", configure)
