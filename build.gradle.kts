// Build tools only (#273): AGP 9.4 brings Bouncy Castle 1.80.2 (known advisories) and an older httpclient onto the
// build's classpath and lint's. None of it ships, but the build runs where the signing key is. Hold them at patched
// versions until AGP moves on (9.6 does), then drop these constraints.
buildscript {
    dependencies {
        constraints {
            classpath(libs.bouncycastle)
            classpath(libs.bouncycastle.pkix)
            classpath(libs.bouncycastle.util)
            classpath(libs.httpclient)
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover)
}

subprojects {
    // A failing test prints its whole stack, causes and all: CI's log is often the only place a failure that
    // doesn't happen locally can be read.
    tasks.withType<Test>().configureEach {
        testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    }
    pluginManager.withPlugin("com.android.base") {
        dependencies.constraints {
            listOf(libs.bouncycastle, libs.bouncycastle.pkix, libs.bouncycastle.util, libs.httpclient).forEach { add("androidLintTool", it) }
        }
    }
}

// Test coverage (Codecov): `./gradlew koverXmlReportCoverage` runs the unit tests of :domain, :data and
// :app (debug) and merges them into build/reports/kover/reportCoverage.xml, which CI sends to Codecov.
dependencies {
    kover(project(":domain"))
    kover(project(":data"))
    kover(project(":app"))
}

kover {
    currentProject {
        createVariant("coverage") {}
    }
    reports {
        filters {
            excludes {
                // Generated code: Hilt, Room, Compose's singletons, BuildConfig and resources.
                classes(
                    "*_Factory", "*_Factory\$*", "*_MembersInjector", "*_HiltModules*", "*.Hilt_*", "*_Impl", "*_Impl\$*",
                    "hilt_aggregated_deps.*", "dagger.hilt.internal.aggregatedroot.codegen.*",
                    "*ComposableSingletons*", "*.BuildConfig", "*.R", "*.R\$*",
                )
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
    }
}

// Robolectric's Android jars (#585). Left to itself, Robolectric fetches android-all-instrumented from Maven
// Central at test time, outside Gradle: no cache holds it, verification-metadata.xml never checks it, and a
// Maven Central hiccup fails every Robolectric test in CI. Here Gradle resolves the jars (one per SDK the tests
// run on, versions in libs.versions.toml beside `robolectric`), copies them to build/robolectric, and every unit
// test task, Roborazzi's and Kover's runs included, reads them from there with Robolectric kept offline.
abstract class RobolectricOffline : CommandLineArgumentProvider {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val jars: DirectoryProperty

    override fun asArguments() = listOf(
        "-Drobolectric.offline=true",
        "-Drobolectric.dependency.dir=${jars.get().asFile.absolutePath}",
    )
}

configure(listOf(project(":app"), project(":data"))) {
    // One configuration per jar: in one, Gradle would keep only the highest of the versions of the one module.
    val androidAll = mapOf(
        "Sdk35" to rootProject.libs.robolectric.androidAll.sdk35,
        "Sdk29" to rootProject.libs.robolectric.androidAll.sdk29,
    ).map { (sdk, jar) ->
        configurations.create("robolectricAndroidAll$sdk") {
            isCanBeConsumed = false
            isTransitive = false
        }.also { dependencies.add(it.name, jar) }
    }
    val copyJars = tasks.register<Sync>("robolectricAndroidAll") {
        description = "Copies the Android jars Robolectric runs the unit tests on, so it never downloads them."
        androidAll.forEach { from(it) }
        into(layout.buildDirectory.dir("robolectric"))
    }
    val offline = objects.newInstance<RobolectricOffline>().apply {
        jars.fileProvider(copyJars.map { it.destinationDir })
    }
    tasks.withType<Test>().configureEach { jvmArgumentProviders.add(offline) }
}
