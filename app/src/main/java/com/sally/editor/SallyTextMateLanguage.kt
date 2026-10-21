package com.sally.editor

import android.os.Bundle
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference

/** Adds language-aware starter snippets to Sora Editor's native completion popup. */
class SallyTextMateLanguage(
    private val snippets: List<CodeSnippet>
) : TextMateLanguage(
    requireNotNull(GrammarRegistry.getInstance().findGrammar("source.sally")) {
        "Sally TextMate grammar was not loaded"
    },
    GrammarRegistry.getInstance().findLanguageConfiguration("source.sally"),
    GrammarRegistry.getInstance(),
    ThemeRegistry.getInstance(),
    true
) {
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        super.requireAutoComplete(content, position, publisher, extraArguments)
        val line = content.getLine(position.line)
        var prefixLength = 0
        var index = (position.column - 1).coerceAtMost(line.length - 1)
        while (index >= 0 && Character.isJavaIdentifierPart(line[index])) {
            prefixLength++
            index--
        }
        snippets.forEach { snippet ->
            publisher.addItem(
                SimpleCompletionItem(
                    snippet.prefix,
                    snippet.description,
                    prefixLength,
                    snippet.body
                )
            )
        }
    }
}
