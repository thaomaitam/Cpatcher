// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    // Source linking cho ReVanced extensions
    sourceSets {
        getByName("main") {
            java {
                srcDirs(
                    "src/main/java",
                    // Link ReVanced extension sources
                    "../revanced-source/extensions/shared/src/main/java",
                    "../revanced-source/extensions/youtube/src/main/java", 
                    "../revanced-source/extensions/spotify/src/main/java"
                )
                // Exclude unnecessary files
                exclude("**/integrations/**")
                exclude("**/test/**")
            }
        }
    }
}

dependencies {
    implementation("org.luckypray:dexkit:2.0.6")
    compileOnly("de.robv.android.xposed:api:82") 
    compileOnly("androidx.annotation:annotation:1.9.1")
}