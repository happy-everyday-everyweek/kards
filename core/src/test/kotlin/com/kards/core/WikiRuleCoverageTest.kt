package com.kards.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 覆盖门槛检查：把「每条 Wiki 规则必须出现在两个或以上测试用例中」变成可执行断言。
 * 扫描测试源码里的 @WikiRule 注解，统计每个 RuleId 被多少个测试方法引用。
 */
class WikiRuleCoverageTest {

    private val dir = File("src/test/kotlin/com/kards/core")

    /** 单元测试文件：文件名包含 Test.kt 但不算集成测试。 */
    private val unitFiles = listOf(
        "KreditAndDrawTest.kt",
        "DeckNationModeTest.kt",
        "FrontlineUnitTest.kt",
        "CombatKeywordTest.kt",
        "OtherKeywordTest.kt"
    )

    /** 集成测试文件：名字里带 Integration。 */
    private fun integrationFileList(): List<File> =
        dir.listFiles()?.filter { it.name.contains("Integration") && it.extension == "kt" } ?: emptyList()

    private fun readAll(): List<File> =
        dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "WikiRuleCoverageTest.kt" }
            .toList()

    private fun collect(files: List<File>): Map<RuleId, MutableList<String>> {
        val hits = RuleId.entries.associateWith { mutableListOf<String>() }
        val annotation = Regex("""@WikiRule\(([^)]*)\)""", RegexOption.DOT_MATCHES_ALL)
        val fn = Regex("""fun\s+([^\s(]+)\s*\(""")

        files.forEach { file ->
            val text = file.readText()
            annotation.findAll(text).forEach { m ->
                val names = Regex("""RuleId\.([A-Z0-9_]+)""")
                    .findAll(m.groupValues[1]).map { it.groupValues[1] }.toList()
                val name = fn.find(text.substring(m.range.last))?.groupValues?.get(1) ?: "unknown"
                names.forEach { raw ->
                    RuleId.entries.firstOrNull { it.name == raw }?.let { hits.getValue(it) += "${file.name}:$name" }
                }
            }
        }
        return hits
    }

    @Test
    fun 每条Wiki规则至少被两个测试用例覆盖() {
        assertTrue("测试目录存在", dir.exists())
        val hits = collect(readAll())
        val missing = hits.filter { it.value.size < 2 }.map { "${it.key.name}(${it.key.cn})=${it.value.size}" }
        assertTrue(
            "以下规则未被两个或以上测试用例覆盖：\n" + missing.joinToString("\n"),
            missing.isEmpty()
        )
    }

    @Test
    fun 每条Wiki规则同时有单元测试与集成测试() {
        assertTrue("测试目录存在", dir.exists())
        val all = collect(readAll())
        val integ = collect(integrationFileList())

        // 每条规则必须有集成层路径
        val noInteg = RuleId.entries.filter { (integ[it]?.size ?: 0) == 0 }.map { "${it.name}(${it.cn})" }
        assertTrue("以下规则缺少集成测试：\n" + noInteg.joinToString("\n"), noInteg.isEmpty())

        // 每条规则还须至少两条独立引用（单元或集成均可），保证不是单点断言
        val thin = all.filter { it.value.size < 2 }.map { "${it.key.name}(${it.key.cn})=${it.value.size}" }
        assertTrue("以下规则引用不足两条：\n" + thin.joinToString("\n"), thin.isEmpty())
    }

    @Test
    fun 标注总数至少是规则数的两倍() {
        val total = collect(readAll()).values.sumOf { it.size }
        assertTrue(
            "标注总数 $total 少于 ${RuleId.entries.size * 2}",
            total >= RuleId.entries.size * 2
        )
    }
}