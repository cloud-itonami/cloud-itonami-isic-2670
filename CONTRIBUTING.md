# Contributing

`cloud-itonami-isic-2670` accepts contributions to the OSS blueprint,
capability bindings, policy tests, documentation and operator model.

## Development
The capability layer lives in `kotoba-lang/*` libraries. This repo holds the
business blueprint and operator contracts.

```bash
clojure -M:dev:test
clojure -M:lint
```

## Rules
- Do not commit real operating, personal or credential data.
- Keep robot dispatch, records and disclosures behind the
  Module-Seating Governor.
- Treat workflows as high-risk: add tests for robot-safety gating,
  record integrity, disclosure and audit logging.
- Document any new business-model or operator assumption in `docs/`.
- Never fabricate an optical-standard citation (ISO 12233 / IEC 60825-1
  / ISO 26262 / ISO 20653 / IP6K9K or any other). If you are not
  confident of a product class's requirements or a numeric engineering-
  estimate figure (e.g. a seating-force tolerance band, or the exact
  scope of a specific SAE reference), leave it out or disclose the
  uncertainty explicitly in `opticsworks.facts`/`opticsworks.robotics`
  coverage/docstrings -- never invent one or present an unconfident
  citation as certain.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
