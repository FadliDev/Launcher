#!/usr/bin/env python3
"""Generate TextMate grammars, language configs, MD3 editor themes and bundled snippets.

Grammar di sini ditulis sendiri (lebih kaya dari v0.2.0: operator, deklarasi fun/class,
string template Kotlin, properti, konstanta, namespace XML, link Markdown). Ganti dengan
grammar VSCode penuh kapan saja (taruh di assets/textmate/<lang>/ dan update languages.json).

Palet tema = turunan palet MD3 biru statis di ui/Theme.kt (surface, primary, outline sama persis)."""
import json, os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets")
TM = os.path.join(OUT, "textmate")

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)

def kw(words):
    return r"\b(?:" + "|".join(words) + r")\b"

ESC = {"name": "constant.character.escape", "match": r"\\(?:u[0-9a-fA-F]{4}|.)"}
NUM = {"name": "constant.numeric",
       "match": r"\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[fFLlDdUu]*)\b"}
OP = {"name": "keyword.operator", "match": r"[-+*/%=<>!&|^~?:]+"}
CONST_UPPER = {"name": "constant.other", "match": r"\b[A-Z][A-Z0-9_]{2,}\b"}
FN_CALL = {"name": "entity.name.function", "match": r"\b[A-Za-z_][A-Za-z0-9_]*(?=\s*\()"}
PROP = {"name": "variable.other.property", "match": r"(?<=\.)[A-Za-z_][A-Za-z0-9_]*\b(?!\s*\()"}
TYPE_NAME = {"name": "entity.name.type", "match": r"\b[A-Z][A-Za-z0-9_]*\b"}
ANNOT = {"name": "storage.type.annotation", "match": r"@[A-Za-z_][\w.]*"}

def dq(inner=()): return {"name": "string.quoted.double", "begin": '"', "end": '"', "patterns": [ESC] + list(inner)}
def sq(): return {"name": "string.quoted.single", "begin": "'", "end": "'", "patterns": [ESC]}
def blk(): return {"name": "comment.block", "begin": r"/\*", "end": r"\*/"}
def line(tok="//"): return {"name": "comment.line", "match": tok + r".*$"}

# Kotlin / Groovy string template: $name dan ${ ... }
TEMPLATE = [
    {"name": "variable.other.interpolation", "match": r"\$[A-Za-z_]\w*"},
    {"name": "meta.template", "begin": r"\$\{", "end": r"\}",
     "beginCaptures": {"0": {"name": "keyword.declaration"}}, "endCaptures": {"0": {"name": "keyword.declaration"}},
     "patterns": [{"include": "$self"}]},
]

def clike(scope, name, kws, mods, types, consts, decl, extra_before=(), extra_after=()):
    pats = list(extra_before) + [blk(), line(), dq(), sq(), NUM, ANNOT] + [
        {"match": r"^\s*(import|package)\s+([\w.*]+)",
         "captures": {"1": {"name": "keyword.declaration"}, "2": {"name": "entity.name.namespace"}}},
        {"match": r"\b(" + "|".join(decl) + r")\s+([A-Za-z_]\w*)",
         "captures": {"1": {"name": "keyword.declaration"}, "2": {"name": "entity.name.type"}}},
        {"name": "keyword.control", "match": kw(kws)},
        {"name": "storage.modifier", "match": kw(mods)},
        {"name": "storage.type.primitive", "match": kw(types)},
        {"name": "constant.language", "match": kw(consts)},
    ] + list(extra_after) + [FN_CALL, PROP, CONST_UPPER, TYPE_NAME, OP]
    return {"scopeName": scope, "name": name, "patterns": pats}

java_kw = "assert break case catch continue default do else finally for if instanceof return switch throw throws try while yield".split()
java_mod = "abstract final native private protected public static strictfp synchronized transient volatile sealed non-sealed permits".split()
java_decl = ["class", "interface", "enum", "record", "extends", "implements", "new"]
java_ty = "boolean byte char double float int long short void var String Object".split()

