#!/usr/bin/env python3
"""Apply the reviewed UI/service refactor to the extracted Android project."""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: refactor_app.py PROJECT_DIR")

repo = Path(__file__).resolve().parent.parent
project = Path(sys.argv[1]).resolve()
app = project / "app/src/main/java/com/example/dronesignal/App.kt"
manifest = project / "app/src/main/AndroidManifest.xml"
source = app.read_text(encoding="utf-8")


def remove_between(text: str, start: str, end: str) -> str:
    begin = text.find(start)
    if begin < 0:
        raise SystemExit(f"Refactor marker not found: {start}")
    finish = text.find(end, begin + len(start))
    if finish < 0:
        raise SystemExit(f"Refactor end marker not found: {end}")
    return text[:begin] + text[finish:]


# Delete FM station configuration.
source = remove_between(source, "data class Station(", "object AlertState {")

# Delete streaming player and the radio-backed service. A dedicated voice-only
# service is copied into the project below; AlertGuard's calls are redirected.
source = remove_between(source, "class StreamPlayer(", "class FastPollService:")
source = source.replace("RadioService", "AlertVoiceService")

# MainActivity uses the new three-section UI; discard the old radio/frequency UI.
source = source.replace("{ AppScreen() }", "{ NewAppScreen() }")
source = remove_between(source, "@Composable\nfun AppScreen()", "\u0000NOT_PRESENT\u0000") if "\u0000NOT_PRESENT\u0000" in source else source[:source.find("@Composable\nfun AppScreen()")]

for obsolete in (
    "import android.media.*\n",
    "import androidx.compose.material.icons.Icons\n",
    "import androidx.compose.material.icons.filled.VolumeUp\n",
    "import androidx.compose.foundation.lazy.itemsIndexed\n",
):
    source = source.replace(obsolete, "")

if "NewAppScreen()" not in source:
    raise SystemExit("MainActivity was not redirected to NewAppScreen")
if any(token in source for token in ("Stations.all", "StreamPlayer(", "VolumeUp", "Дорожное радио")):
    raise SystemExit("Radio UI/player references remain after refactor")

app.write_text(source, encoding="utf-8")

manifest_text = manifest.read_text(encoding="utf-8")
if ".RadioService" not in manifest_text:
    raise SystemExit("Expected .RadioService manifest entry was not found")
manifest.write_text(manifest_text.replace(".RadioService", ".AlertVoiceService"), encoding="utf-8")

package_dir = project / "app/src/main/java/com/example/dronesignal"
for name in ("AlertVoiceService.kt", "UtilitiesScreens.kt"):
    src = repo / "app-redesign" / name
    if not src.is_file():
        raise SystemExit(f"Missing refactor source: {src}")
    (package_dir / name).write_text(src.read_text(encoding="utf-8"), encoding="utf-8")

print("Radio player/stations removed; voice alert service and utilities screens installed.")
