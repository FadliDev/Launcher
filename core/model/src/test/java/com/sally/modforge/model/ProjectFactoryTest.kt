package com.sally.modforge.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectFactoryTest {

    private val matrix = VersionMatrix.parse(
        """{"schema":1,"updated":"x","versions":[{"minecraft":"1.21.1","javaMin":21,"javaMax":21,
          "javaRecommended":21,"gradle":"8.10.2","loom":"1.7","fabricLoader":"0.16.9","fabricApi":"x",
          "mappings":["mojang"]}]}""".trimIndent()
    )

    @Test
    fun slugFromName() {
        assertEquals("my_cool_mod", ProjectFactory.slugModId("My Cool Mod"))
        assertEquals("m_1_21_test", ProjectFactory.slugModId("1.21 Test"))
        assertEquals("mymod", ProjectFactory.slugModId(""))
        assertEquals("mymod", ProjectFactory.slugModId("!!"))
        assertEquals("a_mod", ProjectFactory.slugModId("A"))
    }

    @Test
    fun createdProjectPassesValidation() {
        val mod = ProjectFactory.slugModId("Ruby Tools")
        val config = ProjectFactory.create(
            id = "x", name = "Ruby Tools", modId = mod,
            packageName = ProjectFactory.defaultPackage(mod),
            minecraft = "1.21.1", java = 21, matrix = matrix,
        )
        assertEquals("8.10.2", config.gradle)
        assertTrue(validate(config, matrix).none { it.severity == Severity.ERROR })
    }
}
