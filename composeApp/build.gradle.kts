plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.sqldelight.coroutines)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.location)
                implementation(libs.androidx.core.ktx)
                implementation(libs.sqldelight.driver.android)
                implementation(libs.ktor.client.android)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.sqldelight.driver.sqlite)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.coroutines.swing)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.runner)
                implementation(kotlin("test"))
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.ext)
                implementation(libs.androidx.test.espresso)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }
    }
}

android {
    namespace = "com.activemap"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.activemap"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.activemap.MainKt"
        nativeDistributions {
            packageName = "ActiveMap"
            packageVersion = "1.0.0"
            description = "AI-Coach for Beginners"
            vendor = "ActiveMap"
        }
    }
}

sqldelight {
    databases {
        create("ActiveMapDatabase") {
            packageName.set("com.activemap.db")
        }
    }
}
