import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
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

// Optional custom CORS proxy base for the web target (e.g. a Cloudflare Worker).
// Public proxies are unreliable, so a self-hosted proxy is the supported way to
// make the chart load in the browser. Use "{url}" as a placeholder for the
// encoded Yahoo URL, otherwise it is appended. Empty => public fallbacks only.
val yahooCorsProxy: String = run {
    val props = Properties()
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        localProperties.inputStream().use { props.load(it) }
    }
    props.getProperty("yahoo.cors.proxy")
        ?: System.getenv("YAHOO_CORS_PROXY")
        ?: ""
}

val generatedConfigDir = layout.buildDirectory.dir("generated/finnhub/kotlin")

val generateFinnhubSecrets by tasks.registering {
    val outputDir = generatedConfigDir
    val apiKey = finnhubApiKey
    val corsProxy = yahooCorsProxy
    inputs.property("apiKey", apiKey)
    inputs.property("corsProxy", corsProxy)
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

            internal object YahooSecrets {
                const val CORS_PROXY: String = "$corsProxy"
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
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinxJson)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
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
