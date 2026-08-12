(ns opticsworks.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`
  (flagship checklist item 2).

  This repo already shipped a `docs/samples/operator-console.html`, but
  it was a HAND-WRITTEN page with no generator behind it: every number
  on it was typed by a human and could drift from the actor without
  anything noticing. This namespace replaces it with a page produced by
  actually RUNNING the real actor stack --
  `opticsworks.operation` (langgraph StateGraph)
    -> `opticsworks.opticsadvisor` (contained advisor, proposal only)
    -> `opticsworks.governor` (Module-Seating Governor, independent censor)
    -> `opticsworks.phase` (rollout gate)
    -> `opticsworks.store` (SSoT + append-only ledger)
  -- against the REAL seeded batches `batch-1`..`batch-6`
  (`opticsworks.store/demo-data`). Nothing on the page is hand-typed
  telemetry: every id, force, deviation, band, rule, record number and
  ledger row below is read back out of the store after the run, or
  re-derived from this repo's own pure predicates
  (`opticsworks.robotics/simulation-out-of-tolerance?`,
  `opticsworks.registry/optical-module-batch-focus-back-distance-out-of-
  range?`) and its own gate tables (`opticsworks.phase/phases`,
  `opticsworks.governor/high-stakes`, `opticsworks.facts/catalog`).

  The scenario deliberately drives every disposition this actor can
  reach, including TEN HARD governor holds covering ALL EIGHT of the
  governor's hard rules -- a HARD hold is one that never reaches a
  human at all, and `-main` REFUSES to write the file if the run
  produced none (a console showing no real hold would be indisputable
  evidence that the actor was not actually driven).

  Determinism: no timestamps, no clock reads, no randomness, and every
  collection is explicitly sorted before rendering, so two runs against
  the same seed are byte-identical.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [jp-go-dds.skin]
            [langgraph.graph :as g]
            [opticsworks.facts :as facts]
            [opticsworks.governor :as governor]
            [opticsworks.phase :as phase]
            [opticsworks.registry :as registry]
            [opticsworks.robotics :as robotics]
            [opticsworks.operation :as op]
            [opticsworks.store :as store]))

;; ----------------------------- the real run -----------------------------

(def ^:private operator
  "The same operator context `opticsworks.sim` uses -- phase 3
  (supervised-auto), so the ONLY op that may auto-commit is
  `:optical-module-batch/intake` (`opticsworks.phase/phases`)."
  {:actor-id "op-1" :actor-role :quality-engineer :phase 3})

(def ^:private approver
  "The human quality engineer who answers the approval interrupts in
  this scenario. Deliberately DIFFERENT from the operator actor-id
  (`op-1`) so the approver-attribution probe below can tell 'the
  approver's id survived into the store' apart from 'the actor-id from
  the request context happens to be there'. An approver id is runtime
  input to the approval API, not seed data -- `opticsworks.store/
  demo-data` has no approvers by design."
  "qe-1")

(defn- exec!
  "One operation = one graph run, on its own thread id."
  [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- decide!
  "Answer an approval interrupt as a human, and record what we actually
  submitted (so the page can compare submitted-vs-retained honestly)."
  [actor tid op subject status]
  (g/run* actor {:approval {:status status :by approver}}
          {:thread-id tid :resume? true})
  {:thread tid :op op :subject subject :status status :by approver})

(defn run-demo!
  "Drives the REAL actor over the REAL seeded batches. Returns
  `{:db <store> :approvals [<human decisions actually submitted> ...]}`.

  batch-1 (SMARTPHONE-CAMERA, clean) walks a FULL lifecycle: intake
  auto-commits at phase 3, then optical-standard-rules verification,
  end-of-line quality screening, the real physics-2d lens-seating
  mission, the batch shipment and the Optical Module Test Certificate
  each escalate to the human quality engineer and are approved.

  Then ten HARD holds, covering every hard rule
  `opticsworks.governor` has:
    - batch-3 shipped before its lens-seating mission ran
      -> :robotics-simulation-missing + :optical-module-batch-focus-
         back-distance-out-of-range (a two-violation hold)
    - batch-3 shipped after a PASSING mission
      -> :optical-module-batch-focus-back-distance-out-of-range alone
         (4.2um outside its own [-3,3]um band)
    - batch-2's product class (MEDDEV-OPTICS) is genuinely absent from
      `opticsworks.facts/catalog` -> :no-spec-basis
    - batch-2's certificate, with no verification on file
      -> :evidence-incomplete
    - batch-4's end-of-line screening finds an unresolved defect
      -> :end-of-line-defect-unresolved
    - batch-4 shipped with neither evidence nor mission
      -> :evidence-incomplete + :robotics-simulation-missing
    - batch-5 independently re-checks OVER-pressed (80.0 N > 60.0 N)
      -> :robotics-simulation-out-of-tolerance
    - batch-6 independently re-checks UNDER-pressed (0.4 N < 1.0 N)
      -> :robotics-simulation-out-of-tolerance
    - batch-1 shipped twice -> :already-shipped
    - batch-1 certified twice -> :already-certified

  and one HUMAN REJECTION (batch-4's verification), which is a
  different thing from a HARD hold and is reported separately."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "b1-intake"
           {:op :optical-module-batch/intake :subject "batch-1"
            :patch {:id "batch-1"
                    :batch-name "Meridian Smartphone Camera-Module Batch OM-4401"}})

    (let [_  (exec! actor "b1-verify" {:op :optical-standard-rules/verify :subject "batch-1"})
          a1 (decide! actor "b1-verify" :optical-standard-rules/verify "batch-1" :approved)

          _  (exec! actor "b1-eol" {:op :end-of-line-quality/screen :subject "batch-1"})
          a2 (decide! actor "b1-eol" :end-of-line-quality/screen "batch-1" :approved)

          _  (exec! actor "b1-sim" {:op :robotics/simulate-lens-seating-test :subject "batch-1"})
          a3 (decide! actor "b1-sim" :robotics/simulate-lens-seating-test "batch-1" :approved)

          _  (exec! actor "b1-ship" {:op :actuation/ship-optical-module-batch :subject "batch-1"})
          a4 (decide! actor "b1-ship" :actuation/ship-optical-module-batch "batch-1" :approved)

          _  (exec! actor "b1-cert" {:op :actuation/issue-optical-certificate :subject "batch-1"})
          a5 (decide! actor "b1-cert" :actuation/issue-optical-certificate "batch-1" :approved)

          ;; batch-3 -- ADAS module whose own focus-back-distance deviation
          ;; is outside its own recorded acceptance band.
          _  (exec! actor "b3-verify" {:op :optical-standard-rules/verify :subject "batch-3"})
          a6 (decide! actor "b3-verify" :optical-standard-rules/verify "batch-3" :approved)
          _  (exec! actor "b3-ship-early" {:op :actuation/ship-optical-module-batch :subject "batch-3"})
          _  (exec! actor "b3-sim" {:op :robotics/simulate-lens-seating-test :subject "batch-3"})
          a7 (decide! actor "b3-sim" :robotics/simulate-lens-seating-test "batch-3" :approved)
          _  (exec! actor "b3-ship" {:op :actuation/ship-optical-module-batch :subject "batch-3"})

          ;; batch-2 -- product class genuinely absent from the catalog.
          _  (exec! actor "b2-verify" {:op :optical-standard-rules/verify :subject "batch-2"})
          _  (exec! actor "b2-cert" {:op :actuation/issue-optical-certificate :subject "batch-2"})

          ;; batch-4 -- unresolved end-of-line defect; its verification is
          ;; escalated and the human REJECTS it.
          _  (exec! actor "b4-verify" {:op :optical-standard-rules/verify :subject "batch-4"})
          r1 (decide! actor "b4-verify" :optical-standard-rules/verify "batch-4" :rejected)
          _  (exec! actor "b4-eol" {:op :end-of-line-quality/screen :subject "batch-4"})
          _  (exec! actor "b4-ship" {:op :actuation/ship-optical-module-batch :subject "batch-4"})

          ;; batch-5 / batch-6 -- seeded `:robotics-sim-verified? true`, but
          ;; the governor re-derives the seating force from the batch's own
          ;; real physics-2d telemetry and catches both directions.
          _  (exec! actor "b5-verify" {:op :optical-standard-rules/verify :subject "batch-5"})
          a8 (decide! actor "b5-verify" :optical-standard-rules/verify "batch-5" :approved)
          _  (exec! actor "b5-ship" {:op :actuation/ship-optical-module-batch :subject "batch-5"})

          _  (exec! actor "b6-verify" {:op :optical-standard-rules/verify :subject "batch-6"})
          a9 (decide! actor "b6-verify" :optical-standard-rules/verify "batch-6" :approved)
          _  (exec! actor "b6-ship" {:op :actuation/ship-optical-module-batch :subject "batch-6"})

          ;; double-actuation guards.
          _  (exec! actor "b1-ship-again" {:op :actuation/ship-optical-module-batch :subject "batch-1"})
          _  (exec! actor "b1-cert-again" {:op :actuation/issue-optical-certificate :subject "batch-1"})]
      {:db db :approvals [a1 a2 a3 a4 a5 a6 a7 r1 a8 a9]})))

;; ----------------------------- derivations -----------------------------

(defn hard-holds
  "The HARD governor holds on `ledger` -- facts the Module-Seating
  Governor wrote itself (`:t :governor-hold`), i.e. rejections that
  NEVER reached a human. A human rejection is `:t :approval-rejected`
  and is deliberately NOT counted here."
  [ledger]
  (vec (filter #(= :governor-hold (:t %)) ledger)))

(defn- human-rejections [ledger]
  (vec (filter #(= :approval-rejected (:t %)) ledger)))

(defn- hold-rule-counts
  "rule -> how many HARD holds cited it, sorted by rule name."
  [ledger]
  (->> (hard-holds ledger)
       (mapcat (comp (partial map :rule) :violations))
       frequencies
       (sort-by (comp str key))))

(defn- last-fact-for [ledger id]
  (last (filter #(= (:subject %) id) ledger)))

(defn- approver-keys
  "Every key on `m` whose name mentions an approver. DERIVED by
  inspecting the actual value, never assumed -- if `commit-record!`
  ever starts (or stops) retaining the approver, this page follows
  without an edit."
  [m]
  (when (map? m)
    (vec (sort-by str
                  (filter #(re-find #"(?i)approv" (str (if (keyword? %) (name %) %)))
                          (keys m))))))

(defn- probe-row
  "One approver-attribution probe: which store surface, which entity,
  and whether the approver id we actually submitted survived into it."
  [surface entity m]
  (let [ks (approver-keys m)]
    {:surface surface :entity entity :present? (boolean (seq ks))
     :keys ks :value (when (seq ks) (get m (first ks)))}))

(defn- approver-probe
  "Walks the surfaces this run actually wrote and reports, per surface,
  whether the human approver's id is retrievable from the SSoT."
  [db]
  (let [ledger (vec (store/ledger db))]
    (vec
     (concat
      [(probe-row "optical-module-batch record" "batch-1" (store/optical-module-batch db "batch-1"))
       (probe-row "optical-standard verification payload" "batch-1"
                  (store/optical-standard-verification-of db "batch-1"))
       (probe-row "end-of-line screen payload" "batch-1" (store/eol-screen-of db "batch-1"))]
      (map #(probe-row "optical-module-batch-shipment draft" (get % "batch_id") %)
           (store/shipment-history db))
      (map #(probe-row "Optical Module Test Certificate draft" (get % "batch_id") %)
           (store/certificate-history db))
      [(probe-row "audit-ledger :committed fact" "batch-1 shipment"
                  (last (filter #(and (= :committed (:t %))
                                      (= :actuation/ship-optical-module-batch (:op %)))
                                ledger)))]))))

;; ----------------------------- html -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- kw-name [v] (if (keyword? v) (name v) (str v)))

(defn- yes-no [b] (if b "はい / yes" "いいえ / no"))

(defn- td [& cells] (str "        <tr>" (apply str (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>" (apply str (map #(str "<th>" % "</th>") headers)) "</tr></thead>\n"
       "      <tbody>\n"
       (str/join "\n" rows) "\n"
       "      </tbody>\n"
       "    </table>\n"))

(defn- section [title lede body]
  (str "  <section class=\"card\">\n"
       "    <h2>" title "</h2>\n"
       "    <p class=\"muted\">" lede "</p>\n"
       body
       "  </section>\n"))

(defn- ok [s] (str "<span class=\"ok\">" s "</span>"))
(defn- warn [s] (str "<span class=\"warn\">" s "</span>"))
(defn- err [s] (str "<span class=\"err\">" s "</span>"))
(defn- crit [s] (str "<span class=\"critical\">" s "</span>"))
(defn- muted [s] (str "<span class=\"muted\">" s "</span>"))

(defn- status-cell [ledger id]
  (let [f (last-fact-for ledger id)]
    (case (:t f)
      nil                (muted "no activity")
      :committed         (ok "committed")
      :approval-rejected (warn "human rejected")
      :governor-hold     (crit (str "HARD hold &middot; "
                                    (esc (str/join ", " (map (comp kw-name :rule)
                                                             (:violations f))))))
      (muted (esc (kw-name (:t f)))))))

(defn- batch-row [ledger {:keys [id batch-name product-class jurisdiction
                                 optical-module-batch-shipped? optical-certified?
                                 shipment-number certificate-number] :as b}]
  (td (str "<code>" (esc id) "</code>")
      (esc batch-name)
      (esc (kw-name product-class))
      (str "<code>" (esc jurisdiction) "</code>")
      (if optical-module-batch-shipped?
        (ok (esc shipment-number))
        (muted "not shipped"))
      (if optical-certified?
        (ok (esc certificate-number))
        (muted "not certified"))
      (status-cell ledger (:id b))))

(defn- recheck-row [{:keys [id sim-peak-seating-force-n seating-force-min-n seating-force-max-n
                            seating-press-effective-mass-kg
                            focus-back-distance-deviation-actual-um
                            focus-back-distance-deviation-min-um
                            focus-back-distance-deviation-max-um
                            robotics-sim-verified? robotics-sim-record
                            optical-module-batch-defect-unresolved?] :as b}]
  (let [seating-bad? (robotics/simulation-out-of-tolerance? b)
        fbd-bad? (registry/optical-module-batch-focus-back-distance-out-of-range? b)]
    (td (str "<code>" (esc id) "</code>")
        (str "<span class=\"num\">" (esc seating-press-effective-mass-kg) "</span> kg")
        (let [s (str "<span class=\"num\">" (esc sim-peak-seating-force-n) "</span> N &isin; ["
                     (esc seating-force-min-n) "," (esc seating-force-max-n) "] N")]
          (if seating-bad? (err s) (ok s)))
        (let [s (str "<span class=\"num\">" (esc focus-back-distance-deviation-actual-um)
                     "</span> &micro;m &isin; [" (esc focus-back-distance-deviation-min-um) ","
                     (esc focus-back-distance-deviation-max-um) "] &micro;m")]
          (if fbd-bad? (err s) (ok s)))
        (if robotics-sim-verified? (ok (yes-no true)) (muted (yes-no false)))
        (if-let [m (:mission-id robotics-sim-record)]
          (str "<code>" (esc m) "</code>")
          (muted "mission never ran"))
        (if optical-module-batch-defect-unresolved?
          (err "unresolved")
          (ok "none on file")))))

(defn- gate-row [op]
  (let [phs (sort (keys phase/phases))
        write-from (first (filter #(contains? (:writes (get phase/phases %)) op) phs))
        auto-from (first (filter #(contains? (:auto (get phase/phases %)) op) phs))
        high? (contains? governor/high-stakes op)]
    (td (str "<code>" (esc op) "</code>")
        (if write-from
          (str "phase " (esc write-from) " (" (esc (:label (get phase/phases write-from))) ")")
          (muted "never"))
        (if auto-from
          (ok (str "phase " (esc auto-from)))
          (crit "never &middot; at any phase"))
        (if high?
          (crit "safety-critical actuation &middot; always a human")
          (muted "not high-stakes")))))

(defn- hold-row [{:keys [op subject violations confidence]}]
  (td (str "<code>" (esc op) "</code>")
      (str "<code>" (esc subject) "</code>")
      (str/join "<br>" (map #(crit (esc (kw-name (:rule %)))) violations))
      (str/join "<br>" (map #(esc (:detail %)) violations))
      (str "<span class=\"num\">" (esc confidence) "</span>")))

(defn- approval-row [{:keys [op subject status by]}]
  (td (str "<code>" (esc op) "</code>")
      (str "<code>" (esc subject) "</code>")
      (if (= :approved status) (ok "approved") (warn "rejected"))
      (str "<code>" (esc by) "</code>")))

(defn- probe-html-row [{:keys [surface entity present? keys value]}]
  (td (esc surface)
      (str "<code>" (esc entity) "</code>")
      (if present? (ok "retained") (err "NOT retained"))
      (if present?
        (str "<code>" (esc (str/join ", " (map kw-name keys))) "</code> = <code>"
             (esc value) "</code>")
        (muted "no approver key on this record"))))

(defn- ledger-row [i {:keys [t op subject disposition basis summary violations]}]
  (td (str "<span class=\"num\">" i "</span>")
      (case t
        :committed (ok "committed")
        :governor-hold (crit "governor-hold")
        :approval-rejected (warn "approval-rejected")
        (esc (kw-name t)))
      (str "<code>" (esc op) "</code>")
      (str "<code>" (esc subject) "</code>")
      (esc (kw-name disposition))
      (cond
        summary (esc summary)
        (seq violations) (esc (str/join ", " (map (comp kw-name :rule) violations)))
        :else (esc (str/join ", " (map kw-name basis))))))

(defn- scheme-row [scheme]
  (let [sb (facts/spec-basis scheme)]
    (td (str "<code>" (esc scheme) "</code>")
        (if sb (ok (esc (:name sb))) (err "no spec-basis in opticsworks.facts"))
        (if sb
          (str/join "<br>" (map esc (:required-evidence sb)))
          (muted "requirements are NEVER invented for an uncovered class"))
        (if sb (esc (:owner-authority sb)) (muted "&mdash;")))))

(defn render
  "Pure: `{:db <store after run-demo!> :approvals [..]}` -> the whole
  HTML document. Every collection is explicitly sorted or already
  ordered (the ledger is append-only), so this is byte-stable."
  [{:keys [db approvals]}]
  (let [ledger (vec (store/ledger db))
        batches (vec (store/all-optical-module-batches db))
        holds (hard-holds ledger)
        rejections (human-rejections ledger)
        rule-counts (hold-rule-counts ledger)
        schemes (vec (sort (distinct (map :jurisdiction batches))))
        cov (facts/coverage schemes)
        probe (approver-probe db)
        any-approver? (boolean (some :present? probe))]
    (str
     "<html lang=\"ja\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
     "<title>cloud-itonami-isic-2670 &middot; optical/camera-module plant &mdash; Operator Console</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Optical instrument &amp; photographic equipment manufacturing (ISIC 2670) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · never dispatches hardware</span>\n"
     "</header>\n"
     "<main>\n"

     (section
      "この頁の出どころ / Provenance"
      (str "Build-time generated by <code>opticsworks.render-html</code> "
           "(<code>clojure -M:dev:render-html</code>) by actually running the actor: "
           "<code>opticsworks.operation</code> (langgraph StateGraph) &rarr; "
           "<code>opticsworks.opticsadvisor</code> &rarr; <code>opticsworks.governor</code> &rarr; "
           "<code>opticsworks.phase</code> &rarr; <code>opticsworks.store</code>, "
           "over the seeded batches <code>batch-1</code>&hellip;<code>batch-6</code> of "
           "<code>opticsworks.store/demo-data</code>. "
           "Nothing here is hand-typed telemetry. The generator REFUSES to write this file "
           "if the run produced no HARD governor hold.")
      (table ["measure" "value"]
             [(td "ledger facts written by this run"
                  (str "<span class=\"num\">" (count ledger) "</span>"))
              (td "HARD governor holds (never reached a human)"
                  (crit (str "<span class=\"num\">" (count holds) "</span>")))
              (td "distinct HARD rules exercised"
                  (str "<span class=\"num\">" (count rule-counts) "</span>"))
              (td "human rejections (escalated, then refused)"
                  (warn (str "<span class=\"num\">" (count rejections) "</span>")))
              (td "human approvals submitted"
                  (str "<span class=\"num\">"
                       (count (filter #(= :approved (:status %)) approvals)) "</span>"))
              (td "optical-module-batch shipments committed"
                  (str "<span class=\"num\">" (count (store/shipment-history db)) "</span>"))
              (td "Optical Module Test Certificates issued"
                  (str "<span class=\"num\">" (count (store/certificate-history db)) "</span>"))
              (td "rollout phase driven"
                  (str "<span class=\"num\">" (:phase operator) "</span> ("
                       (esc (:label (get phase/phases (:phase operator)))) ")"))]))

     (section
      "光学モジュールバッチ / Optical-module batches"
      (str "The SSoT after the run — read back through the "
           "<code>opticsworks.store/Store</code> protocol, not from the run's return value. "
           "Shipment and certificate numbers are the real "
           "<code>opticsworks.registry</code> drafts committed during the run.")
      (table ["Batch" "Name" "Product class" "Scheme" "Shipment" "Certificate" "Last decision"]
             (map (partial batch-row ledger) batches)))

     (section
      "独立再検証 / Independent ground-truth rechecks"
      (str "Recomputed HERE, at render time, by this repo's own pure predicates — "
           "<code>opticsworks.robotics/simulation-out-of-tolerance?</code> (two-sided: both "
           "over- and under-pressed hold) and "
           "<code>opticsworks.registry/optical-module-batch-focus-back-distance-out-of-range?</code>. "
           "The seating force is REAL <code>physics-2d</code> time-stepped simulation output for the "
           "batch's own recorded press-run mass, never a self-reported field: note that "
           "<code>batch-5</code> and <code>batch-6</code> carry "
           "<code>:robotics-sim-verified? true</code> on file and are still caught.")
      (table ["Batch" "Press mass" "Seating force (sim) vs. band" "Focus-back-distance dev. vs. band"
              "Mission recorded?" "Mission id" "End-of-line defect"]
             (map recheck-row batches)))

     (section
      (str "HARD ホールド / HARD governor holds (" (count holds) ")")
      (str "A HARD hold is written by <code>opticsworks.governor</code> itself and "
           "<strong>never reaches a human at all</strong> — there is no approval path past it. "
           "Every row below was produced by the run; the detail text is the governor's own.")
      (table ["Op" "Batch" "Rule" "Governor detail" "Advisor confidence"]
             (map hold-row holds)))

     (section
      "発火した規則 / HARD rules exercised"
      (str "Counted off the ledger — a rule appears here only because a real HARD hold cited it. "
           "These are all of the hard rules <code>opticsworks.governor</code> defines "
           "(<code>spec-basis</code>, <code>evidence-incomplete</code>, the two robotics checks, "
           "focus-back-distance, end-of-line defect, and the two double-actuation guards), so the "
           "scenario currently covers the governor completely.")
      (table ["Rule" "HARD holds citing it"]
             (map (fn [[rule n]]
                    (td (str "<code>" (esc (kw-name rule)) "</code>")
                        (crit (str "<span class=\"num\">" n "</span>"))))
                  rule-counts)))

     (section
      "アクションゲート / Action gate"
      (str "Derived from <code>opticsworks.phase/phases</code> and "
           "<code>opticsworks.governor/high-stakes</code> — not a hand-written description. "
           "Two independent layers agree that shipping a batch and issuing a certificate are "
           "always a human quality engineer's call: they are absent from every phase's "
           "<code>:auto</code> set AND they are high-stakes in the governor.")
      (table ["Op" "Writable from" "Auto-commit" "Governor stake"]
             (map gate-row (sort-by str phase/write-ops))))

     (section
      "人間の判断 / Human decisions in this run"
      (str "The approval interrupts this scenario actually answered "
           "(<code>interrupt-before #{:request-approval}</code>). A rejection is NOT a HARD hold: "
           "the governor cleared the proposal and the human still refused it, which the ledger "
           "records as <code>:approval-rejected</code>.")
      (table ["Op" "Batch" "Human decision" "Submitted approver id"]
             (map approval-row approvals)))

     (section
      "承認者の帰属 / Approver attribution — measured, not assumed"
      (str "Probed at render time by walking the actual store records and looking for ANY key "
           "mentioning an approver. This is derived, so it self-corrects if the actor changes. "
           (if any-approver?
             (str "Result for this repo: the approver id <strong>partially survives</strong> — "
                  "it is retained on the effects whose <code>commit-record!</code> branch persists "
                  "the record's <code>:payload</code>, and dropped on the branches that persist "
                  "<code>:value</code> or re-draft the record from "
                  "<code>opticsworks.registry</code>. Read each row.")
             (str "Result for this repo: the approver id is <strong>NOT retained anywhere</strong> "
                  "the store can be queried for it."))
           " Stated plainly rather than omitted: silence would leave a reader unable to tell "
           "&ldquo;nobody approved&rdquo; from &ldquo;the store did not keep it&rdquo;. "
           "Not fixed here — changing <code>commit-record!</code> would change actor SSoT "
           "semantics and does not belong in a demo commit.")
      (table ["Store surface" "Entity" "Approver id" "Evidence"]
             (map probe-html-row probe)))

     (section
      "実行済みアクチュエーション / Committed actuation records"
      (str "Real <code>opticsworks.registry</code> drafts, appended by the store during the run. "
           "Both are UNSIGNED — signature is the plant's own act, not this actor's.")
      (table ["Record id" "Kind" "Batch" "Scheme" "Immutable"]
             (map (fn [r]
                    (td (str "<code>" (esc (get r "record_id")) "</code>")
                        (esc (get r "kind"))
                        (str "<code>" (esc (get r "batch_id")) "</code>")
                        (str "<code>" (esc (get r "jurisdiction")) "</code>")
                        (ok (esc (get r "immutable")))))
                  (concat (store/shipment-history db) (store/certificate-history db)))))

     (section
      "光学規格の根拠 / Optical-standard spec-basis coverage"
      (str "From <code>opticsworks.facts/coverage</code> over the schemes the seeded batches "
           "actually carry: <span class=\"num\">" (:covered cov) "</span> of "
           "<span class=\"num\">" (:requested cov) "</span> covered. "
           "The uncovered class is reported honestly instead of being fabricated — that is "
           "exactly what the <code>:no-spec-basis</code> HARD hold above defends.")
      (table ["Scheme" "Spec basis" "Required evidence" "Owner authority"]
             (map scheme-row schemes)))

     (section
      (str "監査台帳 / Audit ledger (" (count ledger) " facts)")
      (str "The append-only decision log <code>opticsworks.store/append-ledger!</code> wrote "
           "during this run, in order. This is the whole ledger, not a selection.")
      (table ["#" "Fact" "Op" "Batch" "Disposition" "Summary / basis"]
             (map-indexed ledger-row ledger)))

     "</main>\n"
     "<footer>\n"
     "  <p>cloud-itonami-isic-2670 — camera/optical-module manufacturing actor. "
     "Generated by <code>opticsworks.render-html</code>; styled with "
     "<a href=\"https://github.com/kotoba-lang/jp-go-digital-design-system\">jp-go-dds</a> "
     "(デジタル庁デザインシステム). No timestamps: reruns against the same seed are "
     "byte-identical.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db] :as result} (run-demo!)
        hs (hard-holds (store/ledger db))]
    (when (empty? hs)
      (throw (ex-info "no governor hold fact on the ledger — refusing to write a console that shows no real hold"
                      {:ledger-facts (count (store/ledger db))})))
    (spit out (render result))
    (println "wrote" out
             (str "(" (count (store/ledger db)) " ledger facts, "
                  (count hs) " HARD governor holds, "
                  (count (distinct (mapcat (comp (partial map :rule) :violations) hs)))
                  " distinct hard rules, "
                  (count (store/shipment-history db)) " shipments, "
                  (count (store/certificate-history db)) " certificates)"))))
