# Operator Guide

## First Deployment
1. Register quality engineers, plants, optical-module-batches,
   personnel and robots.
2. Import historical optical-module-batch / end-of-line /
   optical-standard-certification records.
3. Run read-only validation and robot mission dry-runs.
4. Configure optical-standard-certification evidence checklists and
   human sign-off paths.
5. Publish a dry-run audit export.

## Minimum Production Controls
- governor gate on every robot action before dispatch
- human sign-off for `:high`/`:safety-critical` robot actions (e.g.
  lens-seating verification on optical-module-batches, Optical Module
  Test Certificate issuance)
- audit export for every shipment, sign-off and disclosure
- backup manual process

## Certification
Certified operators must prove robot-safety integrity, evidence-backed
records and human review for safety-affecting actions.

## Operating states
intake : optical-standard-rules-verify : end-of-line-quality-screen : robotics-simulate-lens-seating-test : approve : ship-optical-module-batch : issue-optical-certificate : audit

## Audit export (social operation)

After a production session, export the append-only package for
downstream-consumer quality auditors or internal compliance:

```clojure
(require '[opticsworks.store :as store]
         '[opticsworks.export :as export])
(export/audit-package store)        ; EDN maps
(export/package->csv-bundle store)  ; CSV files as string map
```

Drafts remain **unsigned** — signing and shipment to the downstream
device/vehicle assembler's own intake are the camera/optical-module
manufacturer's own acts (see README Actuation honesty).

Static UI sample: `docs/samples/operator-console.html`.
