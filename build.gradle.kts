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
