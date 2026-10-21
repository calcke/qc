# QC

Quote cards and stories. One web app, two ways to use it:

- **web/**: the app itself (one offline `index.html`), published as a website.
- **android/**: a small Kotlin app that bundles `web/` and runs it full-screen. GitHub builds `qc.apk` for you.

Photos and settings are saved on your device. The Android app works fully offline.

## Put it on GitHub

1. Unzip `qc-project.zip`. Open the `qc` folder inside it.
2. Open your GitHub repo `qc` > **Add file > Upload files**.
3. Select **everything inside** the `qc` folder (Ctrl+A) and drag it into the page. Drag the contents, not the `qc` folder itself.
   You should see `.github`, `android`, `web`, `README.md` and the rest in the list.
4. Click **Commit changes**.
5. Repo **Settings > Pages > Source: GitHub Actions** (once only).
6. Open the **Actions** tab. "QC build" runs: about 5 to 8 minutes the first time.

If you uploaded an older `index.html` into the repo root earlier, you can delete it. The website is now published from `web/`.

## Get the results

- **Website:** `https://YOUR-NAME.github.io/qc/` (also under Settings > Pages). Chrome menu > Install app puts it on your desktop or phone.
- **Android app:** repo page > **Releases** (right side) > newest build > download **qc.apk**, then install it on your phone.
  Allow "install unknown apps" for your browser when asked.

## Updating later

Edit `web/index.html` (on GitHub: open it, pencil icon, commit) or upload a new copy into `web/`. The site updates and a new
`qc.apk` appears under Releases. Install it **over** the old one: your photos and settings stay, because every build is signed
with the same key (`android/app/personal.keystore`) and has a higher version number.

## If a build fails

Actions tab > the run with the red cross > click the red **Build qc.apk** (or **Publish website**) box > click the step with
the red cross > copy the last 20 lines. Pass those on and the fix is usually one line.
If "Publish website" says Pages is not found, do step 5, then open the run and press **Re-run all jobs**.

## Good to know

- Saved images go to **Pictures/QuoteCards** (they show in your gallery); ZIPs go to **Downloads/QuoteCards**.
- The app keeps its own storage, separate from Chrome. Anything made in Chrome won't appear in the app.
- Android's built-in browser has no colour picker, so the app adds a hex box (like `#F6F2EA`) under each colour. Swatches still work.
- The signing key password is in the repo so builds work on GitHub. That is fine for a personal app. If you prefer, make the repo
  private (you lose the free website; the APK builds still work).
- Needs Android 10 or newer.

## Project layout

```
.github/workflows/qc-workflow.yml   builds the APK and publishes the site
web/                                the app (index.html, manifest, service worker, icons)
android/                            Kotlin WebView app (Gradle project)
```
