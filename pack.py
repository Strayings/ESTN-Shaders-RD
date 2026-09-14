import json
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parent
EXCLUDE = {"textures/environment"}
INCLUDE = ["manifest.json", "pack_icon.png", "LICENSE.txt", "biomes_client.json", "splashes.json", "loading_messages.json", "renderer", "textures", "ui", "shaders", "materials"]


def main():
    version = ".".join(map(str, json.loads((ROOT / "manifest.json").read_text())["header"]["version"]))
    out = ROOT / "build" / f"ESTN-Shaders-{version}.mcpack"
    out.parent.mkdir(exist_ok=True)
    bins = sorted((ROOT / "renderer" / "materials").glob("*.material.bin"))
    if not bins:
        raise SystemExit("no renderer/materials/*.material.bin, run build.bat first")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for name in INCLUDE:
            p = ROOT / name
            if p.is_file():
                z.write(p, name)
            elif p.is_dir():
                for f in sorted(p.rglob("*")):
                    rel = f.relative_to(ROOT).as_posix()
                    if f.is_file() and not any(rel.startswith(e + "/") for e in EXCLUDE):
                        z.write(f, rel)
    print(f"{out} ({out.stat().st_size // 1024} KiB, {len(bins)} materials)")


if __name__ == "__main__":
    main()
