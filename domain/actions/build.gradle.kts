plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin { jvmToolchain(21) }

// Cross-domain compound operations (docs/status/DECISIONS.md): one write path per user action
// that spans several rule modules (scene delete/restore, character delete/merge, export runs, purges).
dependencies {
    api(project(":core:model"))
    api(project(":domain:story"))
    api(project(":domain:script"))
    api(project(":domain:cast"))
    api(project(":domain:world"))
    api(project(":domain:trash"))
    api(project(":domain:library"))
    api(project(":domain:recorder"))
    api(project(":domain:checks"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
