import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(buildLogic.plugins.android.compose.application)
}

configure<ApplicationExtension> {
    namespace = "com.wsr.k.a.row.android.app"

    defaultConfig {
        applicationId = "com.wsr.k.a.row"
        versionCode = 1
        versionName = "0.01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles.add(getDefaultProguardFile("proguard-android-optimize.txt"))
            proguardFiles.add(file("proguard-rules.pro"))
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(projects.shared.ui)

    implementation(libs.bundles.androidx)
}
