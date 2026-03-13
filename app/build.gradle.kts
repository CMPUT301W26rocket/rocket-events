plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}


android {
    namespace = "com.example.eventlotteryapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.eventlotteryapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Mockito for mocking repositories in UI tests
    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    // RecyclerViewActions for scrolling in RecyclerView tests
    // Exclude accessibility-test-framework: it pulls in protobuf-lite (old artifact)
    // which conflicts with Firebase's protobuf-javalite at runtime (NoSuchMethodError).
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1") {
        exclude(module = "accessibility-test-framework")
    }
    // Intents.intended() for verifying Activity navigation
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    // FragmentScenario for launching fragments in isolation
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
    // Firebase transitively pulls in an older androidx.test:core — force it up so
    // Espresso and FragmentScenario can find classes like DirectExecutor at runtime
    implementation("androidx.test:core:1.6.1")
    // Firebase BoM - manages all versions automatically
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    // Firestore
    implementation("com.google.firebase:firebase-firestore")
    // Firebase Auth (anonymous/device login)
    implementation("com.google.firebase:firebase-auth")
    // Firebase Storage (event poster images)
    implementation("com.google.firebase:firebase-storage")
    // Firebase Cloud Messaging (push notifications)
    implementation("com.google.firebase:firebase-messaging")
    // QR Code scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // QR Code generation
    implementation("com.google.zxing:core:3.5.2")
    // Image loading for event posters
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // Google Maps + Location (geolocation feature)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
}