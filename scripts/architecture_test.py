"""Keep clean architecture package dependencies pointed toward the domain."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/ir/rosheno/launcher"
PACKAGE = "ir.rosheno.launcher"


class ArchitectureTest(unittest.TestCase):
    def test_domain_has_no_framework_or_outer_layer_dependencies(self):
        for source in (SOURCE / "domain").rglob("*.kt"):
            for dependency in re.findall(r"^import (\S+)", source.read_text(), re.MULTILINE):
                with self.subTest(source=source.name, dependency=dependency):
                    self.assertTrue(dependency.startswith(("java.", "kotlin.", PACKAGE + ".domain.")))

    def test_data_and_presentation_do_not_import_each_other(self):
        for layer, forbidden in [("data", "presentation"), ("presentation", "data")]:
            for source in (SOURCE / layer).rglob("*.kt"):
                with self.subTest(source=source.name):
                    self.assertNotRegex(source.read_text(), rf"(?m)^import {re.escape(PACKAGE)}\.{forbidden}\.")

    def test_package_declarations_match_directories(self):
        for source in SOURCE.rglob("*.kt"):
            suffix = ".".join(source.relative_to(SOURCE).parent.parts)
            expected = PACKAGE + ("." + suffix if suffix else "")
            with self.subTest(source=source.name):
                self.assertRegex(source.read_text(), rf"(?m)^package {re.escape(expected)}$")

    def test_viewmodels_do_not_depend_on_compose(self):
        for source in (SOURCE / "presentation").rglob("*ViewModel.kt"):
            with self.subTest(source=source.name):
                self.assertNotRegex(source.read_text(), r"(?m)^import androidx\.compose\.")


if __name__ == "__main__":
    unittest.main()
