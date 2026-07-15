(ns opticsworks.registry
  "Pure-function optical-module-batch-shipment + Optical Module Test
  Certificate record construction -- an append-only camera/optical-
  module-manufacturer book-of-record draft.

  Like every sibling actor's registry, there is no single international
  check-digit standard for an optical-module-batch-shipment or Optical
  Module Test Certificate reference number -- every plant/scheme
  assigns its own reference format. This namespace does NOT invent one;
  it builds a jurisdiction/scheme-scoped sequence number and validates
  the record's required fields, the same honest, non-fabricating
  discipline `opticsworks.facts` uses.

  `optical-module-batch-focus-back-distance-out-of-range?` continues
  this fleet's two-sided range check family (`testlab.registry/within-
  tolerance?` established the first; `conservation.registry`/`water.
  registry`/`steelworks.registry`/`turbine.registry`/`automotive.
  registry`/`autoparts.registry`/`bodyshop.registry`/`cellworks.
  registry`/`glassworks.registry`/`moldworks.registry` are further
  siblings), applying the SAME lo/hi bounds-comparison shape to an
  optical-module-batch's own measured focus-back-distance deviation
  from its own optical spec's nominal value -- a real end-of-line
  optical-alignment QA metric, distinct from `opticsworks.robotics`'s
  own seating-force ground-truth check (a physics-derived press-process
  reading, not a post-assembly optical-alignment measurement).

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real plant/MES control system. It builds the RECORD a
  camera/optical-module manufacturer would keep, not the act of
  shipping the optical-module-batch robot action or issuing the Optical
  Module Test Certificate itself (that is `opticsworks.operation`'s
  `:actuation/ship-optical-module-batch`/`:actuation/issue-optical-
  certificate`, always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the camera/optical-module manufacturer's own act, not this actor's.
  See README `Actuation`."
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

(defn optical-module-batch-focus-back-distance-out-of-range?
  "Does `batch`'s own `:focus-back-distance-deviation-actual-um` fall
  outside its own `[:focus-back-distance-deviation-min-um
  :focus-back-distance-deviation-max-um]` recorded acceptance-band
  bounds (a micrometer-scale deviation from the optical module's own
  nominal focus-back-distance spec)? A pure ground-truth check against
  the batch's own permanent fields -- no upstream comparison needed,
  and no physics re-simulation needed (distinct from `opticsworks.
  robotics`'s own seating-force ground-truth check). A further sibling
  in this fleet's two-sided range check family (see ns docstring)."
  [{:keys [focus-back-distance-deviation-actual-um
           focus-back-distance-deviation-min-um
           focus-back-distance-deviation-max-um]}]
  (and (number? focus-back-distance-deviation-actual-um)
       (number? focus-back-distance-deviation-min-um)
       (number? focus-back-distance-deviation-max-um)
       (or (< focus-back-distance-deviation-actual-um focus-back-distance-deviation-min-um)
           (> focus-back-distance-deviation-actual-um focus-back-distance-deviation-max-um))))

(defn register-optical-module-batch-shipment
  "Validate + construct the OPTICAL-MODULE-BATCH-SHIPMENT registration
  DRAFT -- the camera/optical-module manufacturer's own act of
  dispatching a real robot handling/shipment action releasing an
  optical-module-batch onward to a downstream consumer (the real dual
  upstream hand-off to BOTH `cloud-itonami-isic-2630`'s smartphone
  camera-module integration and `cloud-itonami-isic-2910`/`cloud-
  itonami-isic-2920`'s automotive backup-camera/ADAS optical-sensor-
  module integration -- see README `Upstream -> downstream hand-off`).
  Pure function -- does not touch any real plant/MES control system; it
  builds the RECORD a camera/optical-module manufacturer would keep.
  `opticsworks.governor` independently re-verifies the batch's own
  focus-back-distance sufficiency against its own acceptance-band
  bounds, and a double-shipment for the same batch, before this is ever
  allowed to commit."
  [batch-id jurisdiction sequence]
  (when-not (and batch-id (not= batch-id ""))
    (throw (ex-info "optical-module-batch-shipment: batch_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "optical-module-batch-shipment: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "optical-module-batch-shipment: sequence must be >= 0" {})))
  (let [shipment-number (str (str/upper-case jurisdiction) "-OMS-" (zero-pad sequence 6))
        record {"record_id" shipment-number
                "kind" "optical-module-batch-shipment-draft"
                "batch_id" batch-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "shipment_number" shipment-number
     "certificate" (unsigned-certificate "OpticalModuleBatchShipment" shipment-number shipment-number)}))

(defn register-optical-certificate
  "Validate + construct the OPTICAL MODULE TEST CERTIFICATE
  registration DRAFT -- the camera/optical-module manufacturer's own
  act of issuing a real Optical Module Test Certificate (resolution/MTF
  test report + safety compliance) certifying an optical-module-batch's
  optical-standard conformance before onward shipment to either
  downstream consumer. Pure function -- does not touch any real plant/
  MES control system; it builds the RECORD a camera/optical-module
  manufacturer would keep. `opticsworks.governor` independently
  re-verifies the batch's own end-of-line-defect resolution status, and
  a double-issuance for the same batch, before this is ever allowed to
  commit."
  [batch-id jurisdiction sequence]
  (when-not (and batch-id (not= batch-id ""))
    (throw (ex-info "optical-certificate: batch_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "optical-certificate: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "optical-certificate: sequence must be >= 0" {})))
  (let [certificate-number (str (str/upper-case jurisdiction) "-OMC-" (zero-pad sequence 6))
        record {"record_id" certificate-number
                "kind" "optical-certificate-draft"
                "batch_id" batch-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "certificate_number" certificate-number
     "certificate" (unsigned-certificate "OpticalModuleTestCertificate" certificate-number certificate-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