kt_kw = "as break by catch continue do else finally for if in is return throw try when while".split()
kt_mod = "abstract annotation companion const crossinline data external final infix inline inner internal lateinit noinline open operator out override private protected public reified sealed suspend tailrec vararg".split()
kt_decl = ["enum class", "annotation class", "class", "interface", "object", "typealias"]
kt_ty = "Int Long Short Byte Float Double Boolean Char String Unit Any Nothing Array List Map Set".split()

cpp_kw = "asm break case catch continue default delete do else for goto if new return sizeof static_assert switch throw try typeid while alignas alignof decltype noexcept dynamic_cast static_cast const_cast reinterpret_cast".split()
cpp_mod = "const constexpr explicit export extern final friend inline mutable override private protected public register static virtual volatile".split()
cpp_decl = ["class", "struct", "union", "enum class", "enum", "namespace", "typename", "using", "typedef", "template"]
cpp_ty = "auto bool char double float int long short signed unsigned void size_t int8_t int16_t int32_t int64_t uint8_t uint16_t uint32_t uint64_t wchar_t char16_t char32_t".split()

gr_kw = "as assert break case catch continue def default else finally for if in instanceof return switch throw try while".split()
gr_mod = "abstract final private protected public static".split()
gr_decl = ["class", "interface", "enum", "extends", "implements", "new"]
gr_dsl = "plugins dependencies android repositories apply implementation api compileOnly runtimeOnly testImplementation androidTestImplementation kapt ksp id version buildscript allprojects subprojects task tasks ext include pluginManagement".split()

grammars = {}
grammars["java"] = clike("source.java", "Java", java_kw, java_mod, java_ty, ["true", "false", "null", "this", "super"], java_decl)
grammars["kotlin"] = clike("source.kotlin", "Kotlin", kt_kw, kt_mod, kt_ty, ["true", "false", "null", "this", "super"], kt_decl,
    extra_before=[
        {"name": "string.quoted.triple", "begin": '"""', "end": '"""', "patterns": TEMPLATE},
        {"match": r"\b(fun)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?([A-Za-z_]\w*)",
         "captures": {"1": {"name": "keyword.declaration"}, "2": {"name": "entity.name.function"}}},
        {"match": r"\b(val|var)\b", "name": "keyword.declaration"},
        {"name": "entity.name.label", "match": r"\b[A-Za-z_]\w*@"},
    ])
# string Kotlin biasa ikut template
for p in grammars["kotlin"]["patterns"]:
    if p.get("name") == "string.quoted.double":
        p["patterns"] = [ESC] + TEMPLATE

grammars["cpp"] = clike("source.cpp", "C/C++", cpp_kw, cpp_mod, cpp_ty, ["true", "false", "nullptr", "NULL", "this"], cpp_decl,
    extra_before=[{"name": "meta.preprocessor", "match": r"^\s*#\s*\w+"}],
    extra_after=[{"name": "string.quoted.other", "match": r"<[\w./]+>"}])
grammars["groovy"] = clike("source.groovy", "Gradle (Groovy/KTS)", gr_kw, gr_mod, ["String", "Integer", "Boolean", "List", "Map"],
    ["true", "false", "null", "this", "super"], gr_decl,
    extra_before=[{"name": "string.quoted.triple", "begin": "'''|\"\"\"", "end": "'''|\"\"\"", "patterns": TEMPLATE}],
    extra_after=[{"name": "support.function", "match": kw(gr_dsl)},
                 {"name": "entity.name.function", "match": r"\b[A-Za-z_]\w*(?=\s*\{)"}])
for p in grammars["groovy"]["patterns"]:
    if p.get("name") == "string.quoted.double":
        p["patterns"] = [ESC] + TEMPLATE

