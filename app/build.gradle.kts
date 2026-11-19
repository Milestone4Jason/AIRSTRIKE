plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.andre.airstrike"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.andre.airstrike"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
