pluginManagement {
    repositories {
        gradlePluginPortal() // ✅ KSP plugin is here
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }


}

rootProject.name = "HelpAtHome"
include(":app")