(ns opticsworks.governor
  "Module-Seating Governor -- the independent compliance layer that
  earns the Optics Advisor the right to commit. The LLM has no notion
  of optical-standard evidence law, whether an optical-module-batch's
  own measured focus-back-distance deviation actually stays within its
  own recorded acceptance-band bounds, whether an end-of-line-detected
  defect against the batch has actually stayed unresolved, or when an
  act stops being a draft and becomes a real-world robot batch
  shipment or Optical Module Test Certificate issuance, so this MUST be
  a separate system able to *reject* a proposal and fall back to HOLD
  -- the camera/optical-module-manufacturer analog of
  `cloud-itonami-isic-6512`'s CasualtyGovernor.

  Seven checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them (you don't get to approve your way past a
  fabricated optical-standard basis, incomplete evidence, a robot
  lens-seating verification simulation that never ran or that
  independently re-checks out-of-tolerance, an out-of-band focus-back-
  distance deviation, an unresolved end-of-line defect, or a double
  shipment/certificate-issuance). The confidence/actuation gate is SOFT:
  it asks a human to look (low confidence / actuation), and the human
  may approve -- but see `opticsworks.phase`: for `:stake :actuation/
  ship-optical-module-batch`/`:actuation/issue-optical-certificate` (a
  real safety-critical act) NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the optical-standard-rules
                                       evidence proposal cite an
                                       OFFICIAL source
                                       (`opticsworks.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:actuation/ship-optical-
                                       module-batch`/`:actuation/issue-
                                       optical-certificate`, has the
                                       batch actually been verified
                                       with a full optical-standard
                                       evidence checklist (ISO/IEC test
                                       reports etc.) on file?
    3. Robot simulation missing or
       independently out-of-
       tolerance                     -- for `:actuation/ship-optical-
                                       module-batch`, has the robot
                                       lens-seating verification mission
                                       (`opticsworks.robotics`) actually
                                       run and been recorded on the
                                       batch (`:robotics-sim-
                                       verified?`)? AND INDEPENDENTLY
                                       recompute whether the batch's
                                       own recorded REAL `physics-2d`-
                                       simulated seating-force telemetry
                                       (`:sim-peak-seating-force-n`,
                                       from ADR-2607151600/
                                       ADR-2607152000's real
                                       time-stepped simulation) falls
                                       OUTSIDE the batch's own recorded
                                       [:seating-force-min-n :seating-
                                       force-max-n] acceptance band
                                       (`opticsworks.robotics/
                                       simulation-out-of-tolerance?`),
                                       ignoring whatever :passed?
                                       verdict the mission run itself
                                       stored -- the same 'ground
                                       truth, not self-report'
                                       discipline check 4 below uses
                                       for focus-back-distance
                                       deviation. TWO-SIDED (unlike
                                       `moldworks.governor`'s one-sided
                                       clamp-tonnage check): both an
                                       over- and an under-pressed
                                       seating-force reading HARD-hold.
    4. Optical-module-batch focus-
       back-distance out of range    -- for `:actuation/ship-optical-
                                       module-batch`, INDEPENDENTLY
                                       recompute whether the batch's
                                       own measured focus-back-distance
                                       deviation falls outside its own
                                       recorded acceptance-band bounds
                                       (`opticsworks.registry/optical-
                                       module-batch-focus-back-distance-
                                       out-of-range?`) -- needs no
                                       proposal inspection or
                                       stored-verdict lookup at all. A
                                       further instance of this fleet's
                                       two-sided range check family
                                       (see `opticsworks.registry`'s ns
                                       docstring for the lineage).
    5. End-of-line defect
       unresolved                    -- reported by THIS proposal
                                       itself (an `:end-of-line-
                                       quality/screen` that just found
                                       an unresolved defect), or
                                       already on file for the batch
                                       (`:end-of-line-quality/screen`/
                                       `:actuation/issue-optical-
                                       certificate`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op), the SAME
                                       discipline `casualty.governor/
                                       sanctions-violations`/
                                       `automotive.governor`/
                                       `cellworks.governor`/
                                       `glassworks.governor`/
                                       `moldworks.governor` (prior
                                       siblings) established --
                                       exercised in tests/demo via
                                       `:end-of-line-quality/screen`
                                       DIRECTLY, not via an actuation
                                       op against an unscreened batch
                                       -- see this actor's own test
                                       suite.
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/ship-
                                       optical-module-batch`/`:actuation/
                                       issue-optical-certificate` (REAL
                                       safety-critical acts) -> escalate.

  Two more guards, double-shipment/double-certificate-issuance
  prevention, are enforced but NOT listed as numbered HARD checks
  above because they need no upstream comparison at all --
  `already-shipped-violations`/`already-certified-violations` refuse
  to ship an optical-module-batch action/issue an Optical Module Test
  Certificate for the SAME batch twice, off dedicated `:optical-module-
  batch-shipped?`/`:optical-certified?` facts (never a `:status`
  value) -- the SAME 'check a dedicated boolean, not status'
  discipline every prior sibling governor's guards establish, informed
  by `cloud-itonami-isic-6492`'s status-lifecycle bug
  (ADR-2607071320)."
  (:require [opticsworks.facts :as facts]
            [opticsworks.registry :as registry]
            [opticsworks.robotics :as robotics]
            [opticsworks.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Shipping a real optical-module-batch onward to a downstream consumer
  and issuing a real Optical Module Test Certificate are the two
  real-world actuation events this actor performs -- a two-member set,
  matching every prior dual-actuation sibling's shape."
  #{:actuation/ship-optical-module-batch :actuation/issue-optical-certificate})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "An `:optical-standard-rules/verify` (or actuation) proposal with no
  spec-basis citation is a HARD violation -- never invent a product
  class's optical-standard requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:optical-standard-rules/verify :actuation/ship-optical-module-batch :actuation/issue-optical-certificate} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は光学規格要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/ship-optical-module-batch`/`:actuation/issue-optical-
  certificate`, the product class's required optical-standard evidence
  (ISO/IEC test reports etc.) must actually be satisfied -- do not
  trust the advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/ship-optical-module-batch :actuation/issue-optical-certificate} op)
    (let [a (store/optical-module-batch st subject)
          verification (store/optical-standard-verification-of st subject)]
      (when-not (and verification
                     (facts/required-evidence-satisfied?
                      (:jurisdiction a) (:checklist verification)))
        [{:rule :evidence-incomplete
          :detail "製品区分の必要光学規格書類(ISO/IEC試験報告書等)が充足していない状態での提案"}]))))

