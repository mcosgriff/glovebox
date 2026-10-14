plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.aboutlibraries)
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
        targetSdk = 36
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Existing warnings live in the baseline; CI fails only on new issues.
        // Regenerate after fixing old ones: ./gradlew :app:updateLintBaseline
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = false
    }
}

kotlin {
    jvmToolchain(17)
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

androidComponents {
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.zxing.core)
    implementation(libs.tesseract4android)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.uiautomator)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
