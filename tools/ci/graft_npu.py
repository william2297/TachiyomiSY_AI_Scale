#!/usr/bin/env python3
"""Graft the Qualcomm NPU pieces of a released APK onto a CI-built APK.

The QNN native code is only compiled when the Qualcomm AI Runtime SDK headers are available, and the 15 FP16
model files (.dlc) are not stored in Git. Without them a CI build has no NPU backend. The upstream release APK
contains both, so this script copies from it:

  lib/arm64-v8a/libwaifu2x-jni.so   native library with the QNN backend compiled in
  lib/arm64-v8a/libQnnModelDlc.so   DLC loader used to prepare models on the device
  assets/qnn-models/*               the 15 FP16 models and their manifest

The native library must come from the same upstream version this fork is based on, because the Kotlin side calls
into it through JNI. Only Kotlin and resource changes are safe to build in CI with this approach; if C++ files
under app/src/main/cpp change, this graft no longer matches.

Usage: graft_npu.py <ci-built.apk> <release.apk> <output.apk>
The output is unsigned and unaligned: run zipalign and apksigner on it afterwards.
"""
import sys
import zipfile

GRAFT_FILES = {
    "lib/arm64-v8a/libwaifu2x-jni.so",
    "lib/arm64-v8a/libQnnModelDlc.so",
}
GRAFT_PREFIX = "assets/qnn-models/"
EXPECTED_MODELS = 15

# Parts this build never uses are left out to make the APK smaller (about 50 MB compressed):
# - HTP skel/stub libraries for other Snapdragon generations. This build targets HTP v81 (Snapdragon 8 Gen 5,
#   the Oppo Pad Mini). Remove a line to support another generation.
# - MediaTek NPU libraries (the MediaTek backend is hidden).
# - Models for the Vulkan-only enhancement options that are hidden in the settings.
DROP_PREFIXES = (
    "lib/arm64-v8a/libQnnHtpV69",
    "lib/arm64-v8a/libQnnHtpV73",
    "lib/arm64-v8a/libQnnHtpV75",
    "lib/arm64-v8a/libQnnHtpV79",
    "lib/arm64-v8a/libLiteRt",
    "lib/arm64-v8a/liblitert_jni",
    "assets/waifu2x-models/",
    "assets/waifu2x-models-nose/",
    "assets/waifu2x-models-upconv7/",
    "assets/animejanai-ncnn-vulkan/",
    "assets/sudo-ultracompact/",
)
SIGNATURE_SUFFIXES = (".SF", ".RSA", ".DSA", ".EC")


def is_signature_entry(name: str) -> bool:
    return name == "META-INF/MANIFEST.MF" or (name.startswith("META-INF/") and name.endswith(SIGNATURE_SUFFIXES))


def main(ci_apk: str, release_apk: str, output_apk: str) -> None:
    with zipfile.ZipFile(release_apk) as release:
        release_names = set(release.namelist())
        missing = [name for name in GRAFT_FILES if name not in release_names]
        models = [n for n in release_names if n.startswith(GRAFT_PREFIX) and n.endswith(".dlc")]
        if missing or len(models) != EXPECTED_MODELS or GRAFT_PREFIX + "manifest.json" not in release_names:
            sys.exit(f"Release APK is missing NPU parts: files={missing}, models={len(models)}/{EXPECTED_MODELS}")

        with zipfile.ZipFile(ci_apk) as ci, zipfile.ZipFile(output_apk, "w") as out:
            if "lib/arm64-v8a/libwaifu2x-jni.so" not in ci.namelist():
                sys.exit("CI APK has no arm64-v8a libwaifu2x-jni.so; wrong APK?")
            kept = 0
            dropped = 0
            for info in ci.infolist():
                name = info.filename
                if is_signature_entry(name) or name in GRAFT_FILES or name.startswith(GRAFT_PREFIX):
                    continue
                if name.startswith(DROP_PREFIXES):
                    dropped += 1
                    continue
                out.writestr(info, ci.read(name), compress_type=info.compress_type)
                kept += 1
            grafted = 0
            for name in sorted(release_names):
                if name in GRAFT_FILES or name.startswith(GRAFT_PREFIX):
                    out.writestr(name, release.read(name), compress_type=zipfile.ZIP_DEFLATED)
                    grafted += 1
    print(
        f"kept {kept} entries from the CI APK, dropped {dropped} unused ones, "
        f"grafted {grafted} from the release APK -> {output_apk}",
    )


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:])
