# L.B. Smith Parts

An offline Ford base-number reference for a parts-department employee. Search a familiar name, shop synonym, exact base or full service part number. One family card contains its applicable base numbers.

[Download Android APK](https://github.com/urbanrunnerx/lb-smith-parts/releases/latest/download/LB-Smith-Parts.apk) · [Release notes](https://github.com/urbanrunnerx/lb-smith-parts/releases/latest)

## Included catalog

- **15,700 distinct exact base numbers**, grouped into 2,441 named families and broad reference groups.
- 15,171 bases from 200,239 conventional Ford/Motorcraft service-part records in the publicly offered packaging CSV, downloaded September 17, 2026.
- Preserves the prior 690-base historical reference and 1,246 distinct bases from saved EPC observations; the union adds 529 bases beyond the packaging snapshot.
- 2,094 families have more specific names; 347 groups retain broad source descriptions. A broad group such as an engine cover can contain different components. Open the source evidence before selecting a service part.
- Count excluding two-digit body-style prefixes for comparison: **8,947**. This diagnostic is not used to alter or infer a service number.
- Leading zeros and seven/eight-character body bases are preserved. Service prefixes, suffixes, trim variants and duplicate source rows do not count as new bases.

This is a broad snapshot, **not every Ford base ever assigned**. It is not a VIN-fitment catalog or an interchange guarantee. Packaging descriptions can be terse; historical and EPC references supply additional names and context. System labels are navigation aids, partly inferred from standard numbering ranges. Exact original descriptions and example service numbers are available on each base.

### Example

Search **purge valve** → one **EVAP purge valve** card, with **9C915** and **9D289**. Details distinguish a valve from a vapor-line assembly with an integrated valve. The numbers are alternatives across applications, not interchangeable parts.

## Counter tools

- Saved parts, recent searches, grid/list views and light/dark themes.
- Per-part notes for bin locations and reminders.
- Counter worksheet with job/RO, vehicle/VIN note, quantities and confirmed service-number notes.
- Native clipboard copy, CSV exports, CSV reference imports and JSON workspace backup/restore.
- Full-number breakdown; example service-number matching and conservative compact decoding.
- Offline catalog bundled in the APK. No login, app server, analytics or ads. Internet is used only for an explicitly requested new VIN decode. Source links open in the system browser.

Browser and Android storage are separate. Back up the workspace before reinstalling or changing devices. The integrated VIN Decoder uses the official NHTSA vPIC service online for vehicle identity and manufacturer-reported specifications. It validates VIN format/check digit, accepts an optional model-year hint, displays source warnings, and caches the last 15 decoded vehicles for offline access. Missing fields remain unknown. It does not infer factory equipment, live prices, inventory, availability, supersessions or VIN fitment.

## Android installation and builds

Android 8.0+ with a current Android System WebView. Download `LB-Smith-Parts.apk` from the release, open it, allow installation for the downloading browser/file manager if prompted, then install.

GitHub Actions runs the catalog/search tests, checks catalog reproducibility, builds Android APKs, runs Android lint and an emulator smoke test. Workflow artifacts include an **unsigned release** APK and a **debug test** APK. Only the signed APK attached to a release is the intended user download. Debug APK signatures can change between runs; do not install a debug APK over the signed release.

The first release is signed locally. Keep the release signing key and password private and backed up; they are deliberately excluded from this repository. Future releases must use the same key and a larger `versionCode`. Sign an unsigned APK with Android SDK `apksigner`, verify it, then attach it to the GitHub release with the name `LB-Smith-Parts.apk`.

Local build requirements: JDK 17, Gradle 8.13, Android SDK 35/build-tools 35.0.0.

```sh
npm test
python scripts/build-catalog.py
gradle :android:app:assembleRelease :android:app:lintDebug
```

## Web preview

```sh
python -m http.server 8097 --directory dist
```

Open `http://localhost:8097`. All dependencies and reference files are local. A service worker caches the web version after the first successful load. Change the service-worker cache version when publishing content updates.

## Rebuild or extend the data

1. Download the public CSV linked by the [Ford/Motorcraft packaging reference](https://upccrossreference.cdis3.com/default.asp?p=8u%24D%21T44bELeW).
2. Run `python scripts/import-packaging.py /path/to/packagingdata.csv --date YYYY-MM-DD`.
3. Review `data/families.json` for common names and documented multi-base groupings. Never add a base solely to reach a count.
4. Run `python scripts/build-catalog.py` and `npm test`; review changes to `data/catalog-audit.json` and the generated catalog.
5. Update the UI snapshot date, release version and APK `versionCode` when publishing a new snapshot. Current release dates are explicit rather than implied live updates.

`data/packaging-reference.json` stores reduced factual evidence, up to three service examples per base, source-row counts and the SHA-256 of the raw CSV. The raw packaging file is not republished. Rebuilding from the reduced evidence is deterministic.

## Sources and branding

- [Ford / Motorcraft public packaging cross-reference](https://upccrossreference.cdis3.com/default.asp?p=8u%24D%21T44bELeW)
- [Public Ford Basic Numbers quick reference](https://www.terminator-cobra.com/FordBasicNumber.pdf)
- Saved [Snap-on EPC](https://snaponepc.com/epc/#/) observations from the existing `urbanrunnerx/ford-base-finder` project (September 10, 2026); limited catalog coverage, no credentials or VIN records.
- [9D289 purge assembly example](https://www.fordpartsgiant.com/parts/ford-tube-asy-fuel-vapour-separat_k2gz-9d289-a.html)
- [L.B. Smith Ford dealership](https://www.lbsmithford.com/), Lemoyne, Pennsylvania. Logo artwork used at the user's request from the dealership's published header. Original asset URL is in `BRANDING.md`.

Ford and dealership branding remain their owners' property. This workspace is not an official Ford electronic parts catalog.
