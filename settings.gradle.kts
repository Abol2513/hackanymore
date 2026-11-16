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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "HEXFORGE"

include(
    ":app",
    ":core:common",
    ":core:hexcore",
    ":core:hexscan",
    ":core:hexhook",
    ":core:hexelf",
    ":core:database",
    ":core:chronicle",
    ":core:vault",
    ":core:gatekeeper",
    ":feature:hexspace",
    ":feature:ghostscope",
    ":feature:patchforge",
    ":feature:skinwalker",
    ":feature:deepprobe",
    ":feature:obsidianfiles",
    ":feature:shizuku",
    ":feature:oracle",
    ":feature:nightshift",
    ":feature:wishbuilder",
    ":feature:depot",
)
