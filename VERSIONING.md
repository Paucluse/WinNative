# Versioning

Current baseline:
- `VERSION_NAME=0.2.00`
- `VERSION_CODE=200`

Rules:
- Every user-facing feature pushed to remote bumps the third segment: `0.1.00` -> `0.1.01` -> `0.1.02`
- A grouped milestone or noticeably larger feature batch bumps the middle segment and resets the third: `0.1.09` -> `0.2.00`
- The first segment stays `0` until the project reaches a stable public milestone, then moves to `1.0.00`

Version code strategy:
- `VERSION_CODE = major * 10000 + minor * 100 + patch`
- Examples:
  - `0.1.00` -> `100`
  - `0.1.01` -> `101`
  - `0.2.00` -> `200`
  - `1.0.00` -> `10000`

Build behavior:
- Local builds use `version.properties` by default
- CI or manual builds can override with `-PVERSION_NAME=...` and `-PVERSION_CODE=...`
