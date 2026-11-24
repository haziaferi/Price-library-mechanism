// Kotlin Multiplatform common code for Android and the JVM desktop, with no
// storage, screens or tags (SPEC:R03). Tests run on the JVM.
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

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
