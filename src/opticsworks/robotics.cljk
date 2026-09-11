(ns opticsworks.robotics
  "Robot-executed lens-barrel press-fit SEATING-TEST verification -- the
  concrete, actor-level realization of ADR-2607011000's robotics
  premise (every cloud-itonami vertical is designed on the premise that
  a robot performs the physical-domain work; an independent governor
  gates any action before it ever reaches hardware), delivered NATIVELY
  onto ADR-2607151600/ADR-2607152000's real-engineering-simulation
  fleet pattern from day one (this vertical, isic-2670, is a NEW actor
  built to that same standard from day one, mirroring how
  `cloud-itonami-isic-2220`/`cloud-itonami-isic-2720`/`cloud-itonami-
  isic-2310` deliver it natively rather than retrofitted; closest
  technical analog: `moldworks.robotics`'s injection-mold clamping-force
  verification -- a moving rigid body closing onto a static rigid body,
  force derived from F=m*a off the real simulated collision, honest
  confidence disclosure on the tolerance band; also see `harnessworks.
  robotics`'s crimp tensile-pull test and `commsdevice.robotics`'s
  two-sided display-bonding-pressure band) for THIS actor's own
  manufacturing-process evidence requirement: an optical-module-batch-
  shipment proposal must cite a real lens-seating press-fit
  verification report actually on file -- not merely a self-reported
  checklist string.

  The lens-seating step of the mission is an ACTUAL time-stepped
  `kotoba-lang/physics-2d` rigid-body simulation of a REAL, standard
  optical-module assembly QA step: a moving seating-press `Body2D` (the
  press tool that drives the lens barrel into its housing at a
  controlled closing velocity) closes onto a static (mass 0, immovable
  -- the SAME `moldworks.robotics`/`cellworks.robotics`/`glassworks.
  robotics` fixture/anvil pattern) lens-housing seating-register
  `Body2D` (the housing feature the barrel's flange bottoms out
  against). `world-step` actually integrates/collides/resolves the
  contact over real ticks, and `:sim-peak-seating-force-n` is read
  directly off the ACTUAL simulated velocity trajectory (F = m*a, the
  SAME technique every real-physics sibling in this fleet uses) -- not
  invented.

  A robot mission (`kotoba.robotics/mission`) walks the optical-module-
  batch through three steps in the lens-seating cell -- a pre-press
  barrel-alignment check, the press-fit seating cycle itself, and a
  post-press focus-back-distance scan -- built with `kotoba.robotics/
  action` + `kotoba.robotics/telemetry-proof`, and reports an overall
  :passed? verdict derived from the REAL simulated seating reading
  (`:sim-peak-seating-force-n`, see `seating-telemetry-for`), not a
  hand-set field. `simulation-out-of-tolerance?` independently
  re-derives that verdict from the batch's OWN recorded real telemetry
  cross-checked against the batch's OWN recorded `:seating-force-min-n`/
  `:seating-force-max-n` acceptance band, never from the mission's
  self-reported result -- the SAME 'ground truth, not self-report'
  discipline `opticsworks.registry/optical-module-batch-focus-back-
  distance-out-of-range?` uses for focus-back-distance deviation.
  `opticsworks.governor`'s `robotics-simulation-violations` calls this
  ns's independent recheck, never the stored :passed? value, before any
  `:actuation/ship-optical-module-batch` proposal may commit.

  Honest scope + citation disclosure (mirrors every real-physics
  sibling's own disclosure style, ADR-2607151600/ADR-2607152000):

  - 2D projection only (`physics-2d` has no 3D solver) -- x is the
    seating-press's direction of travel (press-fit closing axis); world
    gravity is [0 0] (a horizontal press-closing projection -- press-fit
    seating cells commonly orient the press axis horizontally or
    vertically depending on cell layout; the axis choice itself carries
    no physical consequence for this 2D, gravity-free simulation).
  - the lens-housing seating register is modeled as a STATIC (mass 0)
    AABB, mirroring `moldworks.robotics`'s static mold-half /
    `commsdevice.robotics`'s static display-stack pattern: `physics-2d`
    treats a mass-0 body as having zero inverse mass (an immovable
    anchor), which is also physically apt here -- the housing is held
    in a fixture/nest during seating, not free to recoil. `physics-2d`
    has NO material-stiffness/deformation model whatsoever, so neither
    the lens barrel's own polymer/metal rigidity nor the housing's own
    rigidity varies the simulated reading (the SAME disclosed
    limitation every real-physics sibling states) -- what DOES vary the
    reading is this optical-module-batch's own recorded press-run
    configuration (`:seating-press-effective-mass-kg`, see `seating-
    telemetry-for`), the SAME disclosed lever every real-physics
    sibling in this fleet uses.
  - BOTH the seating-press tool and the lens-housing seating register
    are modeled as rectangular AABBs (a flat plate on the travel axis)
    -- a disclosed simplification necessitated by `physics-2d`'s
    narrowphase, which only supports AABB-vs-AABB or circle-vs-circle
    pairs (`test-collision` returns nil for mixed pairs); a real lens
    barrel and housing seating register have a far more complex
    cylindrical/tapered geometry, but the SEATING contact event this ns
    actually models is the barrel flange's own flat face meeting the
    housing's own flat seating shoulder, which a flat AABB genuinely
    represents honestly at the moment of seating, not merely as a
    stand-in.
  - `seating-press-velocity-mps`/`seating-travel-m` (the lens barrel's
    own real final press-fit engagement/seating travel before its
    flange bottoms against the housing's seating shoulder) are this
    ns's own disclosed engineering priors, NOT measured facts for any
    specific optical-module-batch -- `physics-2d` has no material-
    compliance model at all, so this namespace cannot derive them from
    first principles; they only need to be SOME disclosed distance/rate
    pair that derives a physically meaningful timestep, the SAME role
    `moldworks.robotics`'s `mold-approach-travel-m`/`mold-closing-
    velocity-mps` and `commsdevice.robotics`'s `crush-travel-m`/`press-
    closing-velocity-mps` play there. UNLIKE `moldworks.robotics`'s
    truly ARBITRARY rigid-clamp-stop distance (a rigid mold clamp has NO
    analogous deformation/give distance at all), a lens-barrel press-fit
    seating operation genuinely DOES have a real, physical final
    engagement travel (the barrel's own interference-fit engagement
    zone immediately before the flange seats) -- `seating-travel-m`
    (0.5mm) is a disclosed, plausible order-of-magnitude figure for that
    real engagement travel on a compact (sub-10mm-diameter-class) lens
    barrel, consistent in spirit with `commsdevice.robotics`'s own
    disclosed OCA-layer-thickness prior, not a literal per-batch
    measured value. `seating-press-velocity-mps` (0.2 m/s) is a
    disclosed ANALOG closing rate, NOT a literal transcription of any
    one real press-fit cell's actual controlled seating speed (real
    precision press-fit seating speeds for delicate optical assemblies
    are typically much slower, low mm/s, to avoid shock-loading the
    lens element) -- `physics-2d`'s impulse resolver has NO progressive
    force-vs-displacement stiffness model at all (the SAME disclosed
    limitation every real-physics sibling states): whatever tick first
    detects ANY AABB overlap fully zeroes the closing velocity in that
    ONE tick (restitution 0) -- a discrete, instantaneous stop, not a
    real press-fit cell's actual controlled deceleration ramp. This ns
    uses a faster, disclosed analog rate instead, the SAME disclosed
    choice every real-physics sibling in this fleet makes.
  - By exact kinematic identity (peak deceleration = closing-velocity /
    dt for a single-tick full stop against an immovable body, the SAME
    verified, documented property every real-physics sibling in this
    fleet establishes -- mass cancels algebraically in `physics-2d`'s
    `resolve-contact` when colliding with a mass-0 body), the peak
    deceleration itself is INDEPENDENT of `seating-press-effective-
    mass-kg` -- so `:seating-press-effective-mass-kg` is the ONLY
    quantity that scales `:sim-peak-seating-force-n` for a fixed
    closing velocity/approach distance (via F = m*a), never the closing
    velocity or seating travel (both fixed constants, shared by every
    optical-module-batch). A heavier effective mass (press-tool head +
    the locally-engaged barrel/housing material) stands in for a
    stiffer/larger-scale seating-press configuration -- disclosed as an
    illustrative simulation parameter, not a literal per-machine-model
    spec transcription.
  - `seating-force-band-n` -- this ns's own disclosed, TWO-SIDED
    REASONED ENGINEERING ESTIMATE for real lens-barrel press-fit
    seating forces, keyed by product class. HONEST CONFIDENCE
    DISCLOSURE: this band is a REASONED ENGINEERING ESTIMATE informed
    by commonly-cited compact-camera-module lens-seating/press-fit
    assembly forces (roughly low-single-digit-to-tens of newtons for
    consumer smartphone optics, plausibly higher -- tens of newtons --
    for ruggedized automotive ADAS-grade housings), NOT a verbatim
    transcription of a single formal standard's numeric table, the SAME
    moderate-confidence disclosure discipline `moldworks.robotics`'s
    cavity-pressure-factor bands and `commsdevice.robotics`'s bonding-
    pressure band use for their own reasoned-estimate figures.
    `:consumer-grade` (1-20 N) covers smartphone/compact-camera-module
    lens barrels; `:ruggedized-grade` (5-60 N) covers automotive ADAS
    optical/camera-sensor-module housings, whose larger barrels/seals
    and higher required retention plausibly call for a higher seating
    force. This table SEEDS the demo/seed data's own per-batch
    `:seating-force-min-n`/`:seating-force-max-n` fields (see `store/
    demo-data`); the actual governor check (`lens-seating-force-out-
    of-tolerance?` below) reads those STORED per-batch fields directly
    (ground truth already on file), the SAME 'ground truth, not a
    re-derived formula' discipline `commsdevice.robotics`'s
    `bonding-pressure-out-of-range?` uses for its own acceptance band.
  - `lens-seating-force-out-of-tolerance?` is TWO-SIDED (unlike
    `moldworks.robotics`'s deliberately ONE-SIDED clamp-tonnage check):
    too little seating force is a real defect-risk direction (the lens
    barrel sits loose/misaligned in its housing, causing focus drift or
    optical-axis tilt under vibration/thermal cycling); too much
    seating force is ALSO a real, distinct defect-risk direction (the
    press can crack the lens element itself, or crack/deform the
    housing's own seating shoulder) -- a genuine two-sided failure mode
    for a precision press-fit assembly step, unlike injection-mold
    clamping (where only under-clamping is a real defect-risk
    direction, per `moldworks.robotics`'s own disclosed asymmetry).

  Pure data + pure functions -- no real robot I/O, no network.
  `physics-2d/world-step` is itself a pure, fixed-timestep integrator
  (no wall-clock/IO), so this stays exactly as offline/deterministic as
  every other sibling namespace in this actor -- tests and the demo run
  without a network.

  Honest scope: this DOES model a real time-stepped `physics-2d` rigid-
  body trajectory for the seating-press collision event, along the
  press's own real travel axis, and derives a real seating-force
  reading directly comparable to a real, disclosed reasoned-engineering
  seating-force band. It does NOT model: lens-element optical
  performance (MTF/resolution -- `physics-2d` has no ray-tracing/optics
  model at all), 3D barrel/housing thread or interference-fit geometry
  (2D projection, flat-plate approximation only), a real load-cell/
  press-force-sensor/DAQ connection, or a real press-cell servo-motion-
  planning/control system -- still simulation, not control, the same
  'policy, not control' boundary `kotoba.robotics`'s docstring already
  establishes."
  (:require [kotoba.robotics :as robotics]
            [physics-2d :as p2d]))

;; ------------------------- real physics-2d seating constants -------------------------

(def ^:const seating-press-velocity-mps
  "The seating-press tool's controlled final-approach closing velocity
  (m/s) for THIS simulation -- a disclosed ANALOG rate, NOT a literal
  transcription of any one real press-fit cell's actual (typically much
  slower, shock-averse) controlled seating speed. See ns docstring for
  why."
  0.2)

(def ^:const seating-travel-m
  "The lens barrel's own real final press-fit engagement/seating travel
  (m) before its flange bottoms against the housing's seating shoulder
  -- a disclosed, plausible order-of-magnitude figure (0.5mm) for a
  compact (sub-10mm-diameter-class) lens barrel's real interference-fit
  engagement zone, NOT a literal per-batch measured distance. See ns
  docstring for why this genuinely has a real physical analog, unlike
  `moldworks.robotics`'s truly arbitrary rigid-clamp-stop distance."
  0.0005)

(def ^:const dt
  "Per-tick timestep (s) -- derived from THIS simulation's own
  seating-travel/closing-velocity (the nominal transit time across the
  lens barrel's own final-engagement zone), the SAME principled-not-
  arbitrary identity every real-physics sibling uses for its own `dt`."
  (/ seating-travel-m seating-press-velocity-mps))

(def ^:const seating-press-half-w-m
  "Seating-press tool AABB half-width (m) along the travel axis -- a
  disclosed, arbitrary rigid-body stand-in (4mm full thickness for the
  press-tool head); `physics-2d` colliders do not deform, so this
  dimension is not a load-bearing physical parameter."
  0.002)

(def ^:const seating-press-half-h-m
  "Seating-press tool AABB half-height (m), lateral -- 8mm full width,
  a representative compact lens-barrel-diameter-class press-tool head
  footprint."
  0.004)

(def ^:const lens-housing-half-w-m
  "Lens-housing seating-register AABB half-width (m) along the travel
  axis -- same disclosed rigid-body stand-in dimension as the
  seating-press tool."
  0.002)

(def ^:const lens-housing-half-h-m
  "Lens-housing seating-register AABB half-height (m), lateral -- same
  lateral extent as the seating-press tool, so the whole modeled
  seating-shoulder width interacts."
  0.004)

(def ^:const gap-m
  "Seating-press standoff distance (m) the press tool starts behind the
  lens-housing seating register, so the trajectory captures a real
  pre-contact approach phase, not just the collision tick itself
  (mirrors every sibling's own gap constant). Deliberately NOT an exact
  multiple of `seating-press-velocity-mps` x `dt` so the simulated
  approach genuinely overshoots the contact plane by a sub-tick
  remainder before positional correction resolves it -- keeping
  `:sim-peak-seating-travel-m` a genuinely observed, nonzero simulated
  reading rather than a coincidental exact-alignment zero."
  0.0006)

(def ^:const settle-ticks
  "Extra ticks appended after the seating-press tool is expected to
  reach the lens-housing seating register, so the trajectory also
  captures post-contact settling -- the SAME constant + rationale as
  every real-physics sibling: `physics-2d`'s positional correction
  removes 80% of any remaining overlap per tick, so residual overlap
  after 15 more ticks is ~3e-11 of whatever it was at first contact."
  15)

(def seating-force-band-n
  "Real, disclosed reasoned-engineering-estimate lens-barrel press-fit
  seating-force bands (Newtons) per product class -- see ns docstring
  for the full honest confidence disclosure (a moderate-confidence
  engineering estimate, not a single formal standard). `:consumer-grade`
  covers smartphone/compact-camera-module lens barrels;
  `:ruggedized-grade` covers automotive ADAS optical/camera-sensor-
  module housings."
  {:consumer-grade   {:min 1.0 :max 20.0 :confidence :reasoned-engineering-estimate}
   :ruggedized-grade {:min 5.0 :max 60.0 :confidence :reasoned-engineering-estimate}})

(defn seating-force-band-for
  "This ns's own disclosed seating-force band ({:min :max}, Newtons) for
  `product-class` -- used ONLY to seed demo/seed data (see `store/
  demo-data`); the actual governor check reads the batch's OWN stored
  `:seating-force-min-n`/`:seating-force-max-n` fields directly (ground
  truth already on file), never re-derives this formula per-batch. See
  ns docstring for the full honesty disclosure."
  [product-class]
  (select-keys (get seating-force-band-n product-class) [:min :max]))

;; ------------------------------ real simulation ------------------------------

(defn simulate-lens-seating
  "Time-steps a REAL `physics-2d` world for ONE lens-barrel press-fit
  seating-test cycle: a moving seating-press `Body2D` (mass
  `seating-press-effective-mass-kg`, velocity `seating-press-velocity-
  mps`) approaches and collides with a static (mass 0, immovable)
  lens-housing seating-register `Body2D`. Returns {:trajectory
  [{:tick :position :velocity} ...] (seating-press tool only)
  :sim-peak-seating-force-n n :sim-peak-seating-travel-m n :ticks n
  :dt n :closing-velocity-mps n}.

  `:sim-peak-seating-force-n` is `seating-press-effective-mass-kg` times
  the PEAK magnitude of tick-to-tick velocity change (along the travel
  axis) divided by `dt` -- F = m*a, derived from the ACTUAL simulated
  velocity trajectory (the SAME technique every real-physics sibling in
  this fleet uses). `:sim-peak-seating-travel-m` is the largest AABB
  penetration depth (m) actually observed between the seating-press
  tool's leading face and the lens-housing seating register's near face
  across the whole trajectory -- informational, derived from the actual
  simulated positions, not invented.

  Pure, deterministic -- the same `seating-press-effective-mass-kg`
  always reproduces the same telemetry; no IO, no wall-clock."
  [seating-press-effective-mass-kg]
  (let [v0 seating-press-velocity-mps
        approach-m (+ gap-m seating-press-half-w-m lens-housing-half-w-m)
        ticks (long (+ settle-ticks (long (Math/ceil (/ approach-m (* v0 dt))))))
        static-x 0.0
        moving-x (- static-x lens-housing-half-w-m seating-press-half-w-m gap-m)
        moving (p2d/make-body {:position [moving-x 0.0]
                                :velocity [v0 0.0]
                                :mass (double seating-press-effective-mass-kg)
                                :restitution 0.0
                                :friction 0.0
                                :collider (p2d/make-aabb-collider seating-press-half-w-m seating-press-half-h-m)
                                :user-data :seating-press})
        static (p2d/make-body {:position [static-x 0.0]
                                :velocity [0.0 0.0]
                                :mass 0.0
                                :restitution 0.0
                                :friction 0.0
                                :collider (p2d/make-aabb-collider lens-housing-half-w-m lens-housing-half-h-m)
                                :user-data :lens-housing})
        w0 (p2d/world-new [0.0 0.0])
        [w1 moving-id] (p2d/world-add w0 moving)
        [w2 _static-id] (p2d/world-add w1 static)
        worlds (reductions (fn [w _] (p2d/world-step w dt)) w2 (range ticks))
        trajectory (mapv (fn [tick world]
                            (let [b (nth (:bodies world) moving-id)]
                              {:tick tick :position (:position b) :velocity (:velocity b)}))
                          (range (count worlds)) worlds)
        vxs (mapv (comp first :velocity) trajectory)
        peak-decel-mps2 (->> (map (fn [va vb] (Math/abs (/ (- vb va) dt))) vxs (rest vxs))
                              (reduce max 0.0))
        contact-plane-x (- static-x lens-housing-half-w-m)
        penetrations-m (mapv (fn [{:keys [position]}]
                                (max 0.0 (- (+ (first position) seating-press-half-w-m) contact-plane-x)))
                              trajectory)
        peak-force-n (* (double seating-press-effective-mass-kg) peak-decel-mps2)]
    {:trajectory trajectory
     :sim-peak-seating-force-n peak-force-n
     :sim-peak-seating-travel-m (reduce max 0.0 penetrations-m)
     :ticks (count trajectory)
     :dt dt
     :closing-velocity-mps v0}))

(defn seating-telemetry-for
  "Runs the REAL `simulate-lens-seating` time-stepped `physics-2d`
  simulation for `batch`'s own recorded `:seating-press-effective-mass-
  kg` press-run configuration and returns the actual simulated
  telemetry: {:sim-peak-seating-force-n n :sim-peak-seating-travel-m n
  :ticks n :dt n :closing-velocity-mps n}. Pure, deterministic -- the
  same `:seating-press-effective-mass-kg` always reproduces the same
  telemetry."
  [batch]
  (select-keys (simulate-lens-seating (:seating-press-effective-mass-kg batch))
               [:sim-peak-seating-force-n :sim-peak-seating-travel-m
                :ticks :dt :closing-velocity-mps]))

(def mission-actions
  "The three-step lens-seating-verification-cell mission every optical-
  module-batch walks through before `:actuation/ship-optical-module-
  batch` is proposable. :sense at :none safety, :actuate at :low
  safety -- verification/QA handling of a stationary optical module,
  not the moving-shipment actuation that is `:actuation/ship-optical-
  module-batch` itself (always :safety-critical -- see `opticsworks.
  governor`)."
  [{:step :pre-press-barrel-alignment-check     :kind :sense   :safety :none}
   {:step :lens-barrel-press-fit-seating-cycle  :kind :actuate :safety :low}
   {:step :post-press-focus-back-distance-scan  :kind :sense   :safety :none}])

(defn lens-seating-force-out-of-tolerance?
  "Ground-truth check: does `batch`'s own recorded REAL `physics-2d`-
  simulated seating reading (`:sim-peak-seating-force-n`, see
  `seating-telemetry-for`) fall OUTSIDE `batch`'s own recorded
  [:seating-force-min-n :seating-force-max-n] acceptance band? TWO-
  SIDED -- see ns docstring for why both under- and over-pressed are
  real, distinct defect-risk directions for a lens-barrel press-fit
  seating operation (unlike `moldworks.robotics`'s deliberately
  one-sided clamp-tonnage check). Needs no mission run or proposal
  inspection once the telemetry and acceptance-band fields are on file
  -- its inputs are permanent fields already on the batch, the same
  shape `opticsworks.registry/optical-module-batch-focus-back-distance-
  out-of-range?` uses for focus-back-distance deviation."
  [{:keys [sim-peak-seating-force-n seating-force-min-n seating-force-max-n]}]
  (and (number? sim-peak-seating-force-n) (number? seating-force-min-n) (number? seating-force-max-n)
       (or (< sim-peak-seating-force-n seating-force-min-n)
           (> sim-peak-seating-force-n seating-force-max-n))))

(defn simulate-lens-seating-test
  "Run the robot-executed lens-seating-verification mission for
  `batch-id` (`batch` is the full record, incl. `:seating-press-
  effective-mass-kg`, `:seating-force-min-n`, `:seating-force-max-n`).
  Actually runs the REAL engine: `seating-telemetry-for` -- the actual
  `physics-2d`-stepped seating-press/lens-housing collision trajectory
  (`:sim-peak-seating-force-n`).

  Returns {:mission .. :actions [{:action .. :proof ..} ..] :passed?
  bool :sim-peak-seating-force-n n}. Deterministic: :passed? is derived
  from the batch's OWN recorded seating-run configuration via the REAL
  simulated trajectory (`lens-seating-force-out-of-tolerance?`), never
  invented or randomized -- `kotoba.robotics` mandates no network/IO,
  and a repeatable simulation is what makes the governor's independent
  recheck (`simulation-out-of-tolerance?`) meaningful."
  [batch-id batch]
  (let [telemetry (seating-telemetry-for batch)
        merged (merge batch telemetry)
        out-of-range? (lens-seating-force-out-of-tolerance? merged)
        reading (if out-of-range? :out-of-tolerance :nominal)
        mission (robotics/mission (str "mission-" batch-id "-lens-seating-verify")
                                   :robot/lens-seating-cell-1
                                   :lens-seating-verification
                                   :boundaries {:station "opticsworks-lens-seating-cell"}
                                   :max-steps (count mission-actions))
        actions (mapv (fn [{:keys [step kind safety]}]
                        (let [a (robotics/action (str (:mission/id mission) "-" (name step))
                                                  (:mission/id mission) kind safety
                                                  :params {:step step :batch-id batch-id})]
                          {:action a
                           :proof (robotics/telemetry-proof (:mission/id mission) step reading
                                                             :provenance :simulated)}))
                      mission-actions)]
    {:mission mission
     :actions actions
     :passed? (not out-of-range?)
     :sim-peak-seating-force-n (:sim-peak-seating-force-n telemetry)}))

(defn simulation-out-of-tolerance?
  "Independent ground-truth recheck for the governor: does `batch`'s
  OWN current, on-file real `physics-2d`-simulated seating-force
  telemetry (`:sim-peak-seating-force-n`) fall outside its own recorded
  acceptance band right now? Ignores whatever :passed? verdict a prior
  mission run stored -- identical in spirit to `opticsworks.registry/
  optical-module-batch-focus-back-distance-out-of-range?`'s refusal to
  trust a proposal's self-report. Does NOT re-run the simulation -- it
  re-derives the boolean from the real, already-persisted telemetry
  field (`opticsworks.store` persists it on every `:optical-module-
  batch/upsert`), the same 'ground truth, not self-report' discipline
  applied to the STORED reading, not a fresh recompute."
  [batch]
  (lens-seating-force-out-of-tolerance? batch))
