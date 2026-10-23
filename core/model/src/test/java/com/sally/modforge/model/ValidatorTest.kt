package com.sally.modforge.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorTest {

    private val matrix = VersionMatrix.parse(
        """
        {"schema":1,"updated":"2026-10-23","versions":[
          {"minecraft":"1.21.1","javaMin":21,"javaMax":21,"javaRecommended":21,"gradle":"8.10.2",
           "loom":"1.7","fabricLoader":"0.16.9","fabricApi":"x","mappings":["mojang"]},
          {"minecraft":"1.18.2","javaMin":17,"javaMax":17,"javaRecommended":17,"gradle":"8.1.1",
           "loom":"1.2","fabricLoader":"0.14.22","fabricApi":"x","mappings":["yarn"],"stability":"experimental"}
        ]}
        """.trimIndent()
    )

    private fun config(mc: String = "1.21.1", java: Int = 21, modId: String = "mycoolmod") = ProjectConfig(
        id = "a1",
        name = "My Cool Mod",
        modId = modId,
        packageName = "com.sally.mycoolmod",
        minecraft = mc,
        java = java,
        gradle = "8.10.2",
    )

    @Test
    fun validConfigHasNoErrors() {
        assertTrue(validate(config(), matrix).none { it.severity == Severity.ERROR })
    }

    @Test
    fun wrongJavaSuggestsRecommendedVersion() {
        val issue = validate(config(java = 17), matrix).single()
        assertEquals("java", issue.field)
        assertEquals(21, issue.fixedJava)
    }

    @Test
    fun unknownMinecraftIsError() {
        val issues = validate(config(mc = "9.9.9"), matrix)
        assertEquals(Severity.ERROR, issues.single().severity)
    }

    @Test
    fun invalidModIdIsError() {
        val issues = validate(config(modId = "My-Mod"), matrix)
        assertTrue(issues.any { it.field == "modId" && it.severity == Severity.ERROR })
    }

    @Test
    fun experimentalVersionIsWarningOnly() {
        val issues = validate(config(mc = "1.18.2", java = 17), matrix)
        assertTrue(issues.none { it.severity == Severity.ERROR })
        assertTrue(issues.any { it.severity == Severity.WARNING })
    }
}
