@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("buildLogic") {
            from(files("../gradle/build-logic.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
