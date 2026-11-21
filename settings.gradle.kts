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
rootProject.name = "civilight"
include(":app")
include(":core:model")
include(":domain:story")
include(":domain:script")
include(":domain:cast")
include(":domain:world")
include(":domain:trash")
include(":domain:library")
include(":domain:recorder")
include(":domain:checks")
include(":core:store")
include(":core:designsystem")
include(":core:navigation")
include(":core:ui")
include(":feature:library")
include(":feature:story")
include(":feature:script")
include(":feature:cast")
include(":feature:world")
include(":feature:map")
include(":feature:canvas")
include(":feature:recorder")