XML_REF = {"name": "constant.other.reference", "match": r"[@?]\+?(?:[A-Za-z_.]+:)?[A-Za-z_]+/[\w.]+"}
grammars["xml"] = {"scopeName": "text.xml", "name": "XML", "patterns": [
    {"name": "comment.block", "begin": "<!--", "end": "-->"},
    {"name": "string.unquoted.cdata", "begin": r"<!\[CDATA\[", "end": r"\]\]>"},
    {"name": "meta.preprocessor", "begin": r"<\?", "end": r"\?>"},
    {"begin": r"(</?)([A-Za-z_][\w.-]*)(?:(:)([A-Za-z_][\w.-]*))?", "end": r"/?>",
     "beginCaptures": {"1": {"name": "punctuation.definition.tag"}, "2": {"name": "entity.name.tag"},
                       "3": {"name": "punctuation.definition.tag"}, "4": {"name": "entity.name.tag"}},
     "endCaptures": {"0": {"name": "punctuation.definition.tag"}},
     "patterns": [
        {"name": "entity.other.attribute-name.namespace", "match": r"[A-Za-z_][\w.-]*(?=:[\w.-]+\s*=)"},
        {"name": "punctuation.definition.tag", "match": r":(?=[\w.-]+\s*=)"},
        {"name": "entity.other.attribute-name", "match": r"[A-Za-z_][\w.-]*(?=\s*=)"},
        {"name": "keyword.operator", "match": "="},
        {"name": "string.quoted.double", "begin": '"', "end": '"', "patterns": [XML_REF, {"name": "constant.character.entity", "match": r"&\w+;"}]},
        {"name": "string.quoted.single", "begin": "'", "end": "'", "patterns": [XML_REF]}]},
    {"name": "constant.character.entity", "match": r"&\w+;"}]}

grammars["json"] = {"scopeName": "source.json", "name": "JSON", "patterns": [
    {"name": "support.type.property-name", "match": r'"(?:[^"\\]|\\.)*"(?=\s*:)'},
    {"name": "string.quoted.double", "begin": '"', "end": '"', "patterns": [ESC]},
    NUM,
    {"name": "constant.language", "match": r"\b(?:true|false|null)\b"}]}

grammars["toml"] = {"scopeName": "source.toml", "name": "TOML", "patterns": [
    {"name": "comment.line", "match": r"#.*$"},
    {"name": "entity.name.type", "match": r"^\s*\[\[?[^\]]+\]\]?"},
    {"name": "string.quoted.triple", "begin": "'''|\"\"\"", "end": "'''|\"\"\""},
    {"name": "support.type.property-name", "match": r"^\s*[A-Za-z0-9_.\-]+(?=\s*=)"},
    {"name": "string.quoted.double", "begin": '"', "end": '"', "patterns": [ESC]},
    {"name": "string.quoted.single", "begin": "'", "end": "'"},
    NUM,
    {"name": "keyword.operator", "match": "="},
    {"name": "constant.language", "match": r"\b(?:true|false)\b"}]}

grammars["properties"] = {"scopeName": "source.properties", "name": "Properties", "patterns": [
    {"name": "comment.line", "match": r"^\s*[#!].*$"},
    {"name": "support.type.property-name", "match": r"^\s*[^=:#!\s][^=:\s]*(?=\s*[=:])"},
    {"name": "keyword.operator", "match": r"[=:]"},
    NUM]}

grammars["markdown"] = {"scopeName": "text.html.markdown", "name": "Markdown", "patterns": [
    {"name": "comment.block", "begin": "<!--", "end": "-->"},
    {"name": "markup.heading", "match": r"^#{1,6}\s.*$"},
    {"name": "markup.raw.block", "begin": r"^\s*```.*$", "end": r"^\s*```\s*$"},
    {"name": "markup.inline.raw", "match": r"`[^`\n]+`"},
    {"name": "markup.bold", "match": r"\*\*[^*\n]+\*\*|__[^_\n]+__"},
    {"name": "markup.italic", "match": r"\*[^*\s][^*\n]*\*|\b_[^_\s][^_\n]*_\b"},
    {"match": r"(!?\[)([^\]\n]*)(\])(\()([^)\n]*)(\))",
     "captures": {"1": {"name": "punctuation.definition.tag"}, "2": {"name": "markup.link.text"},
                  "3": {"name": "punctuation.definition.tag"}, "4": {"name": "punctuation.definition.tag"},
                  "5": {"name": "markup.underline.link"}, "6": {"name": "punctuation.definition.tag"}}},
    {"name": "markup.quote", "match": r"^\s*>.*$"},
    {"name": "markup.separator", "match": r"^\s*(?:-{3,}|\*{3,}|_{3,})\s*$"},
    {"match": r"^\s*(?:[-*+]|\d+\.)\s(\[[ xX]\]\s)?",
     "name": "punctuation.definition.list", "captures": {"1": {"name": "constant.language"}}}]}

