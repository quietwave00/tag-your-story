import importlib.util
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("replay-enrichment-diagnostics.py")
SPEC = importlib.util.spec_from_file_location("replay_enrichment_diagnostics", SCRIPT)
REPLAY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REPLAY)


class ReplayDiagnosticsTest(unittest.TestCase):
    def test_manifest_chooses_completed_baseline_and_ignores_newer_run(self):
        events = [
            {"runId": "old-skip", "event": "import_started", "mode": "IMPORT_PIPELINE",
             "time": "2026-11-04T01:00:00Z", "subjectType": "TRACK",
             "spotifyTrackId": "track-1", "title": "Song", "trackArtists": ["Singer"]},
            {"runId": "old-skip", "event": "import_skipped", "reason": "EMPTY_COMPLETED"},
            {"runId": "old-done", "event": "import_started", "mode": "IMPORT_PIPELINE",
             "time": "2026-11-04T02:00:00Z", "subjectType": "TRACK",
             "spotifyTrackId": "track-1", "title": "Song", "trackArtists": ["Singer"]},
            {"runId": "old-done", "event": "tag_calculation_completed",
             "resolved": {"items": [{"name": "Rock", "score": 0.8}]}},
            {"runId": "new", "event": "import_started", "mode": "IMPORT_PIPELINE",
             "time": "2026-11-06T02:00:00Z", "subjectType": "TRACK",
             "spotifyTrackId": "track-1", "title": "Song"},
            {"runId": "new", "event": "tag_calculation_completed",
             "resolved": {"items": [{"name": "Jazz"}]}},
        ]
        samples = REPLAY.manifest(events, "2026-11-05")
        self.assertEqual(len(samples), 1)
        self.assertEqual(samples[0]["before"]["runId"], "old-done")

    def test_compare_marks_reused_result_incomparable(self):
        before = REPLAY.summarize([
            {"runId": "before", "event": "tag_calculation_completed", "resolved": {"items": []}}
        ])
        after = REPLAY.summarize([
            {"runId": "after", "event": "import_skipped", "reason": "ALREADY_RESOLVED"}
        ])
        self.assertEqual(REPLAY.compare(before, after)["status"], "REUSED_EXISTING")

    def test_album_start_uses_top_level_title_and_artists(self):
        samples = REPLAY.manifest([
            {"runId": "album", "event": "import_started", "mode": "IMPORT_PIPELINE",
             "time": "2026-11-04T01:00:00Z", "subjectType": "ALBUM",
             "spotifyAlbumId": "album-1", "albumTitle": "Album", "albumArtists": ["Singer"]},
            {"runId": "album", "event": "tag_calculation_completed", "resolved": {"items": []}},
        ], "2026-11-05")
        self.assertEqual(samples[0]["title"], "Album")
        self.assertEqual(samples[0]["artists"], ["Singer"])

    def test_provider_and_resolved_changes_are_separate(self):
        before = REPLAY.summarize([
            {"runId": "before", "event": "provider_completed", "provider": "LASTFM",
             "status": "SUCCESS", "trackTags": {"items": [{"name": "Pop"}]}},
            {"runId": "before", "event": "tag_calculation_completed", "resolved": {"items": []}}
        ])
        after = REPLAY.summarize([
            {"runId": "after", "event": "provider_completed", "provider": "LASTFM",
             "status": "SUCCESS", "trackTags": {"items": [{"name": "Pop"}]}},
            {"runId": "after", "event": "tag_calculation_completed",
             "resolved": {"items": [{"name": "Pop", "score": 0.65}]}}
        ])
        comparison = REPLAY.compare(before, after)
        self.assertEqual(comparison["resolvedAdded"], ["Pop"])
        self.assertEqual(comparison["providers"]["LASTFM"]["trackTagsAdded"], [])


if __name__ == "__main__":
    unittest.main()
