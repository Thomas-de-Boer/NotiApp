plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// google-services.json is geheim en staat niet in git. Zonder dat bestand bouwt de app
// nog wel, maar werken de meldingen niet (zie README).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("LET OP: app/google-services.json ontbreekt - meldingen werken niet. Zie README.")
}

android {
    namespace = "com.notiapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.notiapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.1"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            // Privé gezinsapp: met de debug-sleutel ondertekenen zodat je de APK direct kunt installeren.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.coroutines.play.services)
    implementation(libs.google.auth)
    debugImplementation(libs.compose.ui.tooling)
}
