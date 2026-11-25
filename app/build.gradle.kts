import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.lenticualrstone.civilight"
    compileSdk = 37
    defaultConfig {
        applicationId = "io.github.lenticualrstone.civilight"
        minSdk = 29
        targetSdk = 37
        versionCode = 10
        versionName = "0.3.3(Antigravity)"
    }
    // Release signing is read from outside the repo: $CIVILIGHT_SIGNING or ~/.civilight-signing/signing.properties
    val signingFile = file(System.getenv("CIVILIGHT_SIGNING") ?: "${System.getProperty("user.home")}/.civilight-signing/signing.properties")
    if (signingFile.exists()) {
        val sp = Properties().apply { signingFile.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                storeFile = file(sp.getProperty("storeFile"))
                storePassword = sp.getProperty("storePassword")
                keyAlias = sp.getProperty("keyAlias")
                keyPassword = sp.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":domain:actions"))
    implementation(project(":feature:library"))
    implementation(project(":feature:story"))
    implementation(project(":feature:script"))
    implementation(project(":feature:cast"))
    implementation(project(":feature:world"))
    implementation(project(":feature:map"))
    implementation(project(":feature:canvas"))
    implementation(project(":feature:recorder"))
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.navigation3.ui)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.androidx.window)
    testImplementation(libs.junit)
}
