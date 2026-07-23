import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

// Read the Finnhub API key from local.properties (git-ignored) or an env var,
// so the secret is never committed to the public repository.
val finnhubApiKey: String = run {
    val props = Properties()
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        localProperties.inputStream().use { props.load(it) }
    }
    props.getProperty("finnhub.api.key")
        ?: System.getenv("FINNHUB_API_KEY")
        ?: ""
}

val generatedConfigDir = layout.buildDirectory.dir("generated/finnhub/kotlin")

val generateFinnhubSecrets by tasks.registering {
    val outputDir = generatedConfigDir
    val apiKey = finnhubApiKey
    inputs.property("apiKey", apiKey)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().asFile.resolve("io/onlyfunds/network")
        packageDir.mkdirs()
        packageDir.resolve("FinnhubSecrets.kt").writeText(
            """
            package io.onlyfunds.network

            internal object FinnhubSecrets {
                const val API_KEY: String = "$apiKey"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    iosArm64()
    iosSimulatorArm64()
    jvm()
    js { browser() }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateFinnhubSecrets)
            dependencies { implementation(libs.ktor.client.core) }
        }
        androidMain.dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        jvmMain.dependencies { implementation(libs.ktor.client.cio) }
        jsMain.dependencies { implementation(libs.ktor.client.js) }
        wasmJsMain.dependencies { implementation(libs.ktor.client.js) }
    }
}

android {
    namespace = "io.onlyfunds.network"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
