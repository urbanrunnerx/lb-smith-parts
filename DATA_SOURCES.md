# Public source provenance

The inherited EPC observations and legacy reference were verified on September 17, 2026 by unauthenticated HTTPS requests to the existing public repository. Both copies match the local source files exactly after newline normalization.

- https://raw.githubusercontent.com/urbanrunnerx/ford-base-finder/main/dist/epc-reference.json
  - SHA-256 (UTF-8, LF): ae88179241c530c8024520274566f5aaa8f93d27b2106763545c8ef12dba0e33
- https://raw.githubusercontent.com/urbanrunnerx/ford-base-finder/main/dist/reference.txt
  - SHA-256 (UTF-8, LF): 891b314d0903d972f196ead0d44b59fac6b55833a2462bd12a298ce4a7f691b1

New packaging records are extracted from Ford/Motorcraft's explicit public download at https://upccrossreference.cdis3.com/downloadfiles/packagingdata.csv. The raw file hash is saved in data/catalog-audit.json.

The user explicitly requested a new L.B. Smith Parts GitHub repository, a downloadable APK, and the dealership's publicly posted logo artwork. The original logo URL is documented in BRANDING.md. No account credentials, private files, customer VINs, or customer records are included. VIN notes and lookups created in the app remain local except the specific VIN submitted to NHTSA when the user chooses Decode VIN, or the VIN/base sent to Snap-on when the employee starts an EPC lookup. The employee signs into their own Snap-on session; credentials are not read by the app's page adapter, copied into the workspace, or sent to this repository.

Round 2 adds UI selectors and a visible-page adapter developed against the employee-authorized catalog workflow. No newly accessed private EPC catalog dump, customer record, or test vehicle VIN is included in the published app. Automated integration tests use explicitly synthetic part numbers and a public example VIN.
