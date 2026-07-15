# cloud-itonami-isic-2670

Open Business Blueprint for **ISIC Rev.5 2670**: manufacture of optical
instruments and photographic equipment -- optical-module-batch intake,
per-product-class optical-standard evidence verification, end-of-line
optical-clarity/dust-ingress/dead-pixel quality screening, robot
lens-barrel press-fit seating-test verification and Optical Module Test
Certificate finalization for a community camera/optical-module plant.

This repository publishes an optical/camera-module-manufacturing actor
-- optical-module-batch intake, per-product-class optical-standard
evidence-checklist verification, end-of-line defect screening, robot
lens-seating verification mission and Optical Module Test Certificate
issuance -- as an OSS business that any qualified camera/optical-module
manufacturer can fork, deploy, run, improve and sell, so a plant keeps
its own production and conformance history instead of renting a closed
MES / quality SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **Optics Advisor ⊣
Module-Seating Governor**.

## Scope note: the missing shared upstream stage for BOTH smartphones and vehicles

This repository is scoped to **manufacturing camera/optical modules**
(lens-barrel press-fit seating-test verification, per-product-class
optical-standard evidence, end-of-line defect screening,
optical-module-batch shipment and Optical Module Test Certificate
issuance). It is not a device-assembly or vehicle-assembly vertical
itself. A camera/optical-module manufacturer that produces BOTH
smartphone camera modules (compact, high-density lens stacks) and
automotive ADAS-grade optical sensor modules (backup cameras, LIDAR/
camera housings) sits directly UPSTREAM of BOTH chains this fleet has
been building out:

- `cloud-itonami-isic-2630` -- manufacture of communication equipment
  (smartphone/communication-device assembly). A smartphone's camera
  module is a fundamentally optical-instrument-manufacturing output;
  `commsdevice.robotics`'s own display-bonding-press mission runs
  downstream of THIS actor's `:actuation/ship-optical-module-batch`
  hand-off for the smartphone-camera-module product class.
- `cloud-itonami-isic-2910` -- manufacture of motor vehicles (final
  assembly) / `cloud-itonami-isic-2920` -- manufacture of bodies
  (coachwork) for motor vehicles. Backup-camera and ADAS-grade
  camera/LIDAR sensor modules are likewise fundamentally
  optical-instrument-manufacturing outputs -- `automotive.governor`'s/
  `bodyshop.governor`'s own end-of-line quality gates assume a
  finished, certified optical sensor module already exists as an
  input.

This vertical is the natural UNIFYING upstream stage for both chains:
neither a smartphone camera module nor an automotive backup-camera/
ADAS sensor module can ship without a seating-force-verified,
optical-standard-certified optical-module-batch first passing THIS
actor's gates. Distinct from:

- `cloud-itonami-isic-2630` -- device ASSEMBLY (consumes
  optical-module-batches for camera modules, does not produce them).
- `cloud-itonami-isic-2910`/`cloud-itonami-isic-2920` -- vehicle/body
  ASSEMBLY (consumes optical-module-batches for backup-camera/ADAS
  sensor modules, does not produce them).
