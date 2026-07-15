(ns opticalmfg.registry
  "Pure-function domain logic for the optical-instrument-and-
  photographic-equipment plant-operations coordination actor --
  equipment/batch verification, shipment-quantity recompute,
  instrument-class validation, resolution-test plausibility
  validation, defect-rate plausibility validation, and draft
  maintenance-schedule/shipment-coordination record construction.

  Per docs/adr/0001-architecture.md Decision 1: this vertical has NO
  pre-existing `kotoba-lang/opticalmfg`-style capability library to
  wrap (verified: no such repo exists, and no `optics`/`opticalinstr`-
  named repo exists in kotoba-lang either). The domain logic therefore
  lives here as pure functions, re-verified INDEPENDENTLY by
  `opticalmfg.governor` -- the same 'ground truth, not self-report'
  discipline every sibling actor's own registry establishes (e.g.
  `medinstrmfg.registry/shipment-quantity-exceeded?` from
  `cloud-itonami-isic-3250`, and `watchmfg.registry` from
  `cloud-itonami-isic-2652`): never trust a proposal's own
  self-reported quantity/status when the inputs needed to recompute it
  independently are already on record.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real plant-operations system. It builds the DRAFT record
  a plant coordinator would keep (a scheduled maintenance window, a
  coordinated shipment), not the act of actuating lens-grinding/
  optics-assembly/testing equipment or dispatching a real freight
  carrier, and never the act of self-issuing an IEC 60825-1 laser
  safety class certification (this actor NEVER does any of those --
  see README `What this actor does NOT do`).

  SCOPE: ISIC 2670 covers manufacture of optical instruments and
  photographic equipment -- lens-grinding (except spectacle lenses),
  optics-assembly (binoculars, microscopes except electron/proton
  microscopes, telescopes, other optical instruments), and testing
  lines (resolution/MTF testing, laser-alignment/collimation testing)
  producing finished lenses, binoculars, microscopes, telescopes,
  cameras and projectors. This actor coordinates the back-office
  record-keeping around that plant (production-batch logging,
  maintenance scheduling, safety-concern flagging, shipment
  coordination) -- it never touches the lens-grinding/optics-assembly/
  testing equipment directly, and it never stands in for the
  accredited testing laboratory that issues IEC 60825-1 laser safety
  class certifications.")

;; ----------------------------- constants -----------------------------

(def valid-instrument-classes
  "The closed set of instrument-class values a production-batch record
  may declare -- the core optical instrument and photographic
  equipment categories ISIC 2670 covers (excluding spectacles/
  spectacle lenses, which fall outside this class, and electron/proton
  microscopes, which are excluded by definition). Anything else is a
  fabricated/unrecognized instrument class -- the governor HARD-holds
  rather than let an invented classification pass through. This actor
  never DECIDES a batch's instrument classification (that is the
  manufacturer's production-engineering function); it only validates
  that a batch record declares one of the real, known values."
  #{:optical-lens :binocular :microscope :telescope :camera :projector})

(def resolution-test-line-pairs-per-mm-min
  "Physical floor for a batch's own resolution-test reading (USAF-1951-
  style target, in resolved line pairs per millimeter). A reading of
  exactly zero (or negative) is not how a real resolution test reports
  a result -- even a badly out-of-focus or defective lens still
  resolves the target's coarsest group at some small positive spatial
  frequency; a non-positive reading is a data-entry error or an
  unresolved-test placeholder, never a genuine numeric resolution
  reading."
  0.1)

(def resolution-test-line-pairs-per-mm-max
  "Physical ceiling for a batch's own resolution-test reading, in
  resolved line pairs per millimeter. Grounded in the diffraction limit
  for visible-light optics: per the Rayleigh criterion at ~550nm, even
  the highest-NA (~1.4) diffraction-limited microscope objectives top
  out at roughly 1000-1200 lp/mm resolving power; ordinary camera,
  binocular, telescope and projector lenses resolve far less. A
  reading beyond 1200 lp/mm exceeds what any real visible-light optical
  system can resolve -- implausible/fabricated test data, not a real
  resolution-test result."
  1200.0)

(def defect-rate-min-percent
  "Physical floor for a batch's own lens-grinding/optics-assembly/
  testing defect-rate reading (zero defects is the best possible
  outcome, never negative)."
  0.0)

(def defect-rate-max-percent
  "Physical ceiling for a batch's own lens-grinding/optics-assembly/
  testing defect-rate reading -- a batch cannot reject more than 100%
  of its own output. A reading above this is implausible sensor/QC
  data, not a real batch."
  100.0)

;; ----------------------------- equipment checks -----------------------------

(defn equipment-verified?
  "Ground-truth check: has `equipment`'s own record been marked
  verified (i.e. it has actually been inspected/commissioned and
  registered in the SSoT, not merely referenced from an unverified
  maintenance request)? A pure predicate over the equipment's own
  permanent field -- no proposal inspection needed."
  [equipment]
  (true? (:verified? equipment)))

(defn equipment-registered?
  "Ground-truth check: does `equipment`'s own record carry a
  `:registered?` true flag (i.e. it is on file in the plant's
  equipment registry)? Scheduling maintenance against equipment that
  is not on file and registered is the exact scope violation this
  actor's HARD invariant ('plant/batch record must be independently
  verified/registered before any action') exists to block."
  [equipment]
  (true? (:registered? equipment)))

(defn equipment-ready?
  "Combined ground-truth gate: the equipment must be both `verified?`
  AND `registered?` before ANY maintenance may be scheduled against
  it. Two independent facts on the equipment's own permanent record,
  neither inferred from the advisor's own rationale."
  [equipment]
  (and (equipment-verified? equipment) (equipment-registered? equipment)))

