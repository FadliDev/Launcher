package com.sally.modforge.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun sample(id: String, name: String) = ProjectConfig(
        id = id, name = name, modId = "mod_$id", packageName = "com.sally.mod$id",
        minecraft = "1.21.1", java = 21, gradle = "8.10.2",
    )

    @Test
    fun saveListGetDelete() {
        val store = ProjectStore(tmp.root)
        assertEquals(emptyList<ProjectConfig>(), store.list())

        store.save(sample("b", "Beta"))
        store.save(sample("a", "Alpha"))
        assertEquals(listOf("Alpha", "Beta"), store.list().map { it.name })
        assertEquals("Beta", store.get("b")?.name)

        assertEquals(true, store.delete("a"))
        assertNull(store.get("a"))
        assertEquals(1, store.list().size)
    }

    @Test
    fun corruptFileIsSkipped() {
        val store = ProjectStore(tmp.root)
        store.save(sample("ok", "Ok"))
        java.io.File(tmp.root, "broken").apply { mkdirs() }
        java.io.File(tmp.root, "broken/project.json").writeText("{not json")
        assertEquals(listOf("Ok"), store.list().map { it.name })
    }
}
