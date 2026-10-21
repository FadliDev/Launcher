package com.sally.ide.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class SnippetParserTest {
    @Test
    fun expandsPlaceholdersAndOrdersStops() {
        val result = SnippetParser.expand("fun \${1:name}(\${2:args}) {\n    \$0\n}")
        assertEquals("fun name(args) {\n    \n}", result.text)
        assertEquals(listOf(1, 2, 0), result.stops.map { it.index })
        assertEquals("name", result.text.substring(result.stops[0].start, result.stops[0].end))
        assertEquals("args", result.text.substring(result.stops[1].start, result.stops[1].end))
    }

    @Test
    fun endsAtTextEndWithoutExplicitZero() {
        val result = SnippetParser.expand("println(\${1:x})")
        assertEquals("println(x)", result.text)
        assertEquals(0, result.stops.last().index)
        assertEquals(result.text.length, result.stops.last().start)
    }

    @Test
    fun mirrorsRepeatedStopAndHandlesEscapesAndChoices() {
        val mirrored = SnippetParser.expand("\${1:a} = \$1")
        assertEquals("a = a", mirrored.text)
        assertEquals(2, mirrored.stops.size)

        val escaped = SnippetParser.expand("cost: \\\$5 \${1|x,y|}")
        assertEquals("cost: \$5 x", escaped.text)
    }

    @Test
    fun indentIsAppliedToFollowingLinesOnly() {
        val base = SnippetParser.expand("if (\${1:c}) {\n    \$0\n}")
        val indented = SnippetParser.applyIndent(base, "  ")
        assertEquals("if (c) {\n      \n  }", indented.text)
        val zero = indented.stops.last()
        assertEquals("if (c) {\n      ".length, zero.start)
    }

    @Test
    fun variablesResolveFromFileName() {
        val result = SnippetParser.expand("class \${TM_FILENAME_BASE}", "Main.kt")
        assertEquals("class Main", result.text)
    }
}
