# L.B. Smith Parts

An offline Ford base-number reference for a parts-department employee. Search a familiar name, shop synonym, exact base or full service part number. One family card contains its applicable base numbers.

[Download Android APK](https://github.com/urbanrunnerx/lb-smith-parts/releases/latest/download/LB-Smith-Parts.apk) · [Release notes](https://github.com/urbanrunnerx/lb-smith-parts/releases/latest)

## Included catalog

- **15,700 distinct exact base numbers**, grouped into 2,436 named families and broad reference groups.
- 15,171 bases from 200,239 conventional Ford/Motorcraft service-part records in the publicly offered packaging CSV, downloaded September 17, 2026.
- Preserves the prior 690-base historical reference and 1,246 distinct bases from saved EPC observations; the union adds 529 bases beyond the packaging snapshot.
- 1,992 families have more specific names; 444 groups retain broad source descriptions. Round 2 classifies generic descriptions more conservatively. A broad group can contain different components. Open the source evidence before selecting a service part.
- Count excluding two-digit body-style prefixes for comparison: **8,947**. This diagnostic is not used to alter or infer a service number.
- Leading zeros and seven/eight-character body bases are preserved. Service prefixes, suffixes, trim variants and duplicate source rows do not count as new bases.

This is a broad snapshot, **not every Ford base ever assigned**. It is not a VIN-fitment catalog or an interchange guarantee. Packaging descriptions can be terse; historical and EPC references supply additional names and context. System labels are navigation aids, partly inferred from standard numbering ranges. Exact original descriptions and example service numbers are available on each base.

### Example

Search **purge valve** → one **EVAP purge valve** card, with **9C915** and **9D289**. Details distinguish a valve from a vapor-line assembly with an integrated valve. The numbers are alternatives across applications, not interchangeable parts.

## Counter tools

- Saved parts, recent searches, grid/list views and light/dark themes.
- Per-part notes for bin locations and reminders.
- Job desk with separate vehicles, job/RO references, quantities, counter notes and selected full service numbers. VIN changes mark earlier selections for rechecking.
- Dedicated Snap-on menu: enter a VIN and search a common part name with the same offline family index. Choose the family and starting base; a different VIN opens its own job.
- Native guided Snap-on lookup on Android: VIN/base load after sign-in, with dropdowns for available locations, systems, sections and illustrations. Review application/date/quantity cards and return an employee-selected service number to the correct job.
- Copy, print or export a job; mark it done or reopen it. Backups include all jobs, selected parts and the VIN decoder cache. Existing v1 worksheets, notes and saved parts migrate on upgrade.
- Native clipboard copy, CSV exports, CSV reference imports and JSON workspace backup/restore.
- Full-number breakdown; example service-number matching and conservative compact decoding.
- Offline catalog bundled in the APK. The local reference needs no login. Online VIN decoding uses NHTSA vPIC; EPC lookups require internet and your own Snap-on account. Source links open in the system browser. No app server, analytics or ads.

Browser and Android storage are separate. Back up the workspace before reinstalling or changing devices. The integrated VIN Decoder uses the official NHTSA vPIC service online for vehicle identity and manufacturer-reported specifications. It validates VIN format/check digit, accepts an optional model-year hint, displays source warnings, and caches the last 15 decoded vehicles for offline access. Missing fields remain unknown. It does not infer factory equipment, live prices, inventory, availability, supersessions or VIN fitment.

## VIN to service part

1. Open **Snap-on**. Enter the VIN and a common part description, synonym, base or service number.
2. Choose a matching family. Alternate bases stay together; choose the base to try first.
3. Tap **Find in integrated Snap-on**. Sign in on the phone through **Catalog / sign in** when needed; the desktop login does not transfer.
4. The app loads the VIN and searches the selected base. Use **Find parts** to retry or search another base. Keep VIN filters enabled.
5. Choose from the native location, system, section or illustration dropdowns. Tap **Open selection** to navigate. The catalog path lets you return to an earlier section.
6. Review service-part cards with the application, restrictions, build dates and quantity. Tap **Review this part**, then **Save to job**. The adapter checks the current VIN, base, catalog path and displayed part again before saving.

Existing family details and job rows also open this guided lookup. **Catalog / sign in** exposes the original catalog for authentication, diagrams or unsupported screens; **Guided lookup** returns to the app controls. **Load more catalog results** advances the rendered rows when the catalog uses a scrolling grid.

The interface reads the rendered page in an isolated Snap-on session. It is not a licensed Snap-on API integration or automatic fitment decision. Credentials are entered only on the original sign-in page and are not read by the adapter. Native choices are taken from recognized catalog navigation controls; ordering, picklist and CDK controls are not driven by the adapter. If the catalog changes or rejects its embedded session, the app shows an explicit catalog fallback. Lists represent rendered rows, not an exhaustive fitment result.

The selectors were checked against an employee-authorized browser session. Automated Android tests use synthetic catalog fixtures; authenticated Android operation still needs verification with the employee's phone session. No private vehicle or newly accessed catalog records from that inspection are shipped in the source.

The browser version opens Snap-on separately and lets the employee record a reviewed number. The app does not send orders or write to CDK. Snap-on's documented CDK integration requires its supported local services/configuration on the counter workstation; see [Snap-on integration setup](https://docs.snaponbusinesssolutions.com/docs/EPC5Help/Ford/EPC5_Help_en-US/Content/ConfiguringIntegration.htm). Job copy/CSV is a handoff aid, not a CDK import contract.

## Android installation and builds

Android 8.0+ with a current Android System WebView. Download `LB-Smith-Parts.apk` from the release, open it, allow installation for the downloading browser/file manager if prompted, then install.

GitHub Actions runs the catalog/search tests, checks catalog reproducibility, builds Android APKs, runs Android lint and Android emulator tests for the offline app, guided controls and catalog adapter. Workflow artifacts include an **unsigned release** APK and a **debug test** APK. Only the signed APK attached to a release is the intended user download. Debug APK signatures can change between runs; do not install a debug APK over the signed release.

Releases are signed locally with the same private key. Keep the release signing key and password private and backed up; they are deliberately excluded from this repository. Install updates over the existing release to retain its local workspace. Future releases must use the same key and a larger `versionCode`. Sign an unsigned APK with Android SDK `apksigner`, verify it, then attach it to the GitHub release with the name `LB-Smith-Parts.apk`.

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
