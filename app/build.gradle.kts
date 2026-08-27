import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.appcompat:appcompat:1.7.0")
    }
}

android {
    namespace = "com.example.bustrack_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bustrack_app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }
            val mapboxToken = localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""
            resValue("string", "mapbox_access_token", mapboxToken)
            // Help & Support Chatbot (Task 6) - key lives only in local.properties,
            // never committed/hardcoded. See CHATBOT_SETUP.md for where to put it.
            val chatbotApiKey = localProperties.getProperty("CHATBOT_API_KEY") ?: ""
            buildConfigField("String", "CHATBOT_API_KEY", "\"$chatbotApiKey\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }
            val mapboxToken = localProperties.getProperty("MAPBOX_ACCESS_TOKEN") ?: ""
            resValue("string", "mapbox_access_token", mapboxToken)
            val chatbotApiKey = localProperties.getProperty("CHATBOT_API_KEY") ?: ""
            buildConfigField("String", "CHATBOT_API_KEY", "\"$chatbotApiKey\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.auth)
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.navigation)
    implementation(libs.mapbox.voice)
    implementation(libs.mapbox.search)
    implementation("com.mapbox.search:mapbox-search-android-ui:2.2.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Cloudinary Android SDK - added because MainActivity.kt/MyApp.kt already contain
    // real Cloudinary upload code (MediaManager, UploadCallback, ErrorInfo), but the
    // dependency was never declared, which is the root cause of the 28 build errors.
    // 3.1.2 is the current stable release, supports minSdk 21+ (project minSdk is 24),
    // and its UploadCallback/MediaManager API matches exactly what the existing code
    // already calls, so no code rewrite was needed beyond this dependency line.
    implementation("com.cloudinary:cloudinary-android:3.1.2")
    implementation(libs.androidx.activity)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.ktx)
    implementation(libs.glide)
    annotationProcessor(libs.glide)


    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