# id -> (grammar key, folder, comments, brackets/quotes style)
SLASH = {"comments": {"lineComment": "//", "blockComment": ["/*", "*/"]}}
HASH = {"comments": {"lineComment": "#"}}
BR = [["{", "}"], ["[", "]"], ["(", ")"]]
PAIRS = [{"open": "{", "close": "}"}, {"open": "[", "close": "]"}, {"open": "(", "close": ")"},
         {"open": '"', "close": '"', "notIn": ["string"]}, {"open": "'", "close": "'", "notIn": ["string", "comment"]}]
langs = {
    "java": SLASH, "kotlin": SLASH, "cpp": SLASH, "groovy": SLASH, "json": {"comments": {}},
    "toml": HASH, "properties": HASH, "markdown": {"comments": {"blockComment": ["<!--", "-->"]}},
    "xml": {"comments": {"blockComment": ["<!--", "-->"]}},
}
entries = []
for key, g in grammars.items():
    w(f"{TM}/{key}/syntaxes/{key}.tmLanguage.json", g)
    cfg = {"comments": langs[key]["comments"], "brackets": BR, "autoClosingPairs": PAIRS, "surroundingPairs": BR}
    if key == "xml":
        cfg["brackets"] = [["<", ">"]] + BR
        cfg["autoClosingPairs"] = PAIRS[:3] + [{"open": '"', "close": '"'}, {"open": "<!--", "close": "-->"}]
    w(f"{TM}/{key}/language-configuration.json", cfg)
    entries.append({"name": key, "scopeName": g["scopeName"],
                    "grammar": f"textmate/{key}/syntaxes/{key}.tmLanguage.json",
                    "languageConfiguration": f"textmate/{key}/language-configuration.json"})
w(f"{TM}/languages.json", {"languages": entries})

# ---- themes (MD3, biru) ----
# Warna latar = token surface MD3 yang sama dengan ui/Theme.kt. Hue token dipilih tonal & harmonis
# dengan primary biru; kontras >= 4.5:1 terhadap latar.
def theme(name, typ, bg, fg, line_hl, sel, gutter, gutter_active, c):
    tok = [
        ("comment", c["comment"], "italic"),
        ("string", c["string"], None),
        ("string.quoted.other", c["string"], None),
        ("constant.numeric", c["num"], None),
        ("constant.language", c["const"], None),
        ("constant.other", c["const"], None),
        ("constant.other.reference", c["const"], None),
        ("constant.character.entity", c["esc"], None),
        ("constant.character.escape", c["esc"], None),
        ("keyword", c["kw"], None),
        ("keyword.control", c["kw"], "bold"),
        ("keyword.declaration", c["kw"], None),
        ("keyword.operator", c["op"], None),
        ("storage", c["kw"], None),
        ("storage.modifier", c["mod"], None),
        ("storage.type", c["type"], None),
        ("storage.type.primitive", c["type"], None),
        ("storage.type.annotation", c["ann"], None),
        ("entity.name.type", c["type"], None),
        ("entity.name.namespace", c["prop"], None),
        ("entity.name.function", c["fn"], None),
        ("entity.name.label", c["ann"], None),
        ("support.function", c["fn"], None),
        ("entity.name.tag", c["kw"], None),
        ("entity.other.attribute-name", c["attr"], None),
        ("entity.other.attribute-name.namespace", c["prop"], None),
        ("support.type.property-name", c["attr"], None),
        ("variable.other.property", c["prop"], None),
        ("variable.other.interpolation", c["const"], None),
        ("punctuation.definition.tag", c["punct"], None),
        ("meta.preprocessor", c["ann"], None),
        ("keyword.control.directive", c["ann"], None),
        ("markup.heading", c["kw"], "bold"),
        ("markup.bold", c["fn"], "bold"),
        ("markup.italic", c["fn"], "italic"),
        ("markup.inline.raw", c["string"], None),
        ("markup.raw.block", c["string"], None),
        ("markup.link.text", c["type"], None),
        ("markup.underline.link", c["attr"], "underline"),
        ("markup.quote", c["comment"], "italic"),
        ("markup.separator", c["punct"], None),
        ("punctuation.definition.list", c["kw"], None),
    ]
    return {"name": name, "type": typ,
        "colors": {"editor.background": bg, "editor.foreground": fg, "editor.lineHighlightBackground": line_hl,
                   "editor.selectionBackground": sel, "editorCursor.foreground": c["cursor"],
                   "editorLineNumber.foreground": gutter, "editorLineNumber.activeForeground": gutter_active},
        "tokenColors": [{"settings": {"foreground": fg, "background": bg}}] + [{"scope": s, "settings": ({"foreground": col, "fontStyle": st} if st else {"foreground": col})} for s, col, st in tok]}