;; ----------------------------- batch checks -----------------------------

(defn batch-verified?
  "Ground-truth check: has `batch`'s own record been marked verified
  (i.e. its instrument-class/resolution-test/quantity/defect-rate
  claims have actually been QC-inspected, not merely logged from an
  unverified intake patch)?"
  [batch]
  (true? (:verified? batch)))

(defn batch-registered?
  "Ground-truth check: is `batch`'s own record on file in the plant's
  production ledger? Coordinating a shipment against a batch that is
  not on file and registered is the exact scope violation this
  actor's HARD invariant ('plant/batch record must be independently
  verified/registered before any action') exists to block."
  [batch]
  (true? (:registered? batch)))

(defn batch-ready?
  "Combined ground-truth gate: the batch must be both `verified?` AND
  `registered?` before ANY shipment may be coordinated against it."
  [batch]
  (and (batch-verified? batch) (batch-registered? batch)))

(defn shipment-quantity-exceeded?
  "Ground-truth check for a `:coordinate-shipment` proposal:
  would `shipped-units` + `new-units` exceed `batch`'s own recorded
  `:quantity-units` (the batch's own logged production quantity)?
  Needs no proposal inspection or stored-verdict lookup -- its inputs
  are permanent fields already on the batch's own record, the same
  shape every sibling actor's own cost/total-matching check uses."
  [batch new-units]
  (let [capacity (:quantity-units batch)
        so-far (:shipped-units batch 0.0)]
    (and (number? capacity)
         (number? new-units)
         (> (+ (double so-far) (double new-units)) (double capacity)))))

(defn instrument-class-valid?
  "Is `instrument-class` one of the closed, known instrument-class
  values? nil/blank is treated as invalid (a production-batch patch
  must declare a real instrument class, not omit it silently)."
  [instrument-class]
  (contains? valid-instrument-classes instrument-class))

(defn resolution-test-line-pairs-per-mm-valid?
  "Is `lp-per-mm` a physically plausible resolution-test reading (USAF-
  1951-style target, in resolved line pairs per millimeter)? Rejects
  nil, non-numbers, values at or below zero, and values beyond the
  visible-light diffraction limit -- a fabricated or sensor-error
  reading, never let through as a real test result."
  [lp-per-mm]
  (and (number? lp-per-mm)
       (>= (double lp-per-mm) resolution-test-line-pairs-per-mm-min)
       (<= (double lp-per-mm) resolution-test-line-pairs-per-mm-max)))

(defn defect-rate-valid?
  "Is `percent` a physically plausible batch lens-grinding/optics-
  assembly/testing defect-rate reading? Rejects nil, non-numbers,
  negative values, and values beyond `defect-rate-max-percent` -- a
  fabricated or sensor-error reading, never let through as a real
  batch fact."
  [percent]
  (and (number? percent)
       (>= (double percent) defect-rate-min-percent)
       (<= (double percent) defect-rate-max-percent)))

;; ----------------------------- draft record construction -----------------------------

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the human plant supervisor's/shipping approver's act, not this
  actor's. And NEVER an IEC 60825-1 laser safety class certification --
  this actor is never the certifying authority (see README `What this
  actor does NOT do`)."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn register-maintenance
  "Validate + construct the MAINTENANCE-SCHEDULE DRAFT -- a proposed
  lens-grinding/optics-assembly/testing-equipment maintenance window
  against a verified, registered piece of equipment. Pure function --
  does not actuate the lens-grinding/optics-assembly/testing equipment
  or execute any maintenance; it builds the RECORD a plant coordinator
  would keep. `opticalmfg.governor` independently re-verifies the
  equipment's own verified/registered ground truth, and permanently
  blocks any attempt to directly actuate lens-grinding/optics-
  assembly/testing equipment (see README `Actuation`), before this is
  ever allowed to commit."
  [maintenance-id equipment-id sequence]
  (when-not (and maintenance-id (not= maintenance-id ""))
    (throw (ex-info "maintenance: maintenance_id required" {})))
  (when-not (and equipment-id (not= equipment-id ""))
    (throw (ex-info "maintenance: equipment_id required" {})))
  (when (< sequence 0)
    (throw (ex-info "maintenance: sequence must be >= 0" {})))
  (let [maintenance-number (str "MNT-" (zero-pad sequence 6))
        record {"record_id" maintenance-number
                "kind" "maintenance-schedule-draft"
                "maintenance_id" maintenance-id
                "equipment_id" equipment-id
                "immutable" true}]
    {"record" record "maintenance_number" maintenance-number
     "certificate" (unsigned-certificate "MaintenanceSchedule" maintenance-number maintenance-number)}))

(defn register-shipment
  "Validate + construct the SHIPMENT-COORDINATION DRAFT -- a proposed
  outbound optical-instrument/photographic-equipment shipment against a
  verified, registered production batch. Pure function -- does not
  dispatch any real freight carrier; it builds the RECORD a plant
  coordinator would keep. `opticalmfg.governor` independently
  re-verifies the shipment's own claimed quantity against
  `shipment-quantity-exceeded?`, before this is ever allowed to
  commit."
  [shipment-id sequence]
  (when-not (and shipment-id (not= shipment-id ""))
    (throw (ex-info "shipment: shipment_id required" {})))
  (when (< sequence 0)
    (throw (ex-info "shipment: sequence must be >= 0" {})))
  (let [shipment-number (str "SHP-" (zero-pad sequence 6))
        record {"record_id" shipment-number
                "kind" "shipment-coordination-draft"
                "shipment_id" shipment-id
                "immutable" true}]
    {"record" record "shipment_number" shipment-number
     "certificate" (unsigned-certificate "ShipmentCoordination" shipment-number shipment-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
