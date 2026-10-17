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
| [🥗 Salat](Salat/) | An "everything" app with many small apps inside it. [Download](https://github.com/oljodev/mobile-apps/releases/tag/salat-latest) |

## How the apps are built

I don't have a Mac, so the apps are built in the cloud with GitHub Actions. This repo is public, which means the macOS runners are free to use.

Each build produces an **unsigned `.ipa`** file, which you can download from the workflow run's artifacts (or from a release). SideStore signs the app with your own Apple ID when you install it, so no paid Apple Developer account is needed.

## Installing an app with SideStore

1. Set up SideStore on your iPhone (see the [SideStore docs](https://docs.sidestore.io)).
2. Download the app's `.ipa` from GitHub Actions or Releases to your phone.
3. Open SideStore, go to **My Apps**, tap **+** and pick the `.ipa`.
4. Refresh your apps in SideStore at least once every 7 days. Apps signed with a free Apple ID stop opening after 7 days.

### Limits of a free Apple ID

- At most 3 sideloaded apps can be active at once, and SideStore itself counts as one of them.
- At most 10 App IDs can be registered per 7 days.
