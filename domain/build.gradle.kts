// Pure Kotlin — no Android. Reminder logic, unit conversion and other rules live here
// so they can be unit-tested quickly in IntelliJ without an emulator.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
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

// Feeds the merged coverage report (root build.gradle.kts).
kover {
    currentProject {
        createVariant("coverage") { add("jvm") }
    }
}

// The tests read the bundled lists and the import docs from outside this module (by relative path, from the
// module's folder), so they are declared here: a changed list reruns the tests instead of passing from the cache.
tasks.test {
    val assets = rootProject.layout.projectDirectory.dir("app/src/main/assets")
    listOf("known-schedules", "known-specs", "known-takes", "maker-documents", "bulletins").forEach { name ->
        inputs.dir(assets.dir(name)).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("assets.$name")
    }
    inputs.dir(rootProject.layout.projectDirectory.dir("docs/import")).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("importDocs")
}
