#!/usr/bin/env python3
"""Generate minimal TextMate grammars, language configs, editor themes and bundled snippets.
Grammar sederhana ini cukup untuk Fase 1. Ganti dengan grammar VSCode penuh kapan saja
(taruh di assets/textmate/<lang>/ dan update languages.json)."""
import json, os

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
TM = os.path.join(OUT, "textmate")

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)

def kw(words):
    return r"\b(?:" + "|".join(words) + r")\b"

ESC = {"name": "constant.character.escape", "match": r"\\."}
NUM = {"name": "constant.numeric",
       "match": r"\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[fFLlDdUu]*)\b"}

def dq(): return {"name": "string.quoted.double", "begin": '"', "end": '"', "patterns": [ESC]}
def sq(): return {"name": "string.quoted.single", "begin": "'", "end": "'", "patterns": [ESC]}
def blk(): return {"name": "comment.block", "begin": r"/\*", "end": r"\*/"}
def line(tok="//"): return {"name": "comment.line", "match": tok + r".*$"}

def clike(scope, name, kws, types, consts, extra_before=(), extra_after=()):
    pats = list(extra_before) + [blk(), line(), dq(), sq(), NUM,
        {"name": "storage.type.annotation", "match": r"@[A-Za-z_][\w.]*"},
        {"name": "keyword.control", "match": kw(kws)},
        {"name": "storage.type", "match": kw(types)},
        {"name": "constant.language", "match": kw(consts)},
    ] + list(extra_after) + [
        {"name": "entity.name.type", "match": r"\b[A-Z][A-Za-z0-9_]*\b"},
        {"name": "entity.name.function", "match": r"\b[A-Za-z_][A-Za-z0-9_]*(?=\s*\()"},
    ]
    return {"scopeName": scope, "name": name, "patterns": pats}

java_kw = "abstract assert break case catch class continue default do else enum extends final finally for if implements import instanceof interface native new package private protected public return static strictfp super switch synchronized this throw throws transient try volatile while var record sealed permits yield".split()
java_ty = "boolean byte char double float int long short void String Object".split()
kt_kw = "as break by catch class companion constructor continue crossinline data do dynamic else enum external fun get if import in infix init inline inner interface internal is it lateinit noinline object open operator out override package private protected public reified return sealed set super suspend tailrec this throw try typealias val var vararg when where while abstract final annotation const field value".split()
kt_ty = "Int Long Short Byte Float Double Boolean Char String Unit Any Nothing Array List Map Set".split()
cpp_kw = "alignas alignof asm auto break case catch class const constexpr const_cast continue decltype default delete do dynamic_cast else enum explicit export extern for friend goto if inline mutable namespace new noexcept operator private protected public register reinterpret_cast return sizeof static static_assert static_cast struct switch template this throw try typedef typeid typename union using virtual volatile while override final".split()
cpp_ty = "bool char double float int long short signed unsigned void size_t int8_t int16_t int32_t int64_t uint8_t uint16_t uint32_t uint64_t wchar_t char16_t char32_t".split()
gr_kw = "as assert break case catch class continue def default else enum extends final finally for if implements import in instanceof interface new package private protected public return static super switch this throw throws try while".split()
gr_dsl = "plugins dependencies android repositories apply implementation api compileOnly runtimeOnly testImplementation androidTestImplementation kapt ksp id version buildscript allprojects subprojects task tasks ext include pluginManagement".split()

grammars = {}
grammars["java"] = clike("source.java", "Java", java_kw, java_ty, ["true", "false", "null"])
grammars["kotlin"] = clike("source.kotlin", "Kotlin", kt_kw, kt_ty, ["true", "false", "null"],
    extra_before=[{"name": "string.quoted.triple", "begin": '"""', "end": '"""'}])
grammars["cpp"] = clike("source.cpp", "C/C++", cpp_kw, cpp_ty, ["true", "false", "nullptr", "NULL"],
    extra_before=[{"name": "keyword.control.directive", "match": r"^\s*#\s*\w+"}],
    extra_after=[{"name": "string.quoted.other", "match": r"<[\w./]+>"}])
grammars["groovy"] = clike("source.groovy", "Gradle (Groovy/KTS)", gr_kw, ["String", "Integer", "Boolean", "List", "Map"],
    ["true", "false", "null"],
    extra_before=[{"name": "string.quoted.triple", "begin": "'''|\"\"\"", "end": "'''|\"\"\""}],
    extra_after=[{"name": "support.function", "match": kw(gr_dsl)}])

