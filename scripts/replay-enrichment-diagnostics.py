#!/usr/bin/env python3
"""Replay distinct diagnostic subjects through Catalog search and import.

The old JSONL is read only. The API must write its new diagnostic events to a
separate file supplied with --after-log and ENRICHMENT_DIAGNOSTICS_FILE.
"""

import argparse
import json
import sys
import time
from collections import defaultdict
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


def rows(path):
    if not path.exists():
        return []
    result = []
    with path.open(encoding="utf-8") as source:
        lines = source.readlines()
        for number, line in enumerate(lines, 1):
            try:
                result.append(json.loads(line))
            except json.JSONDecodeError:
                if number != len(lines):
                    raise ValueError(f"Invalid JSONL: {path}:{number}")
                # A concurrent writer may leave an unfinished final line.
    return result


def append(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as target:
        target.write(json.dumps(value, ensure_ascii=False, sort_keys=True) + "\n")


def subject_key(event):
    kind = event.get("subjectType")
    identifier = event.get("spotifyTrackId" if kind == "TRACK" else "spotifyAlbumId")
    return (kind, identifier) if kind in ("TRACK", "ALBUM") and identifier else None


def event_groups(events):
    groups = defaultdict(list)
    for event in events:
        if event.get("runId"):
            groups[event["runId"]].append(event)
    return groups


def first(events, name):
    return next((event for event in events if event.get("event") == name), None)


def tags(field):
    if isinstance(field, dict):
        return field.get("items", [])
    return field if isinstance(field, list) else []


def truncated(field):
    return bool(field.get("truncated")) if isinstance(field, dict) else False


def name_of(tag):
    return str(tag.get("name", "")) if isinstance(tag, dict) else str(tag)


def names(field):
    return sorted({name_of(tag) for tag in tags(field) if name_of(tag)}, key=str.casefold)


def summarize(events):
    calculation = first(events, "tag_calculation_completed") or {}
    bootstrap = first(events, "tag_bootstrap_completed") or {}
    preview = first(events, "preview_completed") or {}
    providers = {}
    for event in events:
        if event.get("event") == "provider_completed":
            provider = event.get("provider")
            if provider:
                providers[provider] = {
                    "status": event.get("status"),
                    "trackTags": names(event.get("trackTags")),
                    "albumTags": names(event.get("albumTags")),
                    "trackTagCount": event.get("trackTagCount"),
                    "albumTagCount": event.get("albumTagCount"),
                    "trackTagsTruncated": truncated(event.get("trackTags")),
                    "albumTagsTruncated": truncated(event.get("albumTags")),
                }
    resolved = tags(calculation.get("resolved"))
    return {
        "runId": events[0].get("runId") if events else None,
        "mode": events[0].get("mode") if events else None,
        "skipped": (first(events, "import_skipped") or {}).get("reason"),
        "calculationCompleted": bool(calculation),
        "providers": providers,
        "resolved": sorted(
            [{"name": tag.get("name"), "score": tag.get("score"),
              "reason": tag.get("reason"), "status": tag.get("status")}
             for tag in resolved if isinstance(tag, dict)],
            key=lambda tag: (str(tag["name"]).casefold(), str(tag["reason"])),
        ),
        "resolvedTotalCount": (calculation.get("resolved") or {}).get("totalCount")
                              if isinstance(calculation.get("resolved"), dict) else None,
        "resolvedTruncated": truncated(calculation.get("resolved")),
        "bootstrap": {key: bootstrap.get(key) for key in
                      ("created", "existingMatched", "unmatched", "inputNames")},
        "previewCount": preview.get("count"),
        "resolvedDirectCount": calculation.get("resolvedDirectCount"),
        "resolvedInheritedCount": calculation.get("resolvedInheritedCount"),
        "notableEvents": sorted({event.get("event") for event in events if event.get("event") in
                                 ("musicbrainz_metadata_fallback", "musicbrainz_isrc_404_metadata_match",
                                  "album_evidence_reused", "album_evidence_committed")}),
    }


def manifest(before_events, before_cutoff=None):
    groups = event_groups(before_events)
    selected = {}
    for run_id, events in groups.items():
        start = first(events, "import_started")
        if (not start or start.get("mode") != "IMPORT_PIPELINE" or
                (before_cutoff and start.get("time", "") >= before_cutoff)):
            continue
        key = subject_key(start)
        if not key:
            continue
        complete = first(events, "tag_calculation_completed") is not None
        if key not in selected or (complete and not selected[key]["before"]["calculationCompleted"]):
            album = start.get("album") or {}
            selected[key] = {
                "subjectType": key[0], "spotifyId": key[1],
                "title": start.get("title") or start.get("albumTitle") or album.get("albumTitle"),
                "albumTitle": start.get("albumTitle") or album.get("albumTitle"),
                "artists": start.get("trackArtists") or start.get("albumArtists") or
                           album.get("albumArtists") or [],
                "before": summarize(events),
            }
    return list(selected.values())


def api(base, method, path, params=None, body=None, timeout=30):
    url = base.rstrip("/") + path
    if params:
        url += "?" + urlencode(params)
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = Request(url, data=data, method=method,
                      headers={"Accept": "application/json", "Content-Type": "application/json"})
    try:
        with urlopen(request, timeout=timeout) as response:
            payload = response.read().decode("utf-8")
            return response.status, json.loads(payload)
    except HTTPError as error:
        payload = error.read().decode("utf-8", errors="replace")
        try:
            detail = json.loads(payload)
        except json.JSONDecodeError:
            detail = {"error": payload[:500]}
        return error.code, detail


def query_specs(sample):
    kind = sample["subjectType"]
    specs = [("title", kind, sample.get("title"))]
    if kind == "TRACK":
        specs.append(("album", "ALBUM", sample.get("albumTitle")))
    for artist in sample.get("artists", []):
        specs.append(("artist", "ARTIST", artist))
    return list(dict.fromkeys(spec for spec in specs if spec[2]))


def search(base, sample, pages, timeout):
    checks = []
    for field, kind, keyword in query_specs(sample):
        check = {"field": field, "type": kind, "keyword": keyword,
                 "foundSpotifyId": False, "foundName": False, "pagesChecked": 0}
        try:
            for page in range(pages):
                status, data = api(base, "GET", "/api/catalog/search",
                                   {"keyword": keyword, "page": page, "type": kind}, timeout=timeout)
                if status != 200 or data.get("success") is False:
                    check["error"] = {"httpStatus": status, "body": data}
                    break
                items = (data.get("response") or {}).get("items") or []
                check["pagesChecked"] += 1
                target = sample["spotifyId"] if kind == sample["subjectType"] else None
                if target and any(item.get("spotifyId") == target for item in items):
                    check["foundSpotifyId"] = True
                if any((item.get("title") or "").casefold() == keyword.casefold() for item in items):
                    check["foundName"] = True
                if check["foundSpotifyId"] or not items:
                    break
        except (URLError, TimeoutError, ValueError) as error:
            check["error"] = str(error)
        checks.append(check)
    return checks


def new_run(after_log, offset, key, wait_seconds):
    deadline = time.monotonic() + wait_seconds
    while True:
        if after_log.exists():
            with after_log.open("rb") as source:
                source.seek(offset)
                chunk = source.read().decode("utf-8", errors="replace")
            events = []
            for line in chunk.splitlines():
                try:
                    events.append(json.loads(line))
                except json.JSONDecodeError:
                    pass
            groups = event_groups(events)
            for run_id, group in groups.items():
                start = first(group, "import_started")
                if start and subject_key(start) == key:
                    if first(group, "tag_calculation_completed") or first(group, "import_skipped"):
                        return summarize(group)
        if time.monotonic() >= deadline:
            return None
        time.sleep(0.25)


def compare(before, after):
    if not after:
        return {"status": "DIAGNOSTIC_MISSING"}
    if after.get("skipped"):
        return {"status": "REUSED_EXISTING", "reason": after["skipped"]}
    if not before.get("calculationCompleted") or not after.get("calculationCompleted"):
        return {"status": "CALCULATION_INCOMPLETE"}
    old = {tag["name"] for tag in before["resolved"]}
    new = {tag["name"] for tag in after["resolved"]}
    providers = {}
    for name in sorted(set(before["providers"]) | set(after["providers"])):
        left, right = before["providers"].get(name, {}), after["providers"].get(name, {})
        providers[name] = {
            "beforeStatus": left.get("status"), "afterStatus": right.get("status"),
            "trackTagsAdded": sorted(set(right.get("trackTags", [])) - set(left.get("trackTags", []))),
            "trackTagsRemoved": sorted(set(left.get("trackTags", [])) - set(right.get("trackTags", []))),
            "albumTagsAdded": sorted(set(right.get("albumTags", [])) - set(left.get("albumTags", []))),
            "albumTagsRemoved": sorted(set(left.get("albumTags", [])) - set(right.get("albumTags", []))),
        }
    partial = (before.get("resolvedTruncated") or after.get("resolvedTruncated") or
               any(provider.get("trackTagsTruncated") or provider.get("albumTagsTruncated")
                   for provider in list(before["providers"].values()) + list(after["providers"].values())))
    return {"status": "COMPARED_PARTIAL" if partial else "COMPARED",
            "resolvedAdded": sorted(new - old),
            "resolvedRemoved": sorted(old - new), "providers": providers,
            "bootstrapCreatedBefore": before["bootstrap"].get("created"),
            "bootstrapCreatedAfter": after["bootstrap"].get("created"),
            "notableEventsAdded": sorted(set(after["notableEvents"]) - set(before["notableEvents"]))}


def write_report(path, results):
    path.parent.mkdir(parents=True, exist_ok=True)
    counts = defaultdict(int)
    lines = ["# Enrichment replay comparison", "",
             "The table reports observed changes. Provider responses and Spotify search results may change over time.", "",
             "| Type | Spotify ID | Title | Search | Before → after tags | Result |",
             "| --- | --- | --- | --- | --- | --- |"]
    for result in results:
        comparison = result.get("comparison") or {}
        status = comparison.get("status", "UNKNOWN")
        counts[status] += 1
        before_summary = result.get("before") or {}
        before = before_summary.get("resolvedTotalCount")
        if before is None:
            before = len(before_summary.get("resolved") or [])
        after_summary = result.get("after") or {}
        after = after_summary.get("resolvedTotalCount")
        if after_summary.get("calculationCompleted") and after is None:
            after = len(after_summary.get("resolved") or [])
        if not after_summary.get("calculationCompleted"):
            after = "?"
        checks = result.get("search") or []
        search_status = "ID found" if any(check.get("foundSpotifyId") for check in checks) else "ID absent"
        cells = [result.get("subjectType", ""), result.get("spotifyId", ""),
                 result.get("title", ""), search_status, f"{before} → {after}", status]
        lines.append("| " + " | ".join(str(cell).replace("|", "\\|").replace("\n", " ") for cell in cells) + " |")
    lines.extend(["", "## Status counts", ""])
    lines.extend(f"- {status}: {count}" for status, count in sorted(counts.items()))
    lines.extend(["", "Inspect the comparison JSONL for provider statuses, raw tags, resolved names, and run IDs.", ""])
    path.write_text("\n".join(lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--before-log", type=Path, default=Path("logs/enrichment-diagnostics.jsonl"))
    parser.add_argument("--after-log", type=Path, default=Path("logs/enrichment-diagnostics-tobe.jsonl"))
    parser.add_argument("--manifest", type=Path, default=Path("logs/enrichment-replay-manifest.jsonl"))
    parser.add_argument("--results", type=Path, default=Path("logs/enrichment-replay-results.jsonl"))
    parser.add_argument("--comparison", type=Path, default=Path("logs/enrichment-comparison.jsonl"))
    parser.add_argument("--report", type=Path, default=Path("logs/enrichment-comparison.md"))
    parser.add_argument("--before-cutoff", default="2026-11-05", help="ISO timestamp prefix; excludes later runs from baseline")
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--max-search-pages", type=int, default=3)
    parser.add_argument("--timeout", type=float, default=30)
    parser.add_argument("--diagnostic-wait", type=float, default=5)
    parser.add_argument("--limit", type=int)
    parser.add_argument("--execute", action="store_true", help="Call search and import APIs")
    args = parser.parse_args()
    if args.before_log.resolve() == args.after_log.resolve():
        parser.error("--before-log and --after-log must be different files")
    if args.max_search_pages < 1 or args.timeout <= 0 or args.diagnostic_wait < 0:
        parser.error("search pages and timeouts must be positive")
    samples = manifest(rows(args.before_log), args.before_cutoff)
    if args.limit is not None:
        samples = samples[:args.limit]
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    with args.manifest.open("w", encoding="utf-8") as target:
        for sample in samples:
            target.write(json.dumps(sample, ensure_ascii=False) + "\n")
    print(f"Selected {len(samples)} distinct TRACK/ALBUM subjects")
    if not args.execute:
        for sample in samples:
            print(f"{sample['subjectType']} {sample['spotifyId']} {sample['title']}")
        return 0
    existing = {(row.get("subjectType"), row.get("spotifyId")) for row in rows(args.results)
                if row.get("httpStatus") == 200 and row.get("apiSuccess") is True}
    for index, sample in enumerate(samples, 1):
        key = sample["subjectType"], sample["spotifyId"]
        if key in existing:
            continue
        result = {"subjectType": key[0], "spotifyId": key[1], "title": sample["title"],
                  "artists": sample["artists"], "albumTitle": sample["albumTitle"],
                  "before": sample["before"]}
        result["search"] = search(args.base_url, sample, args.max_search_pages, args.timeout)
        offset = args.after_log.stat().st_size if args.after_log.exists() else 0
        try:
            status, payload = api(args.base_url, "POST", "/api/catalog/import",
                                  body={"subjectType": key[0], "spotifyId": key[1]}, timeout=args.timeout)
            result["httpStatus"] = status
            result["apiSuccess"] = payload.get("success")
            if status == 200 and payload.get("success") is True:
                response = (payload.get("response") or {}).get(key[0].lower()) or {}
                result["apiTags"] = response.get("systemTags", [])
                result["apiPreviewTags"] = response.get("previewTags", [])
                result["apiTagDisplayStatus"] = response.get("tagDisplayStatus")
                result["after"] = new_run(args.after_log, offset, key, args.diagnostic_wait)
            else:
                result["apiError"] = payload
        except (URLError, TimeoutError, ValueError) as error:
            result["requestError"] = str(error)
        if "requestError" in result or result.get("apiSuccess") is not True:
            result["comparison"] = {"status": "REQUEST_FAILED"}
        else:
            result["comparison"] = compare(result["before"], result.get("after"))
        append(args.results, result)
        print(f"[{index}/{len(samples)}] {key[0]} {key[1]}: {result['comparison']['status']}", flush=True)
        if "requestError" in result:
            print("Stopping after connection failure; rerun resumes completed results", file=sys.stderr)
            break
        if result["comparison"]["status"] == "DIAGNOSTIC_MISSING":
            print("Stopping: after-log has no matching run. Check server ENRICHMENT_DIAGNOSTICS_FILE.", file=sys.stderr)
            break
    latest = {}
    for result in rows(args.results):
        latest[result["subjectType"], result["spotifyId"]] = result
    results = list(latest.values())
    args.comparison.parent.mkdir(parents=True, exist_ok=True)
    with args.comparison.open("w", encoding="utf-8") as target:
        for result in results:
            target.write(json.dumps({key: result.get(key) for key in
                         ("subjectType", "spotifyId", "title", "artists", "albumTitle",
                          "search", "httpStatus", "apiSuccess", "apiTagDisplayStatus",
                          "before", "after", "comparison")}, ensure_ascii=False) + "\n")
    write_report(args.report, results)
    return 0


if __name__ == "__main__":
    sys.exit(main())