(defn- robotics-simulation-violations
  "For `:actuation/ship-optical-module-batch`: HARD hold if the robot
  lens-seating verification mission (`opticsworks.robotics`) never ran
  and was recorded on the batch (`:robotics-sim-verified?`), OR if it
  did but an INDEPENDENT recompute of the batch's own REAL `physics-
  2d`-simulated seating-force telemetry (`:sim-peak-seating-force-n`,
  ADR-2607151600/ADR-2607152000 -- `opticsworks.robotics/simulation-
  out-of-tolerance?`) falls outside the batch's own acceptance band
  right now -- never trusts the mission's own stored :passed? verdict
  alone, the same discipline `optical-module-batch-focus-back-distance-
  out-of-range-violations` below uses for focus-back-distance."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-optical-module-batch)
    (let [a (store/optical-module-batch st subject)]
      (cond
        (not (:robotics-sim-verified? a))
        [{:rule :robotics-simulation-missing
          :detail (str subject " のレンズ着座検証ロボットミッションが未実行・未合格")}]

        (robotics/simulation-out-of-tolerance? a)
        [{:rule :robotics-simulation-out-of-tolerance
          :detail (str subject " の実測着座力(" (:sim-peak-seating-force-n a)
                       "N)が独立再検証で許容範囲[" (:seating-force-min-n a) ","
                       (:seating-force-max-n a) "]Nを逸脱している(圧入力異常)")}]))))

(defn- optical-module-batch-focus-back-distance-out-of-range-violations
  "For `:actuation/ship-optical-module-batch`, INDEPENDENTLY recompute
  whether the batch's own focus-back-distance deviation falls outside
  its own recorded acceptance-band bounds via `opticsworks.registry/
  optical-module-batch-focus-back-distance-out-of-range?` -- needs no
  proposal inspection or stored-verdict lookup at all, since its
  inputs are permanent ground-truth fields already on the batch."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-optical-module-batch)
    (let [a (store/optical-module-batch st subject)]
      (when (registry/optical-module-batch-focus-back-distance-out-of-range? a)
        [{:rule :optical-module-batch-focus-back-distance-out-of-range
          :detail (str subject " の実測フォーカスバックディスタンス偏差(" (:focus-back-distance-deviation-actual-um a)
                      "um)が許容範囲[" (:focus-back-distance-deviation-min-um a) ","
                      (:focus-back-distance-deviation-max-um a) "]umを逸脱")}]))))

(defn- end-of-line-defect-unresolved-violations
  "An unresolved end-of-line-detected defect (optical-clarity/dust-
  ingress/dead-pixel) -- reported by THIS proposal (e.g. an `:end-of-
  line-quality/screen` that itself just found one), or already on file
  in the store for the batch (`:end-of-line-quality/screen`/
  `:actuation/issue-optical-certificate`) -- is a HARD, un-overridable
  hold. Evaluated UNCONDITIONALLY (not scoped to a specific op) so the
  screening op itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :unresolved (get-in proposal [:value :verdict]))
        batch-id (when (contains? #{:end-of-line-quality/screen :actuation/issue-optical-certificate} op) subject)
        hit-on-file? (and batch-id (= :unresolved (:verdict (store/eol-screen-of st batch-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :end-of-line-defect-unresolved
        :detail "未解決の完成検査欠陥(光学的鮮明度/塵埃侵入/ドット欠陥)がある状態での光学証明書発行提案は進められない"}])))

(defn- already-shipped-violations
  "For `:actuation/ship-optical-module-batch`, refuses to ship a batch
  action for the SAME batch twice, off a dedicated `:optical-module-
  batch-shipped?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/ship-optical-module-batch)
    (when (store/batch-already-shipped? st subject)
      [{:rule :already-shipped
        :detail (str subject " は既に出荷実行済み")}])))

(defn- already-certified-violations
  "For `:actuation/issue-optical-certificate`, refuses to issue an
  Optical Module Test Certificate for the SAME batch twice, off a
  dedicated `:optical-certified?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/issue-optical-certificate)
    (when (store/batch-already-certified? st subject)
      [{:rule :already-certified
        :detail (str subject " は既に光学証明書発行済み")}])))

(defn check
  "Censors an Optics Advisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (robotics-simulation-violations request st)
                           (optical-module-batch-focus-back-distance-out-of-range-violations request st)
                           (end-of-line-defect-unresolved-violations request proposal st)
                           (already-shipped-violations request st)
                           (already-certified-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
