pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GlucoRing"
include(":app")
include(":core-ble")
include(":core-data")
include(":core-signal")
include(":core-ml")
include(":core-sync")
