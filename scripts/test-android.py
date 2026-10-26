#!/usr/bin/env python3
"""Run instrumented feature tests in denied/granted phases on a dedicated emulator."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "ir.rosheno.launcher"
TEST_PACKAGE = PACKAGE + ".test"
DENIED = ["ir.rosheno.launcher.ContactsPermissionDeniedTest", "ir.rosheno.launcher.NoPermissionFeaturesTest"]
PERMISSIONS = ["CAMERA", "CALL_PHONE", "READ_CONTACTS", "WRITE_CONTACTS", "READ_SMS", "SEND_SMS"]


def passed_count(output):
    # adb/am may exit zero even when JUnit failed or the instrumentation process crashed.
    if re.search(r"FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=", output):
        raise RuntimeError("Instrumentation failed; inspect the saved phase log")
    match = re.search(r"^OK \((\d+) tests?\)", output, re.MULTILINE)
    if not match or int(match[1]) == 0:
        raise RuntimeError("No successful JUnit result; inspect the saved phase log")
    return int(match[1])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Dedicated emulator serial, e.g. emulator-5554")
    parser.add_argument("--adb", default=shutil.which("adb") or str(Path(os.environ.get("ANDROID_HOME", "")) / "platform-tools/adb"))
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--gradle-init-script")
    parser.add_argument("--large-text", action="store_true", help="Exercise 150%% system font size")
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        parser.error("Use a dedicated Android emulator; these tests exercise camera/provider fixtures")
    report_dir = ROOT / "app/build/reports/feature-tests" / ("large-text" if args.large_text else "default")
    report_dir.mkdir(parents=True, exist_ok=True)
    summary = {"serial": args.serial, "large_text": args.large_text, "phases": [], "restoration_errors": []}
    started = time.monotonic()

    def command(argv, timeout=600):
        result = subprocess.run(argv, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
        if result.returncode:
            raise RuntimeError(f"Command failed ({result.returncode}): {' '.join(map(str, argv))}\n{result.stdout}")
        return result.stdout

    def adb(*argv):
        return command([args.adb, "-s", args.serial, *argv])

    def permission_states():
        package_dump = adb("shell", "dumpsys", "package", PACKAGE)
        return {p: bool(re.search(rf"android\.permission\.{p}: granted=true", package_dump)) for p in PERMISSIONS}

    initial_permissions = None
    initial_settings = {}
    settings = [("global", "window_animation_scale"), ("global", "transition_animation_scale"), ("global", "animator_duration_scale")]
    if args.large_text:
        settings += [("system", "font_scale")]
    try:
        if not args.skip_build:
            gradle = [str(ROOT / "gradlew")]
            if args.gradle_init_script:
                gradle += ["-I", args.gradle_init_script]
            gradle += [":app:assembleDebug", ":app:assembleDebugAndroidTest", ":app:lintDebug", "--console=plain"]
            if args.offline:
                gradle += ["--offline"]
            output = command(gradle)
            (report_dir / "build.log").write_text(output)
            print("Build and lint passed", flush=True)
        if adb("get-state").strip() != "device":
            raise RuntimeError("Emulator is not ready")
        initial_permissions = permission_states()
        for namespace, key in settings:
            initial_settings[(namespace, key)] = adb("shell", "settings", "get", namespace, key).strip()
        adb("install", "-r", str(ROOT / "app/build/outputs/apk/debug/app-debug.apk"))
        adb("install", "-r", str(ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"))
        for namespace, key in settings:
            adb("shell", "settings", "put", namespace, key, "1.5" if key == "font_scale" else "0")
        for permission in PERMISSIONS:
            adb("shell", "pm", "revoke", PACKAGE, "android.permission." + permission)
        for phase, selection in [("denied", ["-e", "class", ",".join(DENIED)]),
                                 ("features", ["-e", "notClass", ",".join(DENIED)])]:
            print(f"Running {phase} tests…", flush=True)
            output = adb("shell", "am", "instrument", "-w", *selection, TEST_PACKAGE + "/androidx.test.runner.AndroidJUnitRunner")
            (report_dir / f"{phase}.log").write_text(output)
            print(output, flush=True)
            summary["phases"].append({"name": phase, "passed": passed_count(output)})
        summary["success"] = True
    except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
        summary["success"] = False
        summary["error"] = str(error)
        print(str(error), file=sys.stderr, flush=True)
    finally:
        if initial_permissions is not None:
            for permission, granted in initial_permissions.items():
                try:
                    adb("shell", "pm", "grant" if granted else "revoke", PACKAGE, "android.permission." + permission)
                except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
                    summary["restoration_errors"].append(str(error))
        for (namespace, key), value in initial_settings.items():
            try:
                adb("shell", "settings", "delete", namespace, key) if value == "null" else adb("shell", "settings", "put", namespace, key, value)
            except (RuntimeError, OSError, subprocess.TimeoutExpired) as error:
                summary["restoration_errors"].append(str(error))
        summary["elapsed_seconds"] = round(time.monotonic() - started, 2)
        summary["passed"] = sum(phase["passed"] for phase in summary["phases"])
        if summary["restoration_errors"]:
            summary["success"] = False
        (report_dir / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
        print(f"Results: {report_dir / 'summary.json'}", flush=True)
    return 0 if summary.get("success") else 1


if __name__ == "__main__":
    sys.exit(main())
