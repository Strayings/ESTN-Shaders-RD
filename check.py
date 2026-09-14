import itertools
import os
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent
SHADERC = ROOT / ("shaderc.exe" if os.name == "nt" else "shaderc")
PROJ = ROOT / "proj"
INCLUDE = ROOT / "include"

PLATFORMS = {
    "windows": ("windows", "s_5_0"),
    "android": ("android", "310_es"),
    "ios": ("ios", "metal"),
}

PASSES = ["OPAQUE_PASS", "ALPHA_TEST_PASS", "TRANSPARENT_PASS", "DEPTH_ONLY_PASS", "DEPTH_ONLY_OPAQUE_PASS"]
INST = ["INSTANCING__OFF", "INSTANCING__ON"]

FLAG_GROUPS = {
    "RenderChunk": [PASSES, ["SEASONS__OFF", "SEASONS__ON"], INST, ["RENDER_AS_BILLBOARDS__OFF", "RENDER_AS_BILLBOARDS__ON"], ["DITHERING__OFF", "DITHERING__ON"]],
    "Actor": [PASSES, ["EMISSIVE__OFF", "EMISSIVE__EMISSIVE", "EMISSIVE__EMISSIVE_ONLY"], ["CHANGE_COLOR__OFF", "CHANGE_COLOR__ON", "CHANGE_COLOR__MULTI"], ["FANCY__OFF", "FANCY__ON"], INST, ["MASKED_MULTITEXTURE__OFF", "MASKED_MULTITEXTURE__ON"]],
    "ActorBanner": [PASSES, ["EMISSIVE__OFF"], ["CHANGE_COLOR__OFF", "CHANGE_COLOR__MULTI"], ["FANCY__OFF", "FANCY__ON"], INST, ["MASKED_MULTITEXTURE__OFF", "MASKED_MULTITEXTURE__ON"], ["TINTING__DISABLED", "TINTING__ENABLED"]],
    "Particle": [["ALPHA_TEST_PASS", "TRANSPARENT_PASS"], INST],
    "BeaconBeam": [["OPAQUE_PASS", "TRANSPARENT_PASS"], ["FANCY__OFF", "FANCY__ON"]],
    "Sky": [["OPAQUE_PASS"], INST],
    "Clouds": [["TRANSPARENT_PASS"], INST],
    "Stars": [["TRANSPARENT_PASS"], INST],
    "SunMoon": [["TRANSPARENT_PASS"], INST],
    "BlockSelectionOutline": [["OPAQUE_PASS"]],
}


def variants(material, full):
    groups = FLAG_GROUPS.get(material, [["OPAQUE_PASS"]])
    if full:
        return [";".join(c) for c in itertools.product(*groups)]
    n = max(len(g) for g in groups)
    return [";".join(g[i % len(g)] for g in groups) for i in range(n)]


def sampler_defines(shaders):
    import re
    names = []
    for stage in ("vertex", "fragment"):
        src = shaders / f"{stage}.sc"
        if src.is_file():
            names += re.findall(r"SAMPLER\w*_AUTOREG\((\w+)\)", src.read_text())
    return ";".join(f"{n}_REG={i}" for i, n in enumerate(dict.fromkeys(names)))


def compile_one(material, stage, platform, defines):
    device, profile = PLATFORMS[platform]
    shaders = PROJ / material / "shaders"
    src = shaders / f"{stage}.sc"
    regs = sampler_defines(shaders)
    if regs:
        defines = f"{defines};{regs}"
    with tempfile.NamedTemporaryFile(suffix=".bin", delete=False) as tmp:
        out = tmp.name
    args = [str(SHADERC), "-f", str(src), "--platform", device, "-p", profile, "--type", stage,
            "--varyingdef", str(shaders / "varying.def.sc"), "-i", str(INCLUDE), "-i", str(shaders),
            "--define", defines, "-o", out]
    r = subprocess.run(args, capture_output=True, text=True)
    try:
        os.unlink(out)
    except OSError:
        pass
    return r.returncode, (r.stdout + r.stderr).strip()


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    opts = [a for a in sys.argv[1:] if a.startswith("--")]
    platforms = [o.split("=", 1)[1] for o in opts if o.startswith("--platform=")] or list(PLATFORMS)
    materials = args or sorted(p.name for p in PROJ.iterdir() if p.is_dir() and not p.name.startswith(("_", ".")))
    failures = []
    total = 0
    for material in materials:
        shaders = PROJ / material / "shaders"
        if not shaders.is_dir():
            print(f"[skip] {material}: no shaders dir")
            continue
        for platform in platforms:
            for defines in variants(material, full=(platform == "windows" and "--quick" not in opts)):
                for stage in ("vertex", "fragment"):
                    if not (shaders / f"{stage}.sc").is_file():
                        continue
                    total += 1
                    code, log = compile_one(material, stage, platform, defines)
                    if code:
                        failures.append((material, stage, platform, defines, log))
        print(f"{material}: {'FAIL' if any(f[0] == material for f in failures) else 'ok'}")
    for material, stage, platform, defines, log in failures:
        print(f"\n=== FAIL {material}/{stage} [{platform}] {defines}\n{log}")
    print(f"\n{total - len(failures)}/{total} compiled")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
