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
    private val data = listOf("../data/src/main/kotlin")
    private val appAndData = app + data
    private val appTests = listOf("src/test/kotlin")

    /**
     * The annotation and the `fun` it is on, whether on one line or the annotation on its own above it (#646): a
     * `VehicleEntity` among the fun's parameters, or named as the annotation's `entity`.
     */
    private val vehicleRow = Rule(
        "Write a vehicle's columns one at a time (VehicleDao.setNickname etc.), never a whole-row @Update or @Upsert, " +
            "which could undo a recall check that landed meanwhile",
        Regex(
            """@(?:Update|Upsert)\b(?:\s*\([^)]*\))?(?:\s*@\w+(?:\s*\([^)]*\))?)*\s*(?:\w+\s+)*fun\s+\w+\s*\([^)]*\bVehicleEntity\b""" +
                """|@(?:Update|Upsert)\s*\([^)]*\bVehicleEntity\b""",
        ),
        data,
    )

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
        vehicleRow,
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

    /**
     * The lines, from 1, on which [rule] matches [text]. Comments and imports are left out line by line; what is left
     * is matched as a whole, so a pattern can reach across lines.
     */
    private fun lines(rule: Rule, text: String): List<Int> {
        val code = text.lines().joinToString("\n") { line ->
            val code = line.substringBefore("//").trim()
            if (code.startsWith("*") || code.startsWith("import ")) "" else code
        }
        return rule.pattern.findAll(code).map { m -> code.substring(0, m.range.first).count { it == '\n' } + 1 }.distinct().toList()
    }

    @Test
    fun `the source keeps the fixed-pattern rules`() {
        val found = rules.flatMap { rule ->
            rule.roots.flatMap { root -> File(root).walk().filter { it.extension == "kt" && it.name !in rule.allowedIn } }
                .flatMap { file -> lines(rule, file.readText()).map { "${file.path}:$it: ${rule.why}" } }
        }
        assertEquals(found.joinToString("\n"), emptyList<String>(), found)
    }

    @Test
    fun `a whole-row write of a vehicle is caught with its annotation on the fun's line or on its own above it`() {
        listOf(
            "@Update suspend fun update(vehicle: VehicleEntity)",
            "@Update\nsuspend fun update(vehicle: VehicleEntity)",
            "@Upsert(onConflict = 1)\n@Transaction\nsuspend fun put(\n    vehicles: List<VehicleEntity>,\n)",
            "@Update(entity = VehicleEntity::class)\nsuspend fun setNickname(row: NicknameRow)",
        ).forEach { assertEquals(it, listOf(1), lines(vehicleRow, it)) }
        listOf(
            "@Update suspend fun update(item: ScheduleItemEntity)\nsuspend fun byId(id: Long): VehicleEntity?",
            "@Update\nsuspend fun update(item: ScheduleItemEntity)\n@Insert suspend fun insert(vehicle: VehicleEntity): Long",
            "@Query(\"UPDATE vehicles SET nickname = :name WHERE id = :id\")\nsuspend fun setNickname(id: Long, name: String)",
            "// @Update suspend fun update(vehicle: VehicleEntity)",
        ).forEach { assertEquals(it, emptyList<Int>(), lines(vehicleRow, it)) }
    }

    @Test
    fun `every rule reads source that is there`() {
        rules.flatMap { it.roots }.distinct().forEach { root ->
            assertTrue("$root has no Kotlin files", File(root).walk().any { it.extension == "kt" })
        }
    }
}
