import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningFile = rootProject.file(providers.gradleProperty("releaseSigningFile").orElse("release.properties").get())
val releaseSigning = Properties().apply {
    if (releaseSigningFile.isFile) releaseSigningFile.inputStream().use { load(it) }
}

android {
    namespace = "ir.rosheno.launcher"
    compileSdk = 37
    defaultConfig {
        applicationId = "ir.rosheno.launcher"
        minSdk = 28
        targetSdk = 36
        versionCode = 8
        versionName = "0.7.0-prototype"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (releaseSigningFile.isFile) create("release") {
            storeFile = rootProject.file(requireNotNull(releaseSigning.getProperty("storeFile")))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
            storeType = "JKS"
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2026.03.00")
    implementation(bom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")
    androidTestImplementation(bom)
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    // Compose's transitive Espresso 3.5.0 uses reflection removed on API 37.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Missing local credentials must never silently produce an unsigned release.
val validateReleaseCredentials by tasks.registering {
    doLast {
        check(releaseSigningFile.isFile) { "Missing release.properties. See docs/RELEASE_BUILD.md." }
        listOf("storeFile", "storePassword", "keyAlias", "keyPassword").forEach {
            check(!releaseSigning.getProperty(it).isNullOrBlank()) { "Missing release signing property: $it" }
        }
        check(rootProject.file(releaseSigning.getProperty("storeFile")).isFile) { "Release keystore does not exist." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseCredentials) }
