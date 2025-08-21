// settings.gradle.kts
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // DexKit repository
        maven {
            url = uri("https://maven.pkg.github.com/LuckyPray/DexKit")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull 
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull 
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
        
        // JitPack for GitHub dependencies
        maven { url = uri("https://jitpack.io") }
        
        // Local Maven
        mavenLocal()
    }
    
    // Enable version catalogs
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Cpatcher"
include(":app")

// Gradle optimizations
enableFeaturePreview("CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")