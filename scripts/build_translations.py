"""Regenerate checked-in language resources. Python 3, standard library only."""
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / "src/main/resources/assets/achievement_helper/lang"


def main():
    translations = {"en_us": {}, "zh_cn": {}}
    for filename, prefix in [("ui.tsv", ""), ("guides.tsv", "guide.achievement_helper.minecraft.")]:
        with (ROOT / "content" / filename).open(encoding="utf-8", newline="") as stream:
            for row in csv.DictReader(stream, delimiter="\t"):
                key = prefix + row["id"].replace("/", ".")
                for language, values in translations.items():
                    if key in values or not row[language].strip():
                        raise ValueError(f"Duplicate or empty translation: {key} ({language})")
                    values[key] = row[language]
    LANG.mkdir(parents=True, exist_ok=True)
    for language, values in translations.items():
        (LANG / f"{language}.json").write_text(
            json.dumps(values, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(translations['en_us'])} keys in each language.")


if __name__ == "__main__":
    main()
