plugins {
    alias(libs.plugins.androidApp)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // app/google-services.json is now in place (package reelsdrama.freedrama.videosdrama
    // confirmed) — plugin active, generates FirebaseApp's default options from it at build time.
    alias(libs.plugins.googleServices)
}

android {
    namespace = "reelsdrama.freedrama.videosdrama"
    compileSdk = 36

    defaultConfig {
        applicationId = "reelsdrama.freedrama.videosdrama"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

hilt {
    enableAggregatingTask = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.concurrent.futures)

    // GMA Next-Gen SDK (AdMob) - foundation layer only, no ad-loading logic yet.
    implementation(libs.ads.mobile.sdk)

    // OneSignal push notifications. Registers for FCM itself internally - deliberately does NOT
    // need (or want) the Google Services plugin/google-services.json on its own account, per
    // OneSignal's own Android integration guide's Pre-Flight Checklist (this project already has
    // both for Firebase Realtime Database above, which is unrelated and unaffected).
    implementation(libs.onesignal)

    // Firebase Realtime Database only — remotely controls the reels feed's every-3-reels ad-slot
    // type (see AdConfigRepository). Not Remote Config; deliberately firebase-database only, no
    // other Firebase product (Firebase removed the -ktx artifacts from the BOM in v34.0.0 — this
    // base artifact now includes the Kotlin/Flow extensions that -ktx used to provide). Requires
    // app/google-services.json + the google-services plugin (see this file's plugins block)
    // before FirebaseDatabase actually works at runtime.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
