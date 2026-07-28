package plugins.ext

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency

val Project.libs: VersionCatalog
    get() = this.extensions.getByType<VersionCatalogsExtension>().named("libs")

val Project.buildLogic: VersionCatalog
    get() = this.extensions.getByType<VersionCatalogsExtension>().named("buildLogic")

fun VersionCatalog.getVersion(alias: String) = findVersion(alias).get().toString()

fun VersionCatalog.getLibrary(alias: String) = findLibrary(alias).get()

fun VersionCatalog.getBundle(alias: String) = findBundle(alias).get()

fun VersionCatalog.getPlugin(alias: String) = findPlugin(alias).get()

fun PluginManager.alias(plugin: Provider<PluginDependency>) = apply(plugin.get().pluginId)
