plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
//    alias(libs.plugins.agconnect) // Use alias instead of id()
}

apply(plugin = "com.huawei.agconnect")
android {
    namespace = "com.myapps.timewrap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.myapps.timewrap"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.2"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        dataBinding = true
        // viewBinding = true // if you also want viewBinding
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}




dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.annotation:annotation:1.3.0")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.4.0")
    implementation("androidx.work:work-runtime-ktx:2.7.0")


    implementation("com.github.bumptech.glide:glide:4.13.0")
    implementation("com.airbnb.android:lottie:3.4.0")
    implementation("com.facebook.shimmer:shimmer:0.1.0")
//    implementation("com.wang.avi:library:2.1.3")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("pl.droidsonroids.gif:android-gif-drawable:1.2.23")

    implementation ("androidx.lifecycle:lifecycle-extensions:2.0.0")
    implementation ("androidx.lifecycle:lifecycle-runtime:2.0.0")
    annotationProcessor ("androidx.lifecycle:lifecycle-compiler:2.0.0")

    implementation("androidx.camera:camera-view:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")

    implementation("io.reactivex.rxjava2:rxjava:2.0.1")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.1")
    implementation("io.github.ParkSangGwon:tedpermission-normal:3.3.0")
    implementation("com.unity3d.ads:unity-ads:4.12.0")
    implementation("com.huawei.agconnect:agconnect-core:1.9.1.302")
}