dark = dict(comment="#8E9099", string="#A8DAB5", num="#FFB784", kw="#ADC6FF", mod="#8FB0F5", op="#C4C6D0",
            const="#D6BBFB", type="#7FD6E6", fn="#F1DC9C", ann="#F0B8D8", attr="#9CCBFB", prop="#C3D3F5",
            esc="#7FD6E6", punct="#9EA3AE", cursor="#ADC6FF")
light = dict(comment="#6B6F78", string="#1B6B3A", num="#9A4A00", kw="#0B57D0", mod="#3C6FD6", op="#44474E",
             const="#6B4FA3", type="#006874", fn="#7A5900", ann="#9C3D72", attr="#00629D", prop="#3A4F7A",
             esc="#006874", punct="#5F6368", cursor="#0B57D0")
w(f"{TM}/themes/dark.json",   theme("Sally Dark",   "dark",  "#111318", "#E2E2E9", "#1D2024", "#1F3B6E", "#8E9099", "#ADC6FF", dark))
w(f"{TM}/themes/amoled.json", theme("Sally AMOLED", "dark",  "#000000", "#E2E2E9", "#12141A", "#1F3B6E", "#74777F", "#ADC6FF", dark))
w(f"{TM}/themes/light.json",  theme("Sally Light",  "light", "#F9F9FF", "#191C20", "#EDEDF4", "#D8E2FF", "#74777F", "#0B57D0", light))

