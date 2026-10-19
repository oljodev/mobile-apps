# mobile-apps

Small iOS apps I build for my own use. They are not published to the App Store. I install them on my phone with [SideStore](https://sidestore.io).

## Repository layout

Each app has its own folder in the root of the repo:

```
mobile-apps/
├── README.md
├── .github/workflows/   Build workflows, one per app (GitHub requires them here)
└── Salat/
```

Each app folder holds the source code for that app and a short README about it.

## Apps

| App | What it is |
| --- | --- |
| [🥗 Salat](Salat/) | An "everything" app with many small apps inside it. |

## How the apps are built

I don't have a Mac, so the apps are built in the cloud with GitHub Actions. This repo is public, which means the macOS runners are free to use.

Each build from `main` gets a new version number and is published as a GitHub release with an **unsigned `.ipa`** file. SideStore signs the app with your own Apple ID when you install it, so no paid Apple Developer account is needed.

The build also updates a **SideStore source**: a `source.json` file in the [`sidestore` release](../../releases/tag/sidestore) that lists the newest version of each app. SideStore checks it for updates.

## Installing an app with SideStore

1. Set up SideStore on your iPhone (see the [SideStore docs](https://docs.sidestore.io)).
2. In SideStore, go to **Sources**, tap **+** and add this source:
   ```
   https://github.com/oljodev/salat/releases/download/sidestore/source.json
   ```
3. Open the source, pick the app and tap **Get** (or **Free**).
4. Refresh your apps in SideStore at least once every 7 days. Apps signed with a free Apple ID stop opening after 7 days.

When a new version has been built, it shows up under **My Apps** in SideStore. Tap **Update** to install it. The app keeps its data.

### Limits of a free Apple ID

- At most 3 sideloaded apps can be active at once, and SideStore itself counts as one of them.
- At most 10 App IDs can be registered per 7 days.
