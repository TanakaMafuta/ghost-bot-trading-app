plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.ghostbot.trading"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ghostbot.trading"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Build config fields for API configuration
        buildConfigField("String", "DERIV_APP_ID", '"1089"') // Test app ID
        buildConfigField("String", "DERIV_WS_URL", '"wss://ws.derivws.com/websockets/v3"')
        buildConfigField("String", "OPENAI_BASE_URL", '"https://api.openai.com/v1/"')
        buildConfigField("String", "PERPLEXITY_BASE_URL", '"https://api.perplexity.ai/"')
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.compose.material3.window.size)
    
    // Hilt for DI
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Networking
    implementation(libs.bundles.networking)
    implementation(libs.bundles.websocket)
    ksp(libs.moshi.codegen)

    // Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    // AI/ML
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    // Database
    implementation(libs.bundles.room)
    implementation(libs.androidx.datastore)
    ksp(libs.androidx.room.compiler)

    // Utilities
    implementation(libs.timber)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    // Debug tools
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// KSP configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}