grammars["xml"] = {"scopeName": "text.xml", "name": "XML", "patterns": [
    {"name": "comment.block", "begin": "<!--", "end": "-->"},
    {"name": "string.unquoted.cdata", "begin": r"<!\[CDATA\[", "end": r"\]\]>"},
    {"name": "meta.preprocessor", "begin": r"<\?", "end": r"\?>"},
    {"begin": r"(</?)([A-Za-z_][\w:.-]*)", "end": r"/?>",
     "beginCaptures": {"1": {"name": "punctuation.definition.tag"}, "2": {"name": "entity.name.tag"}},
     "patterns": [
        {"name": "entity.other.attribute-name", "match": r"[A-Za-z_:][\w:.-]*(?=\s*=)"},
        {"name": "string.quoted.double", "begin": '"', "end": '"'},
        {"name": "string.quoted.single", "begin": "'", "end": "'"}]},
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
    {"name": "constant.language", "match": r"\b(?:true|false)\b"}]}

grammars["properties"] = {"scopeName": "source.properties", "name": "Properties", "patterns": [
    {"name": "comment.line", "match": r"^\s*[#!].*$"},
    {"name": "support.type.property-name", "match": r"^\s*[^=:#!\s][^=:\s]*(?=\s*[=:])"},
    NUM]}

grammars["markdown"] = {"scopeName": "text.html.markdown", "name": "Markdown", "patterns": [
    {"name": "markup.heading", "match": r"^#{1,6}\s.*$"},
    {"name": "markup.raw.block", "begin": r"^\s*```.*$", "end": r"^\s*```\s*$"},
    {"name": "markup.inline.raw", "match": r"`[^`\n]+`"},
    {"name": "markup.bold", "match": r"\*\*[^*\n]+\*\*|__[^_\n]+__"},
    {"name": "markup.italic", "match": r"\*[^*\s][^*\n]*\*|\b_[^_\s][^_\n]*_\b"},
    {"name": "markup.underline.link", "match": r"\[[^\]\n]*\]\([^)\n]*\)"},
    {"name": "markup.quote", "match": r"^\s*>.*$"},
    {"name": "punctuation.definition.list", "match": r"^\s*(?:[-*+]|\d+\.)\s"},
    {"name": "comment.block", "begin": "<!--", "end": "-->"}]}

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

# ---- themes ----
def theme(name, typ, bg, fg, line_hl, sel, gutter, c):
    tok = [
        ("comment", c["comment"], "italic"), ("string", c["string"], None), ("constant.numeric", c["num"], None),
        ("constant.language", c["kw2"], None), ("constant.character.escape", c["esc"], None),
        ("keyword", c["kw"], None), ("storage", c["kw"], None), ("storage.type", c["type"], None),
        ("storage.type.annotation", c["ann"], None), ("entity.name.type", c["type"], None),
        ("entity.name.function", c["fn"], None), ("support.function", c["fn"], None),
        ("entity.name.tag", c["kw"], None), ("entity.other.attribute-name", c["attr"], None),
        ("support.type.property-name", c["attr"], None), ("punctuation.definition.tag", c["punct"], None),
        ("meta.preprocessor", c["kw"], None), ("keyword.control.directive", c["kw"], None),
        ("markup.heading", c["kw"], "bold"), ("markup.bold", c["fn"], "bold"), ("markup.italic", c["fn"], "italic"),
        ("markup.inline.raw", c["string"], None), ("markup.raw.block", c["string"], None),
        ("markup.underline.link", c["attr"], "underline"), ("markup.quote", c["comment"], "italic"),
        ("punctuation.definition.list", c["kw"], None),
    ]
    return {"name": name, "type": typ,
        "colors": {"editor.background": bg, "editor.foreground": fg, "editor.lineHighlightBackground": line_hl,
                   "editor.selectionBackground": sel, "editorCursor.foreground": c["cursor"],
                   "editorLineNumber.foreground": gutter, "editorLineNumber.activeForeground": fg},
        "tokenColors": [{"scope": s, "settings": ({"foreground": col, "fontStyle": st} if st else {"foreground": col})} for s, col, st in tok]}

dark = dict(comment="#6A9955", string="#CE9178", num="#B5CEA8", kw="#C586C0", kw2="#569CD6", type="#4EC9B0", fn="#DCDCAA",
            ann="#DCDCAA", attr="#9CDCFE", esc="#D7BA7D", punct="#808080", cursor="#AEAFAD")
light = dict(comment="#008000", string="#A31515", num="#098658", kw="#AF00DB", kw2="#0000FF", type="#267F99", fn="#795E26",
             ann="#795E26", attr="#001080", esc="#EE0000", punct="#800000", cursor="#000000")
w(f"{TM}/themes/dark.json", theme("Dark+", "dark", "#1E1E1E", "#D4D4D4", "#2A2D2E", "#264F78", "#858585", dark))
w(f"{TM}/themes/amoled.json", theme("AMOLED", "dark", "#000000", "#D4D4D4", "#141414", "#264F78", "#6E6E6E", dark))
w(f"{TM}/themes/light.json", theme("Light+", "light", "#FFFFFF", "#1F1F1F", "#F3F3F3", "#ADD6FF", "#6E7681", light))

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
