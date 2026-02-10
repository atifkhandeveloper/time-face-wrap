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
        google()
        jcenter()
        gradlePluginPortal()
        maven { url = uri("https://developer.huawei.com/repo/") } // Add this for plugins
//        maven(url = "https://developer.huawei.com/repo/")
    }

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Time Wrap"
include(":app")
include(":nativetemplates")
