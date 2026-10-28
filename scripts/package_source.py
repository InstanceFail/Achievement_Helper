"""Package a clean GitHub-ready source tree, excluding caches, JDKs and saves."""
from pathlib import Path
import zipfile
import re

root = Path(__file__).resolve().parents[1]
version = re.search(r"^mod_version=(.+)$", (root / "gradle.properties").read_text(encoding="utf-8"), re.MULTILINE).group(1).strip()
output = root / f"build/distributions/Achievement_Helper-{version}-source.zip"
output.parent.mkdir(parents=True, exist_ok=True)
folders = ("src", "content", "scripts", "gradle", ".github", "docs")
files = [p for name in folders for p in (root / name).rglob("*") if p.is_file()]
files += [p for p in root.iterdir() if p.is_file() and p.name not in ("AGENTS.md",)]
with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(files):
        if "__pycache__" not in path.parts:
            archive.write(path, "Achievement_Helper/" + path.relative_to(root).as_posix())
print(output)
