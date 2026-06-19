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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
<<<<<<< HEAD
    }
}

rootProject.name = "Bustrack"
=======

        val localProperties = java.util.Properties()
        val localPropertiesFile = File(rootProject.projectDir, "local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val mapboxToken = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""

        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                // Do not change the username ==> 'mapbox' is correct
                username = "mapbox"
                // Use the secret token you created as the password
                password = mapboxToken
            }
        }
    }
}

rootProject.name = "BusTrack_App"
>>>>>>> 30877b02823e2ae42972719cea4a7ea631857fdd
include(":app")
 