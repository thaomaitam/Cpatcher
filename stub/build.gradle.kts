plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.cpatcher.stub"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets  {
        getByName("main") {
            java {
                srcDirs(
                    "../revanced-patches/patches/stub/src/main/java",
                    "../revanced-patches/extensions/youtube/stub/src/main/java",
                    "../revanced-patches/extensions/spotify/stub/src/main/java"
                )
            }
        }
    }
}

dependencies {
}