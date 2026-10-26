// Modified by lanxize in 2026-10. See CHANGES.md for details.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.juliencardoux.openwol"
    compileSdk = 35
    // Pinned explicitly: AGP 8.6 would otherwise default to build-tools 34.0.0, which
    // no longer matches the compile SDK, and it refuses to build when the requested
    // revision is not installed. Must stay in sync with ANDROID_BUILD_TOOLS in .gitlab-ci.yml.
    buildToolsVersion = "35.0.0"

    defaultConfig {
        // Fork-specific. The upstream identity is com.juliencardoux.openwol; reusing it
        // would collide with the official build (signature mismatch on update, forcing a
        // reinstall) and would borrow a namespace this fork does not own.
        // `namespace` above is deliberately left alone: it only names the generated R
        // class and is invisible to users, so changing it would churn every source file.
        applicationId = "io.github.lanxize.openwol"
        minSdk = 21
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation(libs.androidx.material.icons.extended)
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.11.0")
}