#!/usr/bin/env python3
"""Legger en ny versjon av en app inn i SideStore-kilden (source.json).

Kilden følger AltStore sitt format, som SideStore også bruker:
https://faq.altstore.io/developers/make-a-source
"""

import argparse
import json
import os
import plistlib
from datetime import datetime, timezone

# Hvor mange tidligere versjoner som beholdes i kilden.
MAX_VERSIONS = 10


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, help="source.json som oppdateres, eller lages hvis den mangler")
    parser.add_argument("--repo", required=True, help="GitHub-repoet, f.eks. oljodev/salat")
    parser.add_argument("--app", required=True, help="Den bygde .app-mappen, for å lese Info.plist")
    parser.add_argument("--ipa", required=True, help=".ipa-filen, for å finne størrelsen")
    parser.add_argument("--download-url", required=True)
    parser.add_argument("--icon-url", required=True)
    parser.add_argument("--description", required=True)
    parser.add_argument("--tint-color", required=True, help="Hex-farge uten #, f.eks. 8CD47E")
    parser.add_argument("--notes", default="", help="Hva som er nytt i denne versjonen")
    args = parser.parse_args()

    with open(os.path.join(args.app, "Info.plist"), "rb") as f:
        info = plistlib.load(f)

    owner = args.repo.split("/")[0]
    source = load_source(args.source) or {
        "name": args.repo,
        "identifier": f"io.github.{args.repo.replace('/', '.')}",
        "sourceURL": f"https://github.com/{args.repo}/releases/download/sidestore/source.json",
        "apps": [],
        "news": [],
    }

    version = {
        "version": info["CFBundleShortVersionString"],
        "buildVersion": info["CFBundleVersion"],
        "date": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "localizedDescription": args.notes,
        "downloadURL": args.download_url,
        "size": os.path.getsize(args.ipa),
        "minOSVersion": info.get("MinimumOSVersion", "26.0"),
    }

    bundle_id = info["CFBundleIdentifier"]
    old = next((a for a in source["apps"] if a["bundleIdentifier"] == bundle_id), {})
    old_versions = [v for v in old.get("versions", []) if v["version"] != version["version"]]

    app = {
        "name": info.get("CFBundleDisplayName") or info["CFBundleName"],
        "bundleIdentifier": bundle_id,
        "developerName": owner,
        "localizedDescription": args.description,
        "iconURL": args.icon_url,
        "tintColor": args.tint_color,
        "versions": [version] + old_versions[: MAX_VERSIONS - 1],
        # SideStore viser hvilke tillatelser appen ber om, og sjekker at de stemmer med appen.
        "appPermissions": {
            "entitlements": [],
            "privacy": {k: v for k, v in info.items() if k.startswith("NS") and k.endswith("UsageDescription")},
        },
        # Eldre versjoner av SideStore leser disse feltene i stedet for "versions".
        "version": version["version"],
        "versionDate": version["date"],
        "downloadURL": version["downloadURL"],
        "size": version["size"],
    }

    source["apps"] = [a for a in source["apps"] if a["bundleIdentifier"] != bundle_id] + [app]

    with open(args.source, "w") as f:
        json.dump(source, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"La til {app['name']} {version['version']} ({version['buildVersion']}) i {args.source}")


def load_source(path):
    if not os.path.exists(path):
        return None
    with open(path) as f:
        return json.load(f)


if __name__ == "__main__":
    main()
