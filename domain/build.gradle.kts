// Pure Kotlin — no Android. Reminder logic, unit conversion and other rules live here
// so they can be unit-tested quickly in IntelliJ without an emulator.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Reads import files as a JSON tree (ImportFormat). Pure Kotlin, like everything here.
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    // Test only: checks the import examples against the published JSON Schemas, so the schemas and
    // ImportFormat can't drift apart. Never ships in the app.
    testImplementation(libs.json.schema.validator)
}
