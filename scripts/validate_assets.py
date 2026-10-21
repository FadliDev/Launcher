#!/usr/bin/env python3
"""Validate bundled TextMate and VS Code-style snippet JSON before Android builds."""
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
errors = []
json_files = list(ASSETS.rglob("*.json"))
for path in json_files:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"{path.relative_to(ROOT)}: invalid JSON: {exc}")
        continue

languages_path = ASSETS / "textmate" / "languages.json"
languages = json.loads(languages_path.read_text(encoding="utf-8")).get("languages", [])
for lang in languages:
    grammar = ASSETS / lang.get("grammar", "")
    if not grammar.is_file():
        errors.append(f"Missing grammar for {lang.get('name')}: {grammar.relative_to(ROOT)}")

snippets_dir = ASSETS / "snippets"
for path in snippets_dir.glob("*.json"):
    try:
        content = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(content, dict):
            errors.append(f"{path.relative_to(ROOT)}: snippet root must be an object")
            continue
        for name, snippet in content.items():
            if not isinstance(snippet, dict):
                errors.append(f"{path.relative_to(ROOT)}: {name} must be an object")
                continue
            if "prefix" not in snippet or "body" not in snippet:
                errors.append(f"{path.relative_to(ROOT)}: {name} needs prefix and body")
    except Exception as exc:
        errors.append(f"{path.relative_to(ROOT)}: snippet validation failed: {exc}")

if errors:
    print("Asset validation failed:", file=sys.stderr)
    print("\n".join(f" - {item}" for item in errors), file=sys.stderr)
    sys.exit(1)

print(f"Validated {len(json_files)} JSON assets and {len(languages)} TextMate grammars.")
