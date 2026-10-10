// Pure Kotlin — no Android. Reminder logic, unit conversion and other rules live here
// so they can be unit-tested quickly in IntelliJ without an emulator.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
