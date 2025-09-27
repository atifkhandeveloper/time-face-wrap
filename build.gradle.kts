// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
//        maven(url = "https://developer.huawei.com/repo/") // Huawei repo
    }
    dependencies {
        classpath("com.huawei.agconnect:agcp:1.9.1.300")
        classpath("com.android.tools.build:gradle:8.1.0") // Make sure this is present
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.22") // Make sure this is present
    }
}
