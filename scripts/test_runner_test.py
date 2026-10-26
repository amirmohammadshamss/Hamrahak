"""Host-side checks for instrumentation results (no emulator required)."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("android_runner", Path(__file__).with_name("test-android.py"))
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class InstrumentationResultTest(unittest.TestCase):
    def test_accepts_single_and_multiple_successes(self):
        self.assertEqual(1, runner.passed_count("Time: 1\nOK (1 test)\n"))
        self.assertEqual(41, runner.passed_count("Time: 1\nOK (41 tests)\n"))

    def test_rejects_failure_crash_or_missing_result_even_with_zero_adb_exit(self):
        for result in ["FAILURES!!!\nTests run: 1", "INSTRUMENTATION_FAILED", "Process crashed.",
                       "INSTRUMENTATION_RESULT: shortMsg=crash", "", "OK (0 tests)\n",
                       "OK (1 test)\nFAILURES!!!"]:
            with self.subTest(result=result), self.assertRaises(RuntimeError):
                runner.passed_count(result)


if __name__ == "__main__":
    unittest.main()
