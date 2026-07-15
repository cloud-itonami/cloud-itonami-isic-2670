(ns opticsworks.opticsadvisor
  "Optics Advisor client -- the *contained intelligence node* for the
  camera/optical-module-manufacturer actor.

  It normalizes optical-module-batch intake, drafts a per-product-class
  optical-standard evidence checklist, screens batches for an
  unresolved end-of-line-detected defect, drafts the batch-shipment
  action, and drafts the Optical Module Test Certificate issuance
  action. CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real robot dispatch/certificate issuance.
  Every output is censored downstream by `opticsworks.governor` before
  anything touches the SSoT, and `:actuation/ship-optical-module-batch`/
  `:actuation/issue-optical-certificate` proposals NEVER auto-commit
  at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/ship-optical-module-batch | :actuation/issue-optical-certificate | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [opticsworks.facts :as facts]
            [opticsworks.registry :as registry]
            [opticsworks.robotics :as robotics]
            [opticsworks.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the batch, the focus-back-distance figures or the
  product-class scheme. High confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "光学モジュールバッチ記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :optical-module-batch/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-requirements
  "Per-product-class optical-standard evidence checklist draft.
  `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a product class with NO official
  spec-basis in `opticsworks.facts` -- the Module-Seating Governor must
  reject this (never invent a product class's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [a (store/optical-module-batch db subject)
        scheme (if no-spec? "MEDDEV-OPTICS" (:jurisdiction a))
        sb (facts/spec-basis scheme)]
    (if (nil? sb)
      {:summary    (str scheme " の公式spec-basisが見つかりません")
       :rationale  "opticsworks.facts に未登録の製品区分。要件を推測で作らない。"
       :cites      []
       :effect     :optical-standard-verification/set
       :value      {:jurisdiction scheme :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str scheme " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :optical-standard-verification/set
       :value      {:jurisdiction scheme
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-eol-defect
  "End-of-line-defect (optical-clarity/dust-ingress/dead-pixel)
  screening draft. `:optical-module-batch-defect-unresolved?` on the
  batch record injects the failure mode: the Module-Seating Governor
  must HOLD, un-overridably, on any unresolved defect."
  [db {:keys [subject]}]
  (let [a (store/optical-module-batch db subject)]
    (cond
      (nil? a)
      {:summary "対象光学モジュールバッチ記録が見つかりません" :rationale "no batch record"
       :cites [] :effect :eol-screen/set :value {:batch-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:optical-module-batch-defect-unresolved? a))
      {:summary    (str (:batch-name a) ": 未解決の完成検査欠陥(光学的鮮明度/塵埃侵入/ドット欠陥)を検出")
       :rationale  "完成検査スクリーニングが未解決の欠陥を検出。人手確認とホールドが必須。"
       :cites      [:eol-check]
       :effect     :eol-screen/set
       :value      {:batch-id subject :verdict :unresolved}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:batch-name a) ": 未解決の完成検査欠陥なし")
       :rationale  "完成検査欠陥スクリーニング完了。"
       :cites      [:eol-check]
       :effect     :eol-screen/set
       :value      {:batch-id subject :verdict :resolved}
       :stake      nil
       :confidence 0.9})))

(defn- simulate-lens-seating-test
  "Runs the robot lens-seating-verification mission
  (`opticsworks.robotics`) and drafts its result as a proposal. High
  confidence -- the mission itself is a REAL time-stepped `physics-2d`
  rigid-body simulation derived from the batch's own recorded press-
  run configuration, not an LLM guess; the Module-Seating Governor
  still independently re-derives :passed? from those same fields before
  any `:actuation/ship-optical-module-batch` proposal may commit -- see
  `opticsworks.governor`'s `robotics-simulation-violations`."
  [db {:keys [subject]}]
  (let [a (store/optical-module-batch db subject)]
    (if (nil? a)
      {:summary "対象光学モジュールバッチ記録が見つかりません" :rationale "no batch record"
       :cites [] :effect :optical-module-batch/upsert :value {:id subject :robotics-sim-verified? false}
       :stake nil :confidence 0.0}
      (let [{:keys [mission actions passed? sim-peak-seating-force-n]}
            (robotics/simulate-lens-seating-test subject a)]
        {:summary    (str subject ": レンズ着座検証ロボットミッション " (if passed? "合格" "不合格")
                          " (実測 " sim-peak-seating-force-n " N)")
         :rationale  (str "mission=" (:mission/id mission) " actions=" (count actions)
                          " sim-peak-seating-force-n=" sim-peak-seating-force-n)
         :cites      [(:mission/id mission)]
         :effect     :optical-module-batch/upsert
         :value      {:id subject
                      :robotics-sim-verified? passed?
                      :robotics-sim-record {:mission-id (:mission/id mission)
                                            :actions (mapv #(dissoc % :action) actions)
                                            :passed? passed?}}
         :stake      nil
         :confidence 0.95}))))

(defn- propose-optical-module-batch-shipment
  "Draft the actual OPTICAL-MODULE-BATCH-SHIPMENT action -- dispatching
  a real robot handling/shipment action on a safety-critical batch.
  ALWAYS `:stake :actuation/ship-optical-module-batch` -- this is a
  REAL-WORLD safety-critical act, never a draft the actor may
  auto-run. See README `Actuation`: no phase ever adds this op to a
  phase's `:auto` set (`opticsworks.phase`); the governor also always
  escalates on `:actuation/ship-optical-module-batch`. Two independent
  layers agree, deliberately."
  [db {:keys [subject]}]
  (let [a (store/optical-module-batch db subject)]
    {:summary    (str subject " 向け光学モジュールバッチ出荷提案"
                      (when a (str " (batch=" (:batch-name a) ")")))
     :rationale  (if a
                   (str "focus-back-distance-deviation-actual-um=" (:focus-back-distance-deviation-actual-um a)
                        " spec=[" (:focus-back-distance-deviation-min-um a) "," (:focus-back-distance-deviation-max-um a) "]")
                   "光学モジュールバッチ記録が見つかりません")
     :cites      (if a [subject] [])
     :effect     :optical-module-batch/mark-shipped
     :value      {:batch-id subject}
     :stake      :actuation/ship-optical-module-batch
     :confidence (if (and a (not (registry/optical-module-batch-focus-back-distance-out-of-range? a))) 0.9 0.3)}))

(defn- propose-optical-certificate
  "Draft the actual OPTICAL MODULE TEST CERTIFICATE action -- issuing
  a real Optical Module Test Certificate certifying a batch's optical-
  standard conformance. ALWAYS `:stake :actuation/issue-optical-
  certificate` -- this is a REAL-WORLD safety-critical act, never a
  draft the actor may auto-run. See README `Actuation`: no phase ever
  adds this op to a phase's `:auto` set (`opticsworks.phase`); the
  governor also always escalates on `:actuation/issue-optical-
  certificate`. Two independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [a (store/optical-module-batch db subject)]
    {:summary    (str subject " 向け光学証明書発行提案"
                      (when a (str " (batch=" (:batch-name a) ")")))
     :rationale  (if a
                   "optical-standard-evidence-checklist referenced"
                   "光学モジュールバッチ記録が見つかりません")
     :cites      (if a [subject] [])
     :effect     :optical-module-batch/mark-certified
     :value      {:batch-id subject}
     :stake      :actuation/issue-optical-certificate
     :confidence (if a 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :optical-module-batch/intake                 (normalize-intake db request)
    :optical-standard-rules/verify               (verify-requirements db request)
    :end-of-line-quality/screen                  (screen-eol-defect db request)
    :robotics/simulate-lens-seating-test         (simulate-lens-seating-test db request)
    :actuation/ship-optical-module-batch         (propose-optical-module-batch-shipment db request)
    :actuation/issue-optical-certificate         (propose-optical-certificate db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは光学モジュール製造プラントの出荷実行・光学証明書発行エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:optical-module-batch/upsert|:optical-standard-verification/set|:eol-screen/set|"
       ":optical-module-batch/mark-shipped|:optical-module-batch/mark-certified) "
       "(:robotics/simulate-lens-seating-test も :optical-module-batch/upsert で "
       ":robotics-sim-verified? を提案する) "
       ":stake(:actuation/ship-optical-module-batch か :actuation/issue-optical-certificate か nil) :confidence(0..1)。\n"
       "重要: 登録されていない製品区分の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [subject]}]
  {:batch (store/optical-module-batch st subject)})

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Module-Seating Governor
  escalates/holds -- an LLM hiccup can never auto-ship an optical-
  module-batch or auto-issue an Optical Module Test Certificate."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :opticsadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
