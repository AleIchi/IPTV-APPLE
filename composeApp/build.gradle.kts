plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Ktor Networking
            implementation("io.ktor:ktor-client-core:2.3.6")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
            
            // Coil 3
            implementation("io.coil-kt.coil3:coil-compose:3.0.0-alpha06")
            implementation("io.coil-kt.coil3:coil-network-ktor:3.0.0-alpha06")
            
            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            
            // DataStore
            implementation("androidx.datastore:datastore-preferences-core:1.1.0")
            
            // Room
            implementation("androidx.room:room-runtime:2.7.0")
            
            // Navigation
            implementation("org.jetbrains.androidx.navigation:navigation-compose:2.7.0-alpha06")
        }
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation("androidx.activity:activity-compose:1.8.1")
            implementation("androidx.appcompat:appcompat:1.6.1")
            
            // Ktor Android
            implementation("io.ktor:ktor-client-okhttp:2.3.6")
            
            // ExoPlayer (Android Only)
            implementation("androidx.media3:media3-exoplayer:1.2.0")
            implementation("androidx.media3:media3-ui:1.2.0")
            implementation("androidx.media3:media3-common:1.2.0")
            implementation("androidx.media3:media3-exoplayer-dash:1.2.0")
            implementation("androidx.media3:media3-exoplayer-hls:1.2.0")
            implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.2.0+1")
            
            // Room Android
            implementation("androidx.room:room-ktx:2.7.0")
            
            // Coil Android
            implementation("io.coil-kt:coil:2.5.0")
            
            // ZXing
            implementation("com.google.zxing:core:3.5.3")
        }
        
        iosMain.dependencies {
            // Ktor iOS
            implementation("io.ktor:ktor-client-darwin:2.3.6")
        }
    }
}

android {
    namespace = "com.iptv.fiber"
    compileSdk = 34

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.iptv.fiber"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
