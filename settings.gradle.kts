@file:Suppress("UnstableApiUsage")

pluginManagement {
    enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://androidx.dev/storage/compose-compiler/repository/")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://androidx.dev/storage/compose-compiler/repository/")
    }
    versionCatalogs {
        create("buildLogic") {
            from(files("./gradle/build-logic.versions.toml"))
        }
    }
}

rootProject.name = "KInARow"
include(":androidApp")

include(":shared:lib")
include(":shared:ui")
