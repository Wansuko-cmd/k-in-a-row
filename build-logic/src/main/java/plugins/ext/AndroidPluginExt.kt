package plugins.ext

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

fun Project.configureCommonAndroidSetting(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = buildLogic.getVersion("android.compileSdk").toInt()

        defaultConfig.minSdk = buildLogic.getVersion("android.minSdk").toInt()

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}
