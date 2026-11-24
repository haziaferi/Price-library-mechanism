// Kotlin Multiplatform common code for Android and the JVM desktop, with no
// storage, screens or tags (SPEC:R03). Tests run on the JVM and on Node.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

group = "com.hazia.pricecompare"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.hazia.pricecompare"
        compileSdk = 37
        minSdk = 33
    }

    jvm()

    // A non-JVM target, so the shared code compiles against the common standard
    // library and the tests also run on Node (SPEC:R03). No host uses it.
    js {
        nodejs()
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Node.js and Yarn come from the repositories declared in settings.gradle.kts:
// with no base URL of their own, the Kotlin plugins add no repositories.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
}