- `cloud-itonami-isic-2620` -- computers/peripheral-equipment
  manufacturing (a general EMC/product-safety self-declaration
  regime, no radio transmitter and no optical-imaging-specific
  resolution/laser-safety basis this actor's own catalog covers).
- `cloud-itonami-isic-2220` -- injection-molded-plastics manufacturing
  (an adjacent but chemically/physically distinct materials vertical
  that may supply this actor's own housing components as an input,
  not an optics-manufacturing analog).

## Upstream -> downstream hand-off (2670 -> 2630 / 2910 / 2920)

```text
cloud-itonami-isic-2670 (THIS repo: optical-module-batch seating-force verification + optical-standard cert -> released batch)
  --> cloud-itonami-isic-2630 (smartphone/communication-device assembly: camera-module integration)
  --> cloud-itonami-isic-2910 / cloud-itonami-isic-2920 (motor-vehicle/body assembly: backup-camera/ADAS optical-sensor-module integration)
```

`:actuation/ship-optical-module-batch` is the REAL hand-off event: a
camera/optical-module manufacturer dispatches a seating-force-verified,
optical-standard-certified optical-module-batch onward to a downstream
consumer. This actor does not assume which downstream consumer a given
batch ships to -- the same released batch record and Optical Module
Test Certificate serve either hand-off.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here robots (lens-seating-cell
handling, end-of-line optical/dimensional scan) operate under an actor
that proposes actions and an independent **Module-Seating Governor**
that gates them. The governor never issues an Optical Module Test
Certificate itself; `:high`/`:safety-critical` actions (`:actuation/
ship-optical-module-batch`, `:actuation/issue-optical-certificate`)
require human sign-off.

**Robot process simulation is a REAL, time-stepped physics
simulation, not a symbolic field comparison** (native from day one,
per ADR-2607151600/ADR-2607152000's fleet pattern -- this vertical is a
NEW actor built to that standard, not a retrofit): `opticsworks.robotics`
walks every optical-module-batch through a robot-executed lens-barrel
press-fit seating-test mission (`kotoba.robotics` mission/action/
telemetry-proof contracts) -- a real, tested rigid-body physics engine
(`kotoba-lang/physics-2d`) time-steps a moving seating-press tool rigid
body closing at a controlled velocity onto a static lens-housing
seating-register rigid body, and reads a real peak seating force
(`:sim-peak-seating-force-n`, Newtons) directly off the simulated
collision -- not an invented or hand-set number. The Module-Seating
Governor independently re-derives the batch's own
`:sim-peak-seating-force-n` against the batch's own recorded
`:seating-force-min-n`/`:seating-force-max-n` acceptance band --
anchored on a REASONED ENGINEERING ESTIMATE for real lens-barrel
press-fit seating forces (roughly low-single-digit-to-tens of newtons
for consumer smartphone optics, plausibly higher for ruggedized
automotive housings -- see `opticsworks.robotics`'s own docstring for
the full honest confidence disclosure) -- never trusting the mission's
self-reported verdict alone.

## Core contract

```text
optical-module-batch intake + optical-standard-rules verify + end-of-line quality screen
  -> Optics Advisor proposal
  -> Module-Seating Governor (HARD holds un-overridable)
  -> phase gate (actuation always escalates)
  -> human approval for high stakes
  -> append-only ledger + draft records
```

## Actuation honesty

Shipping an optical-module-batch onward to a downstream consumer via a
robot handling/dispatch action and issuing an Optical Module Test
Certificate produce **unsigned draft records and ledger facts only**.
This actor does not talk to real plant control systems or a downstream
consumer's own intake portal. Signature and hardware dispatch are the
camera/optical-module manufacturer's own acts.

## Ops

| Op | Effect |
|---|---|
| `:optical-module-batch/intake` | normalize optical-module-batch directory patch (phase 3 may auto-commit when clean) |
| `:optical-standard-rules/verify` | per-product-class optical-standard evidence checklist (ISO/IEC consumer-camera / ISO automotive-ADAS; always human) |
| `:end-of-line-quality/screen` | end-of-line optical-clarity/dust-ingress/dead-pixel defect screen (HARD hold if unresolved) |
| `:robotics/simulate-lens-seating-test` | robot lens-barrel press-fit seating-test mission (always human; required on file before shipment) |
| `:actuation/ship-optical-module-batch` | draft optical-module-batch-shipment record onward to a downstream consumer (always human; HARD hold if robotics-sim missing, independently out-of-tolerance seating force, or focus-back-distance deviation out of range) |
| `:actuation/issue-optical-certificate` | draft Optical Module Test Certificate record (always human) |

## Optical-standard schemes (honest coverage)

`opticsworks.facts` seeds two REAL, current, cited product-class
schemes -- see that namespace's own docstring for the full honest
disclosure of why these keys are organized by PRODUCT CLASS, not a
per-country code table (the same structural observation
`moldworks.facts` makes for injection-molded-plastics material
conformance):

- **SMARTPHONE-CAMERA** -- smartphone camera-module optics: ISO 12233
  (the real standard resolution/MTF test method for camera-module
  image-quality QA), IEC 60825-1 (laser/optical-radiation safety --
  relevant because modern smartphone camera modules commonly include a
  laser-based autofocus/Time-of-Flight depth-sensing emitter).
- **AUTOMOTIVE-ADAS** -- automotive optical/ADAS sensor modules:
  ISO 26262 (road-vehicle functional safety, commonly referenced for
  qualifying ADAS-grade camera/LIDAR sensor modules), ISO 20653 (which
  defines the IP6K9K ingress-protection rating commonly required for
  exterior-mounted automotive camera/optical-sensor housings). SAE
  J3088 is also cited as a plausible ADAS/camera-calibration-adjacent
  reference, but disclosed HONESTLY with LOW confidence (this session
  could not verify J3088's exact current title/scope without live web
  access) and held OUT of the hard `:required-evidence` gate -- see
  `opticsworks.facts` for the full disclosure.

A product class not in this table (e.g. the demo's
`"MEDDEV-OPTICS"` scheme) has NO spec-basis and the Module-Seating
Governor HARD-holds rather than inventing one -- see
`opticsworks.facts` for the full coverage discipline.

## Social / regulatory hand-off

```clojure
(require '[opticsworks.store :as store]
         '[opticsworks.export :as export])

(def db (store/seed-db))
(export/audit-package db)           ;; EDN maps for downstream-consumer/audit hand-off
(export/package->csv-bundle db)     ;; CSV bundle (optical-module-batches/ledger/shipments/optical-certificates)
```

Operator console (static sample): `docs/samples/operator-console.html`.

## Develop

```bash
clojure -M:dev:test
clojure -M:lint
clojure -M:dev:run
```

## License

AGPL-3.0-or-later — see `LICENSE`.

## Operator console (Pages)

After enabling GitHub Pages (Settings → Pages → GitHub Actions), the
static console is at:

https://cloud-itonami.github.io/cloud-itonami-isic-2670/

Local: open `docs/index.html` or `docs/samples/operator-console.html`.

## Export audit package (CLI)

```bash
clojure -M:dev:export
# or: clojure -M:dev:export /tmp/audit-2670
```

Writes CSV files under `out/audit-package/` (or the given directory).

## Render-harness scene export (CLI)

```bash
clojure -M:dev:render-export
```

Runs the REAL `opticsworks.robotics/simulate-lens-seating` `physics-2d`
trajectory (nominal, passing configuration) and dumps the seating-press
and lens-housing rigid bodies' real per-tick positions as JSON to
`/tmp/render-2670/scene-data.json` and `docs/samples/scene-data.json`,
for a downstream render harness to visualize.
