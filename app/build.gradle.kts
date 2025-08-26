import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.refine)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = if (keystorePropertiesFile.exists() && keystorePropertiesFile.isFile) {
    Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }
} else null

fun String.execute(): String =
    Runtime.getRuntime().exec(split("\\s".toRegex()).toTypedArray())
        .let { proc ->
            proc.waitFor()
            val result = proc.inputStream.use {
                it.readBytes()
            }.toString(StandardCharsets.UTF_8).trim()
            proc.destroy()
            result
        }


val gitCommitCount = "git rev-list HEAD --count".execute().toInt()
val gitCommitHash = "git rev-parse --verify --short HEAD".execute()

android {
    compileSdk = 36
    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.cpatcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "$gitCommitCount-$gitCommitHash"

        base.archivesName = "Cpatcher-$versionName"
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += "arm64-v8a"
        }
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                ("proguard-rules.pro")
            )
            val releaseSig = signingConfigs.findByName("release")
            signingConfig = if (releaseSig != null) releaseSig else {
                println("use debug signing config")
                signingConfigs["debug"]
            }
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    androidResources.additionalParameters += listOf(
        "--allow-reserved-package-id",
        "--package-id",
        "0x68"
    )
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    namespace = "io.github.cpatcher"
    packaging {
        resources {
            excludes += "**"
        }
    }
    // https://stackoverflow.com/a/77745844
    tasks.withType<PackageAndroidArtifact> {
        doFirst { appMetadata.asFile.orNull?.writeText("") }
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.xposed.api)
    implementation(libs.dexkit)
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.dev.rikka.hidden.compat)
    compileOnly(libs.dev.rikka.hidden.stub)
}
