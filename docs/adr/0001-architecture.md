# ADR-0001: Optics Advisor ⊣ Module-Seating Governor architecture

- Status: Accepted (2026-07-15)
- Repository: `cloud-itonami-isic-2670` (ISIC Rev.5 `2670`)

## Context

Optical-instrument/photographic-equipment manufacturing (lens-barrel
press-fit seating-test verification, per-product-class optical-standard
evidence verification, end-of-line optical-clarity/dust-ingress/
dead-pixel inspection, Optical Module Test Certificate issuance) needs
the same governed-actor pattern as the rest of the cloud-itonami fleet:
an untrusted advisor proposes; an independent governor may HOLD;
high-stakes actuation never auto-commits.

The industry-registry entry for `2670` had sat with no repo, no
business model, no actor (a plain "plant operations coordination" stub
with no physics simulation and no real optical-standard evidence
catalog). A value-chain review found `cloud-itonami-isic-2630`
(communication-equipment/smartphone assembly) and
`cloud-itonami-isic-2910`/`cloud-itonami-isic-2920` (motor-vehicle/body
assembly) both implemented, each consuming a finished camera/optical
sensor module as an input, but the camera/optical-module manufacturing
stage feeding BOTH chains had no real actor -- the same "missing shared
upstream stage" gap `cloud-itonami-isic-2220` (injection-molded
plastics) closed for both chains' housing/trim components.

This vertical adopts ADR-2607151600/ADR-2607152000's real-engineering-
simulation fleet pattern NATIVELY from day one -- mirroring how
`cloud-itonami-isic-2220`/`cloud-itonami-isic-2720`/`cloud-itonami-isic-2310`
were each built real-physics-first.

## Decision

1. Namespaces live under `opticsworks.*` with the standard facts /
   registry / store / governor / phase / advisor / operation / sim /
   robotics / export shape.
2. Entity is an **optical-module-batch** (a manufactured lot of camera/
   optical modules of one spec -- either a smartphone camera-module
   batch or an automotive ADAS optical/camera sensor-module batch), not
   a finished device or a finished vehicle.
3. Dual actuation on the same entity:
   - `:actuation/ship-optical-module-batch` (robot optical-module-
     batch-shipment dispatch draft, onward to a downstream consumer --
     the real dual hand-off to BOTH `cloud-itonami-isic-2630`'s
     smartphone camera-module integration and `cloud-itonami-isic-2910`/
     `cloud-itonami-isic-2920`'s automotive backup-camera/ADAS
     optical-sensor-module integration)
   - `:actuation/issue-optical-certificate` (Optical Module Test
     Certificate draft: resolution/MTF test report + safety compliance)
4. Double-actuation guards use dedicated booleans
   (`:optical-module-batch-shipped?`, `:optical-certified?`), never a
   status lifecycle (ADR-2607071320 / 6492 lesson).
5. `optical-module-batch-focus-back-distance-out-of-range?` continues
   the fleet two-sided range check family, applied here to a batch's
   own measured focus-back-distance deviation from its own optical
   spec's nominal value -- a real end-of-line optical-alignment QA
   metric, distinct from the physics-derived seating-force check.
