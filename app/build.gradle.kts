plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.vignesh.cycleshop"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.vignesh.cycleshop"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "dev.vignesh.cycleshop.IsolatedTestRunner"
    }

    buildTypes {
        create("isolated") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".isolated"
            matchingFallbacks += "debug"
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    // Connected tests may uninstall their target. Give that target its own data sandbox.
    testBuildType = "isolated"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
        getByName("isolated").assets.directories.add("$projectDir/src/test/resources")
    }
}

// The isolated app runs local tests without the business app's Firebase project.
tasks.matching { it.name == "processIsolatedGoogleServices" }.configureEach {
    enabled = false
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.firebase.ai)
    implementation(libs.gson)

    // Room 2.8.5 persistence & KSP annotation processor
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)
    
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    add("isolatedImplementation", libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ── Phase 3: CameraX ──────────────────────────────────────────────────────
    // camera-core: UseCase base (ImageCapture, Preview, etc.)
    // camera-camera2: Camera2 implementation; required by lifecycle bridge.
    // camera-lifecycle: ProcessCameraProvider that ties CameraX to LifecycleOwner.
    // camera-view: PreviewView Compose interop surface.
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ── Phase 3: ML Kit Text Recognition v2 ──────────────────────────────────
    // Bundles the Latin-script OCR model in the APK. No network, no paid API.
    implementation(libs.mlkit.text.recognition)

    // ── Phase 3: Coil ─────────────────────────────────────────────────────────
    // Lightweight Compose image loader for in-app file URIs (invoice preview).
    implementation(libs.coil.compose)
}
