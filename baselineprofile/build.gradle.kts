// Generates the app's baseline profile and measures it (#222). Nothing here ships in the app.
//
//   ./gradlew :app:generateReleaseBaselineProfile        regenerate the profile (CLAUDE.md, Commands)
//   ./gradlew :baselineprofile:pixel6Api34BenchmarkReleaseAndroidTest   startup and scroll timings
//
// Both run on a Gradle managed device by default, an emulator Gradle makes for itself, so neither can
// touch an emulator or phone with someone's data on it: they clear the app's data.
//
//   ANDROID_SERIAL=emulator-5556 ./gradlew :app:generateReleaseBaselineProfile -Pglovebox.profileOn=connected
//   ANDROID_SERIAL=emulator-5556 ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.cosgriff.glovebox.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Generation needs API 28 or later; collecting on an emulator needs 33 (no root).
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // The timings are compared with each other on the same emulator, never taken as a phone's.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"

    testOptions.managedDevices.allDevices {
        create<ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            // Plain AOSP: no Play services to start work of their own while the app is measured. The
            // image matches the computer, x86_64 on a PC and arm64 on a Mac.
            systemImageSource = "aosp"
            testedAbi = if (System.getProperty("os.arch") == "aarch64") "arm64-v8a" else "x86_64"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

// -Pglovebox.profileOn=connected runs on the one connected emulator instead (say which with ANDROID_SERIAL),
// for a newer phone than the managed one. Only a throwaway emulator: the app's data is cleared.
val onConnected = findProperty("glovebox.profileOn") == "connected"

baselineProfile {
    if (onConnected) {
        useConnectedDevices = true
    } else {
        managedDevices += "pixel6Api34"
        useConnectedDevices = false
    }
}

dependencies {
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