6. `opticsworks.robotics` delivers a REAL, time-stepped `physics-2d`
   rigid-body lens-barrel press-fit seating-test simulation from day
   one (not a symbolic field comparison, and not a retrofit): a moving
   seating-press `Body2D` closes at a controlled velocity onto a static
   lens-housing seating-register `Body2D`; `:sim-peak-seating-force-n`
   is read directly off the actual simulated collision trajectory. The
   governor HARD-holds if the mission never ran, OR if an independent
   recompute of the batch's own `:sim-peak-seating-force-n` falls
   outside the batch's own recorded `:seating-force-min-n`/
   `:seating-force-max-n` acceptance band (a REASONED ENGINEERING
   ESTIMATE for real lens-barrel press-fit seating forces, disclosed
   HONESTLY as a moderate-confidence estimate, not a single formal
   standard's numeric threshold) -- never trusting the mission's
   self-reported verdict. UNLIKE `moldworks.robotics`'s deliberately
   ONE-SIDED clamp-tonnage check, this check is TWO-SIDED: too little
   seating force risks a loose/misaligned lens (focus drift), too much
   risks a cracked lens element or housing -- both are real, distinct
   defect-risk directions for a press-fit seating operation.
7. Optical-standard scheme catalog (`opticsworks.facts`) seeds
   SMARTPHONE-CAMERA (ISO 12233 + IEC 60825-1) and AUTOMOTIVE-ADAS
   (ISO 26262 + ISO 20653/IP6K9K) only; SAE J3088 is cited as a
   plausible ADAS/camera-calibration-adjacent reference but disclosed
   as LOW-confidence and held OUT of the hard `:required-evidence`
   gate, since this session could not verify its exact scope/title
   without live web access; missing product classes (e.g. medical-
   device imaging optics) are uncovered, never fabricated.
8. End-of-line defect (optical-clarity/dust-ingress/dead-pixel)
   unresolved is evaluated unconditionally so
   `:end-of-line-quality/screen` itself can HARD-hold (parksafety
   ADR-2607071922 Decision 5 discipline, same as `moldworks.governor`'s/
   `automotive.governor`'s/`cellworks.governor`'s/`glassworks.governor`'s
   end-of-line-defect-unresolved checks).

## Consequences

(+) The camera/optical-module manufacturing stage gains a forkable OSS
operating stack with auditable governor holds, closing a gap common to
BOTH the smartphone-assembly and vehicle-assembly value chains -- the
SAME dual-downstream-hand-off shape `cloud-itonami-isic-2220`/
`cloud-itonami-isic-2720`/`cloud-itonami-isic-2310` established for
plastics, batteries and glass.
(+) Delivers a REAL time-stepped physics simulation (not a symbolic
comparison) as a native part of this actor's initial build, extending
ADR-2607151600/ADR-2607152000's fleet pattern to a NEW actor rather
than retrofitting an existing symbolic one -- and anchors its
tolerance band on a REAL, disclosed reasoned-engineering-estimate
(low-single-digit-to-tens-of-newtons for consumer optics, higher for
ruggedized automotive housings), honestly disclosed as a moderate-
confidence estimate rather than a single formal-standard number.
(+) Genuine dual-downstream hand-off value: the same optical-module-
batch-shipment/certificate shape serves both `cloud-itonami-isic-2630`
and `cloud-itonami-isic-2910`/`cloud-itonami-isic-2920` without this
actor needing to know which downstream consumer a given shipment goes
to.
(-) No physical plant digital-twin tick beyond the single lens-seating
physics check in this repo (follow-up domain data, e.g. an MTF/
resolution optical-simulation, is out of scope here -- `physics-2d` has
no optical/ray-tracing model at all).
(-) Optical-standard-scheme coverage is a starting catalog (2 product
classes), not exhaustive, and does not capture every product class an
optical-module manufacturer might produce (e.g. medical-imaging optics,
industrial machine-vision optics).
(-) `physics-2d` is a 2D projection with no material-stiffness/
deformation model, and both the seating-press tool and the lens-housing
seating register are approximated as flat-plate AABBs (a disclosed
simplification necessitated by `physics-2d`'s narrowphase) -- see
`opticsworks.robotics`'s own docstring for the full disclosure.
(-) The SAE J3088 citation is disclosed as LOW-confidence (this session
could not verify its exact scope/title without live web access) --
flagged honestly rather than presented as certain, and held out of the
hard evidence gate.

## Related

- ADR-2607011000 (robotics premise + ISIC coverage)
- ADR-2607151600 (real engineering-simulation integration, automotive
  pilot)
- ADR-2607152000 (real engineering-simulation fleet extension)
- Sibling architecture: `cloud-itonami-isic-2220` docs/adr/0001 (closest
  physics-2d technical analog: a moving rigid body vs. a static rigid
  body, force derived from F=m*a off the real simulated collision),
  `cloud-itonami-isic-2732` `src/harnessworks/robotics.cljc` (crimp
  tensile-pull-test analog), `cloud-itonami-isic-2630`
  `src/commsdevice/robotics.cljc` (two-sided press-pressure band
  analog + product-class facts-catalog structure)
