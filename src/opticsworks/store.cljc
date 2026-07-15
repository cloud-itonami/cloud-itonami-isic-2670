(ns opticsworks.store
  "SSoT for the camera/optical-module-manufacturer actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite -- the same
  seam every prior `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/opticsworks/store_contract_test.clj), which is the whole point:
  the actor, the Module-Seating Governor and the audit ledger never know
  which SSoT they run on.

  Like `moldworks.store`'s dual molding-run-batch-shipment/material-
  certificate history and `cellworks.store`'s dual cell-batch-shipment/
  safety-certificate history, this actor has TWO actuation events
  (shipping an optical-module-batch onward to a downstream consumer,
  issuing an Optical Module Test Certificate) acting on the SAME entity
  (an optical-module-batch), each with its OWN history collection,
  sequence counter and dedicated double-actuation-guard boolean
  (`:optical-module-batch-shipped?`/`:optical-certified?`, never a
  `:status` value) -- the same discipline every prior sibling
  governor's guards establish, informed by `cloud-itonami-isic-6492`'s
  status-lifecycle bug (ADR-2607071320).

  The ledger stays append-only on every backend: 'which optical-module-
  batch was screened for an unresolved end-of-line defect, which
  optical-module-batch shipment was dispatched onward to a downstream
  consumer, which Optical Module Test Certificate was issued, on what
  product-class basis, approved by whom' is always a query over an
  immutable log -- the audit trail a community trusting a camera/
  optical-module manufacturer needs, and the evidence a plant needs if a
  shipment or certificate decision is later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [opticsworks.registry :as registry]
            [opticsworks.robotics :as robotics]
            [langchain.db :as d]))

(defprotocol Store
  (optical-module-batch [s id])
  (all-optical-module-batches [s])
  (eol-screen-of [s batch-id] "committed end-of-line quality screening verdict for a batch, or nil")
  (optical-standard-verification-of [s batch-id] "committed optical-standard-rules evidence verification, or nil")
  (ledger [s])
  (shipment-history [s] "the append-only optical-module-batch-shipment history (opticsworks.registry drafts)")
  (certificate-history [s] "the append-only Optical Module Test Certificate history (opticsworks.registry drafts)")
  (next-shipment-sequence [s jurisdiction] "next shipment-number sequence for a product-class scheme")
  (next-certificate-sequence [s jurisdiction] "next certificate-number sequence for a product-class scheme")
  (batch-already-shipped? [s batch-id] "has this optical-module-batch already been shipped onward?")
  (batch-already-certified? [s batch-id] "has this optical-module-batch's Optical Module Test Certificate already been issued?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-optical-module-batches [s batches] "replace/seed the optical-module-batch directory (map id->batch)"))

;; ----------------------------- demo data -----------------------------

(defn- with-seating-telemetry
  "Merges REAL lens-seating-verification telemetry onto a demo optical-
  module-batch's base fields -- `opticsworks.robotics/seating-
  telemetry-for` actually runs `simulate-lens-seating`'s `physics-2d`-
  stepped simulation for this batch's own `:seating-press-effective-
  mass-kg` (ADR-2607151600/ADR-2607152000), so even the 'already on
  file' seed data (as if from an earlier real lens-seating verification
  report) is genuinely simulation-derived, never hand-typed doubles."
  [base]
  (merge base (select-keys (robotics/seating-telemetry-for base)
                           [:sim-peak-seating-force-n])))

(defn demo-data
  "A small, self-contained optical-module-batch set covering both
  actuation lifecycles (shipping a batch onward to a downstream
  consumer, issuing an Optical Module Test Certificate) so the actor
  + tests run offline. `:seating-press-effective-mass-kg`
  (ADR-2607151600/ADR-2607152000) is a permanent batch press-run-
  configuration field (like `:focus-back-distance-deviation-actual-um`);
  `:sim-peak-seating-force-n` is the REAL `opticsworks.robotics/
  simulate-lens-seating`-computed telemetry for that field (`with-
  seating-telemetry`), the ground truth `opticsworks.robotics/
  simulation-out-of-tolerance?` independently rechecks against the
  batch's own recorded `:seating-force-min-n`/`:seating-force-max-n`
  acceptance band.

  batch-1 -- smartphone camera-module batch (consumer-grade,
    25g seating-press effective mass -> ~2.0 N, clears its own
    [1,20] N acceptance band with margin) -- the clean, fully-
    processable smartphone batch.
  batch-2 -- smartphone camera-module batch (consumer-grade, same
    ~2.0 N in-band seating force), but recorded against a product class
    (\"MEDDEV-OPTICS\", a medical-imaging-optics scheme)
    `opticsworks.facts` genuinely does NOT cover -- the no-spec-basis
    negative control.
  batch-3 -- automotive ADAS optical/camera sensor-module batch
    (ruggedized-grade, 400g -> ~32.0 N, clears its own [5,60] N band),
    but its own recorded focus-back-distance deviation (4.2 micrometers)
    falls outside its own [-3,3] micrometer acceptance band -- a
    genuine post-assembly optical-alignment defect distinct from the
    seating-force physics check.
  batch-4 -- smartphone camera-module batch (consumer-grade, real,
    covered \"SMARTPHONE-CAMERA\" scheme), seating force and focus-back-
    distance both clean, but an unresolved end-of-line defect (optical-
    clarity/dust-ingress/dead-pixel visual reject) is on file.
  batch-5 -- automotive ADAS optical/camera sensor-module batch
    (ruggedized-grade), DELIBERATELY recorded with a much HEAVIER
    `:seating-press-effective-mass-kg` (1000g) than its own [5,60] N
    band can clear (real simulated seating reading ~80 N, OVER the 60 N
    max -- an over-pressed press-run-configuration inconsistency risking
    a cracked lens element/housing) that the real, re-run simulation
    catches on independent recheck even though `:robotics-sim-
    verified?` was seeded `true` (\"already on file\", i.e. someone/
    something marked it passed without this real check ever having
    run) -- the optical-module-manufacturer analog of moldworks' batch-5
    misconfigured press-run.
  batch-6 -- smartphone camera-module batch (consumer-grade),
    DELIBERATELY recorded with a much LIGHTER `:seating-press-
    effective-mass-kg` (5g) than its own [1,20] N band requires (real
    simulated seating reading ~0.4 N, UNDER the 1 N min -- an
    under-pressed press-run-configuration inconsistency risking a
    loose/misaligned lens and focus drift) that the real, re-run
    simulation ALSO catches on independent recheck even though
    `:robotics-sim-verified?` was seeded `true` -- the opposite-
    direction failure this actor's own two-sided seating-force check
    must also catch (unlike `moldworks.robotics`'s deliberately
    one-sided clamp-tonnage check)."
  []
  {:optical-module-batches
   (into {}
         (map (fn [v] [(:id v) (with-seating-telemetry v)]))
         [{:id "batch-1" :batch-name "Meridian Smartphone Camera-Module Batch OM-4401"
           :product-class :consumer-grade
           :seating-press-effective-mass-kg 0.025
           :seating-force-min-n 1.0 :seating-force-max-n 20.0
           :focus-back-distance-deviation-actual-um 0.5
           :focus-back-distance-deviation-min-um -3.0
           :focus-back-distance-deviation-max-um 3.0
           :optical-module-batch-defect-unresolved? false
           :robotics-sim-verified? false :robotics-sim-record nil
           :optical-module-batch-shipped? false :optical-certified? false
           :jurisdiction "SMARTPHONE-CAMERA" :status :intake}
          {:id "batch-2" :batch-name "Atlas Camera-Module Batch OM-1180"
           :product-class :consumer-grade
           :seating-press-effective-mass-kg 0.025
           :seating-force-min-n 1.0 :seating-force-max-n 20.0
           :focus-back-distance-deviation-actual-um 0.5
           :focus-back-distance-deviation-min-um -3.0
           :focus-back-distance-deviation-max-um 3.0
           :optical-module-batch-defect-unresolved? false
           :robotics-sim-verified? false :robotics-sim-record nil
           :optical-module-batch-shipped? false :optical-certified? false
           :jurisdiction "MEDDEV-OPTICS" :status :intake}
          {:id "batch-3" :batch-name "田中バックアップカメラモジュールバッチ OM-2215"
           :product-class :ruggedized-grade
           :seating-press-effective-mass-kg 0.4
           :seating-force-min-n 5.0 :seating-force-max-n 60.0
           :focus-back-distance-deviation-actual-um 4.2
           :focus-back-distance-deviation-min-um -3.0
           :focus-back-distance-deviation-max-um 3.0
           :optical-module-batch-defect-unresolved? false
           :robotics-sim-verified? false :robotics-sim-record nil
           :optical-module-batch-shipped? false :optical-certified? false
           :jurisdiction "AUTOMOTIVE-ADAS" :status :intake}
          {:id "batch-4" :batch-name "佐藤スマートフォンカメラモジュールバッチ OM-3330"
           :product-class :consumer-grade
           :seating-press-effective-mass-kg 0.025
           :seating-force-min-n 1.0 :seating-force-max-n 20.0
           :focus-back-distance-deviation-actual-um 0.5
           :focus-back-distance-deviation-min-um -3.0
           :focus-back-distance-deviation-max-um 3.0
           :optical-module-batch-defect-unresolved? true
           :robotics-sim-verified? false :robotics-sim-record nil
           :optical-module-batch-shipped? false :optical-certified? false
           :jurisdiction "SMARTPHONE-CAMERA" :status :intake}
          {:id "batch-5" :batch-name "鈴木ADASセンサーモジュールバッチ OM-1118"
           :product-class :ruggedized-grade
           :seating-press-effective-mass-kg 1.0
           :seating-force-min-n 5.0 :seating-force-max-n 60.0
           :focus-back-distance-deviation-actual-um 0.5
           :focus-back-distance-deviation-min-um -3.0
           :focus-back-distance-deviation-max-um 3.0
           :optical-module-batch-defect-unresolved? false
           :robotics-sim-verified? true :robotics-sim-record nil
           :optical-module-batch-shipped? false :optical-certified? false
           :jurisdiction "AUTOMOTIVE-ADAS" :status :intake}
          {:id "batch-6" :batch-name "Nakamura Compact Lens-Module Batch OM-5502"
           :product-class :consumer-grade
           :seating-press-effective-mass-kg 0.005
           :seating-force-min-n 1.0 :seating-force-max-n 20.0
           :focus-back-distance-deviation-actual-um 0.5
           :focus-back-distance-deviation-min-um -3.0
           :focus-back-distance-deviation-max-um 3.0
           :optical-module-batch-defect-unresolved? false
           :robotics-sim-verified? true :robotics-sim-record nil
           :optical-module-batch-shipped? false :optical-certified? false
           :jurisdiction "SMARTPHONE-CAMERA" :status :intake}])})

;; ----------------------------- shared commit logic -----------------------------

(defn- ship-optical-module-batch!
  "Backend-agnostic `:optical-module-batch/mark-shipped` -- looks up the
  batch via the protocol and drafts the optical-module-batch-shipment
  record, and returns {:result .. :batch-patch ..} for the caller to
  persist."
  [s batch-id]
  (let [a (optical-module-batch s batch-id)
        seq-n (next-shipment-sequence s (:jurisdiction a))
        result (registry/register-optical-module-batch-shipment batch-id (:jurisdiction a) seq-n)]
    {:result result
     :batch-patch {:optical-module-batch-shipped? true
                  :shipment-number (get result "shipment_number")}}))

(defn- issue-optical-certificate!
  "Backend-agnostic `:optical-module-batch/mark-certified` -- looks up
  the batch via the protocol and drafts the Optical Module Test
  Certificate record, and returns {:result .. :batch-patch ..} for the
  caller to persist."
  [s batch-id]
  (let [a (optical-module-batch s batch-id)
        seq-n (next-certificate-sequence s (:jurisdiction a))
        result (registry/register-optical-certificate batch-id (:jurisdiction a) seq-n)]
    {:result result
     :batch-patch {:optical-certified? true
                  :certificate-number (get result "certificate_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (optical-module-batch [_ id] (get-in @a [:optical-module-batches id]))
  (all-optical-module-batches [_] (sort-by :id (vals (:optical-module-batches @a))))
  (eol-screen-of [_ id] (get-in @a [:eol-screens id]))
  (optical-standard-verification-of [_ batch-id] (get-in @a [:verifications batch-id]))
  (ledger [_] (:ledger @a))
  (shipment-history [_] (:shipments @a))
  (certificate-history [_] (:certificates @a))
  (next-shipment-sequence [_ jurisdiction] (get-in @a [:shipment-sequences jurisdiction] 0))
  (next-certificate-sequence [_ jurisdiction] (get-in @a [:certificate-sequences jurisdiction] 0))
  (batch-already-shipped? [_ batch-id] (boolean (get-in @a [:optical-module-batches batch-id :optical-module-batch-shipped?])))
  (batch-already-certified? [_ batch-id] (boolean (get-in @a [:optical-module-batches batch-id :optical-certified?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :optical-module-batch/upsert
      (swap! a update-in [:optical-module-batches (:id value)] merge value)

      :optical-standard-verification/set
      (swap! a assoc-in [:verifications (first path)] payload)

      :eol-screen/set
      (swap! a assoc-in [:eol-screens (first path)] payload)

      :optical-module-batch/mark-shipped
      (let [batch-id (first path)
            {:keys [result batch-patch]} (ship-optical-module-batch! s batch-id)
            jurisdiction (:jurisdiction (optical-module-batch s batch-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:shipment-sequences jurisdiction] (fnil inc 0))
                       (update-in [:optical-module-batches batch-id] merge batch-patch)
                       (update :shipments registry/append result))))
        result)

      :optical-module-batch/mark-certified
      (let [batch-id (first path)
            {:keys [result batch-patch]} (issue-optical-certificate! s batch-id)
            jurisdiction (:jurisdiction (optical-module-batch s batch-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:certificate-sequences jurisdiction] (fnil inc 0))
                       (update-in [:optical-module-batches batch-id] merge batch-patch)
                       (update :certificates registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-optical-module-batches [s batches] (when (seq batches) (swap! a assoc :optical-module-batches batches)) s))

(defn seed-db
  "A MemStore seeded with the demo optical-module-batch set. The
  deterministic default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :verifications {} :eol-screens {} :ledger []
                           :shipment-sequences {} :shipments []
                           :certificate-sequences {} :certificates []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (verification/eol-screen payloads, ledger facts,
  shipment/certificate records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:optical-module-batch/id           {:db/unique :db.unique/identity}
   :verification/batch-id             {:db/unique :db.unique/identity}
   :eol-screen/batch-id               {:db/unique :db.unique/identity}
   :ledger/seq                        {:db/unique :db.unique/identity}
   :shipment/seq                      {:db/unique :db.unique/identity}
   :certificate/seq                   {:db/unique :db.unique/identity}
   :shipment-sequence/jurisdiction    {:db/unique :db.unique/identity}
   :certificate-sequence/jurisdiction {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- batch->tx [{:keys [id batch-name product-class
                          seating-press-effective-mass-kg sim-peak-seating-force-n
                          seating-force-min-n seating-force-max-n
                          focus-back-distance-deviation-actual-um
                          focus-back-distance-deviation-min-um
                          focus-back-distance-deviation-max-um
                          optical-module-batch-defect-unresolved? robotics-sim-verified? robotics-sim-record
                          optical-module-batch-shipped? optical-certified?
                          jurisdiction status shipment-number certificate-number]}]
  (cond-> {:optical-module-batch/id id}
    batch-name                                        (assoc :optical-module-batch/batch-name batch-name)
    product-class                                      (assoc :optical-module-batch/product-class product-class)
    seating-press-effective-mass-kg                    (assoc :optical-module-batch/seating-press-effective-mass-kg seating-press-effective-mass-kg)
    (some? sim-peak-seating-force-n)                    (assoc :optical-module-batch/sim-peak-seating-force-n sim-peak-seating-force-n)
    seating-force-min-n                                (assoc :optical-module-batch/seating-force-min-n seating-force-min-n)
    seating-force-max-n                                (assoc :optical-module-batch/seating-force-max-n seating-force-max-n)
    focus-back-distance-deviation-actual-um            (assoc :optical-module-batch/focus-back-distance-deviation-actual-um focus-back-distance-deviation-actual-um)
    focus-back-distance-deviation-min-um               (assoc :optical-module-batch/focus-back-distance-deviation-min-um focus-back-distance-deviation-min-um)
    focus-back-distance-deviation-max-um               (assoc :optical-module-batch/focus-back-distance-deviation-max-um focus-back-distance-deviation-max-um)
    (some? optical-module-batch-defect-unresolved?)    (assoc :optical-module-batch/defect-unresolved? optical-module-batch-defect-unresolved?)
    (some? robotics-sim-verified?)                     (assoc :optical-module-batch/robotics-sim-verified? robotics-sim-verified?)
    (some? robotics-sim-record)                        (assoc :optical-module-batch/robotics-sim-record (enc robotics-sim-record))
    (some? optical-module-batch-shipped?)               (assoc :optical-module-batch/shipped? optical-module-batch-shipped?)
    (some? optical-certified?)                          (assoc :optical-module-batch/certified? optical-certified?)
    jurisdiction                                        (assoc :optical-module-batch/jurisdiction jurisdiction)
    status                                              (assoc :optical-module-batch/status status)
    shipment-number                                     (assoc :optical-module-batch/shipment-number shipment-number)
    certificate-number                                  (assoc :optical-module-batch/certificate-number certificate-number)))

(def ^:private batch-pull
  [:optical-module-batch/id :optical-module-batch/batch-name :optical-module-batch/product-class
   :optical-module-batch/seating-press-effective-mass-kg :optical-module-batch/sim-peak-seating-force-n
   :optical-module-batch/seating-force-min-n :optical-module-batch/seating-force-max-n
   :optical-module-batch/focus-back-distance-deviation-actual-um
   :optical-module-batch/focus-back-distance-deviation-min-um
   :optical-module-batch/focus-back-distance-deviation-max-um
   :optical-module-batch/defect-unresolved? :optical-module-batch/robotics-sim-verified? :optical-module-batch/robotics-sim-record
   :optical-module-batch/shipped? :optical-module-batch/certified?
   :optical-module-batch/jurisdiction :optical-module-batch/status
   :optical-module-batch/shipment-number :optical-module-batch/certificate-number])

(defn- pull->batch [m]
  (when (:optical-module-batch/id m)
    {:id (:optical-module-batch/id m) :batch-name (:optical-module-batch/batch-name m)
     :product-class (:optical-module-batch/product-class m)
     :seating-press-effective-mass-kg (:optical-module-batch/seating-press-effective-mass-kg m)
     :sim-peak-seating-force-n (:optical-module-batch/sim-peak-seating-force-n m)
     :seating-force-min-n (:optical-module-batch/seating-force-min-n m)
     :seating-force-max-n (:optical-module-batch/seating-force-max-n m)
     :focus-back-distance-deviation-actual-um (:optical-module-batch/focus-back-distance-deviation-actual-um m)
     :focus-back-distance-deviation-min-um (:optical-module-batch/focus-back-distance-deviation-min-um m)
     :focus-back-distance-deviation-max-um (:optical-module-batch/focus-back-distance-deviation-max-um m)
     :optical-module-batch-defect-unresolved? (boolean (:optical-module-batch/defect-unresolved? m))
     :robotics-sim-verified? (boolean (:optical-module-batch/robotics-sim-verified? m))
     :robotics-sim-record (dec* (:optical-module-batch/robotics-sim-record m))
     :optical-module-batch-shipped? (boolean (:optical-module-batch/shipped? m))
     :optical-certified? (boolean (:optical-module-batch/certified? m))
     :jurisdiction (:optical-module-batch/jurisdiction m) :status (:optical-module-batch/status m)
     :shipment-number (:optical-module-batch/shipment-number m) :certificate-number (:optical-module-batch/certificate-number m)}))

(defrecord DatomicStore [conn]
  Store
  (optical-module-batch [_ id]
    (pull->batch (d/pull (d/db conn) batch-pull [:optical-module-batch/id id])))
  (all-optical-module-batches [_]
    (->> (d/q '[:find [?id ...] :where [?e :optical-module-batch/id ?id]] (d/db conn))
         (map #(pull->batch (d/pull (d/db conn) batch-pull [:optical-module-batch/id %])))
         (sort-by :id)))
  (eol-screen-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?k :eol-screen/batch-id ?aid] [?k :eol-screen/payload ?p]]
              (d/db conn) id)))
  (optical-standard-verification-of [_ batch-id]
    (dec* (d/q '[:find ?p . :in $ ?aid
                :where [?a :verification/batch-id ?aid] [?a :verification/payload ?p]]
              (d/db conn) batch-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (shipment-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :shipment/seq ?s] [?e :shipment/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (certificate-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :certificate/seq ?s] [?e :certificate/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-shipment-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :shipment-sequence/jurisdiction ?j] [?e :shipment-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-certificate-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :certificate-sequence/jurisdiction ?j] [?e :certificate-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (batch-already-shipped? [s batch-id]
    (boolean (:optical-module-batch-shipped? (optical-module-batch s batch-id))))
  (batch-already-certified? [s batch-id]
    (boolean (:optical-certified? (optical-module-batch s batch-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :optical-module-batch/upsert
      (d/transact! conn [(batch->tx value)])

      :optical-standard-verification/set
      (d/transact! conn [{:verification/batch-id (first path) :verification/payload (enc payload)}])

      :eol-screen/set
      (d/transact! conn [{:eol-screen/batch-id (first path) :eol-screen/payload (enc payload)}])

      :optical-module-batch/mark-shipped
      (let [batch-id (first path)
            {:keys [result batch-patch]} (ship-optical-module-batch! s batch-id)
            jurisdiction (:jurisdiction (optical-module-batch s batch-id))
            next-n (inc (next-shipment-sequence s jurisdiction))]
        (d/transact! conn
                     [(batch->tx (assoc batch-patch :id batch-id))
                      {:shipment-sequence/jurisdiction jurisdiction :shipment-sequence/next next-n}
                      {:shipment/seq (count (shipment-history s)) :shipment/record (enc (get result "record"))}])
        result)

      :optical-module-batch/mark-certified
      (let [batch-id (first path)
            {:keys [result batch-patch]} (issue-optical-certificate! s batch-id)
            jurisdiction (:jurisdiction (optical-module-batch s batch-id))
            next-n (inc (next-certificate-sequence s jurisdiction))]
        (d/transact! conn
                     [(batch->tx (assoc batch-patch :id batch-id))
                      {:certificate-sequence/jurisdiction jurisdiction :certificate-sequence/next next-n}
                      {:certificate/seq (count (certificate-history s)) :certificate/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-optical-module-batches [s batches]
    (when (seq batches) (d/transact! conn (mapv batch->tx (vals batches)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:optical-module-batches ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [optical-module-batches]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-optical-module-batches s optical-module-batches))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo optical-module-batch set -- the
  Datomic-backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
