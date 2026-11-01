package io.cosgriff.glovebox.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The rules in CLAUDE.md that are a fixed pattern in the source, checked here so the build catches
 * them instead of review. A rule that needs judgement (which `Locale.US` is for display in `:domain`)
 * stays with review.
 */
class SourceRulesTest {
    private class Rule(val why: String, val pattern: Regex, val roots: List<String>, val allowedIn: Set<String> = emptySet())

    private val app = listOf("src/main/kotlin")
    private val appAndData = app + "../data/src/main/kotlin"
    private val appTests = listOf("src/test/kotlin")

    private val rules = listOf(
        Rule("Use PlexMono, never FontFamily.Monospace", Regex("""\bFontFamily\.Monospace\b"""), app),
        Rule(
            "Format figures with the shared formatters in Mappers.kt, never String.format",
            Regex("""\bString\.format\s*\("""), app,
        ),
        Rule(
            "The app runs in its translation's locale: Locale.US only to change case",
            Regex("""(?<!uppercase\(|lowercase\()\bLocale\.US\b"""), app,
        ),
        Rule(
            "Add a destination with screen<Route>, never entry<Route>",
            Regex("""\bentry<"""), app, allowedIn = setOf("GloveboxNavHost.kt"),
        ),
        Rule(
            "Go around the DAOs with useReaderConnection / useWriterConnection, never openHelper",
            Regex("""\bopenHelper\b"""), appAndData,
        ),
        Rule(
            "Composables use stringResource, never LocalContext.current.getString",
            Regex("""\bLocalContext\.current\.getString\b"""), app,
        ),
        Rule(
            "Tests make their database with testDatabase(), so closeWhenIdle() can wait for its queries",
            Regex("""\binMemoryDatabaseBuilder\b"""), appTests, allowedIn = setOf("GarageTestSupport.kt"),
        ),
        Rule(
            "Close a test database with closeWhenIdle(), never close(): a query still running fails at random",
            Regex("""\b(db|database)\.close\(\)"""), appTests,
        ),
    )

    @Test
    fun `the source keeps the fixed-pattern rules`() {
        val found = rules.flatMap { rule ->
            rule.roots.flatMap { root -> File(root).walk().filter { it.extension == "kt" && it.name !in rule.allowedIn } }
                .flatMap { file ->
                    file.readLines().withIndex().mapNotNull { (i, line) ->
                        val code = line.substringBefore("//").trim()
                        if (code.startsWith("*") || code.startsWith("import ") || !rule.pattern.containsMatchIn(code)) null
                        else "${file.path}:${i + 1}: ${rule.why}"
                    }
                }
        }
        assertEquals(found.joinToString("\n"), emptyList<String>(), found)
    }

    @Test
    fun `every rule reads source that is there`() {
        rules.flatMap { it.roots }.distinct().forEach { root ->
            assertTrue("$root has no Kotlin files", File(root).walk().any { it.extension == "kt" })
        }
    }
}
