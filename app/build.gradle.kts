// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.cpatcher"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "io.github.cpatcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        
        // Kotlin compiler optimizations
        freeCompilerArgs += listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
    
    // Source linking cho ReVanced extensions
    sourceSets {
        getByName("main") {
            java {
                srcDirs(
                    "src/main/java",
                    // Link ReVanced extension sources (nếu có submodule)
                    // "../revanced-source/extensions/shared/src/main/java",
                    "../revanced-source/extensions/tiktok/src/main/java"
                )
                // Exclude unnecessary files
                exclude("**/integrations/**")
                exclude("**/test/**")
            }
        }
    }
    
    packaging {
        resources {
            excludes += setOf(
                "**",
                "META-INF/*.version",
                "META-INF/*.kotlin_module",
                "kotlin/**"
            )
        }
    }
    
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Core dependencies
    implementation(libs.dexkit)
    compileOnly(libs.xposed.api)
    
    // Android libraries
    implementation(libs.bundles.android.core)
    
    // JSON processing
    implementation(libs.gson)
    
    // Optional: ReVanced libraries (if using external approach)
    // implementation(libs.bundles.revanced)
    
    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.testing)
}
