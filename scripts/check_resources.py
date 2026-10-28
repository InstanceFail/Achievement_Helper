"""Check translation coverage, placeholders, and optional vanilla JAR coverage."""
import csv
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / "src/main/resources/assets/achievement_helper/lang"
languages = {name: json.loads((LANG / f"{name}.json").read_text(encoding="utf-8"))
             for name in ("en_us", "zh_cn")}
assert languages["en_us"].keys() == languages["zh_cn"].keys(), "Language keys differ"
for key, english in languages["en_us"].items():
    chinese = languages["zh_cn"][key]
    assert english and chinese, f"Empty text: {key}"
    assert re.findall(r"%(?:\d+\$)?[sd]|%%", english) == re.findall(r"%(?:\d+\$)?[sd]|%%", chinese), key

for filename, prefix in [("ui.tsv", ""), ("guides.tsv", "guide.achievement_helper.minecraft.")]:
    with (ROOT / "content" / filename).open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream, delimiter="\t"))
    assert len(rows) == len({row["id"] for row in rows}), f"Duplicate IDs: {filename}"
    for row in rows:
        key = prefix + row["id"].replace("/", ".")
        for name, values in languages.items():
            assert values[key] == row[name], f"Regenerate language resources: {key} ({name})"

guides = {key.removeprefix("guide.achievement_helper.minecraft.").replace(".", "/")
          for key in languages["en_us"] if key.startswith("guide.achievement_helper.minecraft.")}
assert len(guides) == 125, f"Expected all 125 vanilla 26.1.2 guides, got {len(guides)}"
if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as archive:
        prefix = "data/minecraft/advancement/"
        vanilla = {name.removeprefix(prefix).removesuffix(".json") for name in archive.namelist()
                   if name.startswith(prefix) and name.endswith(".json")
                   and "display" in json.loads(archive.read(name))}
    assert guides == vanilla, f"Guide coverage mismatch. Missing: {vanilla - guides}; extra: {guides - vanilla}"
print(f"PASS: {len(languages['en_us'])} bilingual keys, matching placeholders, 125 guides, generated resources current.")
