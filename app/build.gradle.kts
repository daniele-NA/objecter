@file:Suppress("AndroidGradlePluginVersion","NewerVersionAvailable","UnstableApiUsage","UNCHECKED_CAST","UseTomlInstead","GradleDependency")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()  // Call toml property
    kotlin("kapt")
}

android {
    namespace = "com.crescenzi.objecter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.crescenzi.objecter"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild{cmake { abiFilters+=mutableSetOf("armeabi-v7a","arm64-v8a","x86","x86_64") }}
    }

    buildTypes {
        release {
            isShrinkResources = false
            isDebuggable = false
            isMinifyEnabled = true
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
    buildFeatures{
        viewBinding = true
    }

    externalNativeBuild { cmake{path=file("src/main/jni/CMakeLists.txt")} }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)

    implementation("androidx.camera:camera-camera2:1.2.0")
    implementation("androidx.camera:camera-lifecycle:1.2.0")
    implementation("androidx.camera:camera-view:1.0.0-alpha22")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

}