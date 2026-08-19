plugins {
    alias(libs.plugins.androidApp) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Declared here (classpath only, `apply false`) so it's ready to switch on in app/build.gradle.kts
    // the moment app/google-services.json is added — see the comment there for why it isn't applied yet.
    alias(libs.plugins.googleServices) apply false
}