# ---- snippets (format VSCode) ----
def S(prefix, body, desc=""): return {"prefix": prefix, "body": body.split("\n"), "description": desc}
snip = {
"kotlin": {
 "Function": S("fun", "fun ${1:name}($2): ${3:Unit} {\n\t$0\n}", "Fungsi"),
 "Main": S("main", "fun main() {\n\t$0\n}", "main()"),
 "Class": S("class", "class ${1:Name}($2) {\n\t$0\n}", "Class"),
 "Data class": S("data", "data class ${1:Name}(val ${2:id}: ${3:Int})", "Data class"),
 "If / else": S("ifelse", "if (${1:cond}) {\n\t$2\n} else {\n\t$0\n}", "if-else"),
 "When": S("when", "when (${1:x}) {\n\t${2:a} -> $3\n\telse -> $0\n}", "when"),
 "For": S("for", "for (${1:i} in ${2:0 until 10}) {\n\t$0\n}", "for loop"),
 "Try/catch": S("try", "try {\n\t$1\n} catch (e: ${2:Exception}) {\n\t$0\n}", "try-catch"),
 "Log.d": S("logd", "Log.d(\"${1:TAG}\", \"$2\")", "Log debug"),
 "Composable": S("comp", "@Composable\nfun ${1:Name}(modifier: Modifier = Modifier) {\n\t$0\n}", "Compose function"),
 "Activity": S("activity", "class ${1:Main}Activity : ComponentActivity() {\n\toverride fun onCreate(savedInstanceState: Bundle?) {\n\t\tsuper.onCreate(savedInstanceState)\n\t\t$0\n\t}\n}", "Activity"),
 "Fabric init": S("fabricinit", "object ${1:ModName} : ModInitializer {\n\toverride fun onInitialize() {\n\t\t$0\n\t}\n}", "Fabric ModInitializer"),
},
"java": {
 "main": S("main", "public static void main(String[] args) {\n\t$0\n}", "main"),
 "Class": S("class", "public class ${1:Name} {\n\t$0\n}", "Class"),
 "sout": S("sout", "System.out.println($0);", "println"),
 "fori": S("fori", "for (int ${1:i} = 0; $1 < ${2:n}; $1++) {\n\t$0\n}", "for index"),
 "foreach": S("foreach", "for (${1:Type} ${2:item} : ${3:items}) {\n\t$0\n}", "for-each"),
 "try": S("try", "try {\n\t$1\n} catch (${2:Exception} e) {\n\t$0\n}", "try-catch"),
 "if": S("ifelse", "if (${1:cond}) {\n\t$2\n} else {\n\t$0\n}", "if-else"),
 "Override": S("ov", "@Override\npublic ${1:void} ${2:name}($3) {\n\t$0\n}", "@Override"),
 "Mixin": S("mixin", "@Mixin(${1:Target}.class)\npublic class ${1:Target}Mixin {\n\t$0\n}", "Mixin Fabric"),
},
"cpp": {
 "main": S("main", "int main(int argc, char** argv) {\n\t$0\n\treturn 0;\n}", "main"),
 "include": S("inc", "#include <${1:iostream}>", "#include"),
 "for": S("for", "for (int ${1:i} = 0; $1 < ${2:n}; ++$1) {\n\t$0\n}", "for"),
 "class": S("class", "class ${1:Name} {\npublic:\n\t$1();\n\t~$1();\nprivate:\n\t$0\n};", "class"),
 "JNI": S("jni", "extern \"C\" JNIEXPORT ${1:jstring} JNICALL\nJava_${2:com_example_app_Native}_${3:method}(JNIEnv* env, jobject thiz) {\n\t$0\n}", "Fungsi JNI"),
 "Log": S("logi", "__android_log_print(ANDROID_LOG_INFO, \"${1:TAG}\", \"$2\");", "Android log"),
 "guard": S("guard", "#pragma once\n$0", "pragma once"),
},
"xml": {
 "LinearLayout": S("linear", "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n\tandroid:layout_width=\"match_parent\"\n\tandroid:layout_height=\"match_parent\"\n\tandroid:orientation=\"vertical\">\n\t$0\n</LinearLayout>", "LinearLayout"),
 "TextView": S("tv", "<TextView\n\tandroid:layout_width=\"wrap_content\"\n\tandroid:layout_height=\"wrap_content\"\n\tandroid:text=\"$1\" />", "TextView"),
 "Button": S("btn", "<Button\n\tandroid:id=\"@+id/${1:button}\"\n\tandroid:layout_width=\"wrap_content\"\n\tandroid:layout_height=\"wrap_content\"\n\tandroid:text=\"$2\" />", "Button"),
 "Activity (manifest)": S("act", "<activity\n\tandroid:name=\".${1:MainActivity}\"\n\tandroid:exported=\"${2:false}\" />", "<activity>"),
 "Permission": S("perm", "<uses-permission android:name=\"android.permission.${1:INTERNET}\" />", "uses-permission"),
 "String": S("str", "<string name=\"${1:name}\">$2</string>", "string resource"),
},
"groovy": {
 "dependencies": S("deps", "dependencies {\n\timplementation(\"${1:group:artifact:version}\")$0\n}", "dependencies"),
 "plugins": S("plugins", "plugins {\n\tid(\"${1:com.android.application}\")$0\n}", "plugins"),
 "repositories": S("repos", "repositories {\n\tgoogle()\n\tmavenCentral()\n}", "repositories"),
 "android": S("android", "android {\n\tnamespace = \"${1:com.example.app}\"\n\tcompileSdk = ${2:35}\n\t$0\n}", "android block"),
 "task": S("task", "tasks.register(\"${1:name}\") {\n\tdoLast {\n\t\t$0\n\t}\n}", "task"),
},
"markdown": {
 "Link": S("link", "[${1:teks}](${2:url})", "Link"),
 "Image": S("img", "![${1:alt}](${2:url})", "Gambar"),
 "Code block": S("code", "```${1:kotlin}\n$0\n```", "Code block"),
 "Checklist": S("todo", "- [ ] $0", "Checklist"),
},
}
for lang, d in snip.items():
    w(f"{OUT}/snippets/{lang}.json", d)
print("assets generated")
