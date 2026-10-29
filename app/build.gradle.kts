plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kover)
}

// CI passes -PversionName=1.2.0 -PversionCode=<run number>. Local builds use version.txt, which the
// release PR keeps current.
val appVersionName = (findProperty("versionName") as String?)
    ?: rootProject.file("version.txt").takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
    ?: "0.1.0"
val appVersionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1

android {
    namespace = "io.cosgriff.glovebox"
    compileSdk = 37

    defaultConfig {
        // Never change after the first release — Obtainium/Android treat a new ID as a new app.
        applicationId = "io.cosgriff.glovebox"
        minSdk = 29
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Set by the GitHub release workflow from repository secrets. See README.
            val ks = System.getenv("GLOVEBOX_KEYSTORE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("GLOVEBOX_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("GLOVEBOX_KEY_ALIAS")
                keyPassword = System.getenv("GLOVEBOX_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // The released APK carries native code (SQLCipher, Tesseract) for 64-bit ARM only, which is every
            // phone the app is meant for; four processor types made it 49 MB, this makes it 22. A 32-bit
            // phone or an x86 emulator refuses to install it rather than failing later. Debug builds keep
            // every type, so any emulator works. See CLAUDE.md before adding one back.
            ndk { abiFilters += "arm64-v8a" }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("GLOVEBOX_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    androidResources {
        // DashReader asks how long Tesseract's language file is before copying it out, which Android
        // can only answer for an asset stored as it is.
        noCompress += "traineddata"
        // The app's languages for Android's per-app language setting, from the values-xx folders;
        // res/resources.properties says the default folder is US English (#236).
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // BuildConfig.DEBUG: Read an invoice writes its trace in debug builds only (#484).
        buildConfig = true
    }

    // The screenshot tests (#233) draw screens under Robolectric, which needs the app's resources and fonts.
    testOptions.unitTests.isIncludeAndroidResources = true
    // Gradle's default 512 MB test heap ran out under `build`, where Robolectric screen tests share the
    // machine with every other module: they died of OutOfMemoryError or timed out waiting on a busy JVM.
    testOptions.unitTests.all { it.maxHeapSize = "2g" }
    // :data's fake Android Keystore: PhotoCrypto refuses to work without one, as on a phone whose Keystore fails.
    sourceSets { named("test") { kotlin.directories.add("${rootDir}/data/src/testShared/kotlin") } }

    lint {
        abortOnError = true
        // No warnings are kept: a new one fails the build, as an error does.
        warningsAsErrors = true
        // Dependabot proposes new versions, after its 5-day cooldown; lint would flag them on day one.
        // AndroidGradlePluginVersion is the same check for Gradle and AGP themselves.
        disable += "NewerVersionAvailable"
        disable += "AndroidGradlePluginVersion"
        // The release is 64-bit ARM only on purpose (abiFilters above); no ChromeOS x86_64 build.
        disable += "ChromeOsAbiSupport"
    }
}

kotlin {
    jvmToolchain(17)
}

// The plugin otherwise downloads each licence's wording from GitHub while it builds, and quietly leaves it empty
// when that fails. The wording is the app's own copy instead (assets/licences/spdx), so a build fetches nothing.
aboutLibraries {
    offlineMode = true
}

/** Puts the repository's LICENSE into the app's assets, so the About screen shows the same text the repo carries. */
abstract class CopyAppLicence : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val licence: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val target = outputDir.get().asFile.resolve("licences/glovebox.txt")
        target.parentFile.mkdirs()
        licence.get().asFile.copyTo(target, overwrite = true)
    }
}

val copyAppLicence = tasks.register<CopyAppLicence>("copyAppLicence") {
    licence.set(rootProject.layout.projectDirectory.file("LICENSE"))
}

// The baseline profile (#222) is generated by :baselineprofile and committed under
// src/release/generated/baselineProfiles. A build only copies it in; it is regenerated by hand
// (CLAUDE.md, Commands), never as part of `build`.
baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
    // One profile for the one release; there are no flavors.
    mergeIntoMain = false
}

androidComponents {
    // The plugin adds `nonMinifiedRelease` (to generate the profile) and `benchmarkRelease` (to measure it),
    // both copied from `release` and so arm64 only. They run on an emulator, which on a PC is x86_64, so
    // those two carry that too. They are never released: the release APK stays arm64 only (#123).
    finalizeDsl { android ->
        listOf("nonMinifiedRelease", "benchmarkRelease").forEach { name ->
            android.buildTypes.findByName(name)?.ndk?.abiFilters?.add("x86_64")
        }
    }
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyAppLicence, CopyAppLicence::outputDir)
    }
}

dependencies {
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.work.runtime)
    // The home-screen widget (#238). androidx only, no network, no permissions; checked as CLAUDE.md asks.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.compose)
    // Documents open in the app (#224). Its OCR add-on (pdf-ocr-play-services) is never on the classpath.
    implementation(libs.androidx.pdf.compose)
    implementation(libs.androidx.pdf.document.service)
    implementation(libs.zxing.cpp)
    implementation(libs.pdfbox.android) {
        // Only needed to open encrypted PDFs, and it would clash with the app's own BouncyCastle.
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.tesseract4android)
    // Installs the baseline profile on first launch (#222). Compose brings it in already; named here
    // because the app depends on it now.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.uiautomator)
    // Draws the barcodes the reader is tested on; the app reads them with zxing-cpp.
    androidTestImplementation(libs.zxing.core)
    testImplementation(libs.zxing.core)

    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.roborazzi.accessibility.check)
    // The empty activity the Compose test rule draws into; only in the debug manifest, never the release.
    debugImplementation(libs.compose.ui.test.manifest)
}

// Feeds the merged coverage report (root build.gradle.kts).
kover {
    currentProject {
        createVariant("coverage") { add("debug") }
    }
}
