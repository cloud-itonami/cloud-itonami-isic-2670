(ns opticsworks.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [opticsworks.robotics :as robotics]
            [opticsworks.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Meridian Smartphone Camera-Module Batch OM-4401" (:batch-name (store/optical-module-batch s "batch-1"))))
      (is (= "SMARTPHONE-CAMERA" (:jurisdiction (store/optical-module-batch s "batch-1"))))
      (is (= 0.5 (:focus-back-distance-deviation-actual-um (store/optical-module-batch s "batch-1"))))
      (is (= -3.0 (:focus-back-distance-deviation-min-um (store/optical-module-batch s "batch-1"))))
      (is (= 3.0 (:focus-back-distance-deviation-max-um (store/optical-module-batch s "batch-1"))))
      (is (false? (:optical-module-batch-defect-unresolved? (store/optical-module-batch s "batch-1"))))
      (is (= 4.2 (:focus-back-distance-deviation-actual-um (store/optical-module-batch s "batch-3"))))
      (is (true? (:optical-module-batch-defect-unresolved? (store/optical-module-batch s "batch-4"))))
      (is (false? (:robotics-sim-verified? (store/optical-module-batch s "batch-1"))) "no robotics mission has run yet")
      (is (true? (:robotics-sim-verified? (store/optical-module-batch s "batch-5"))) "seeded as already-on-file")
      (is (true? (:robotics-sim-verified? (store/optical-module-batch s "batch-6"))) "seeded as already-on-file")
      (is (= 1.0 (:seating-press-effective-mass-kg (store/optical-module-batch s "batch-5"))))
      (is (= 0.005 (:seating-press-effective-mass-kg (store/optical-module-batch s "batch-6"))))
      (is (> (:sim-peak-seating-force-n (store/optical-module-batch s "batch-5"))
             (:seating-force-max-n (store/optical-module-batch s "batch-5")))
          "batch-5's real physics-2d-simulated seating force (over-pressed) exceeds its own max acceptance band")
      (is (< (:sim-peak-seating-force-n (store/optical-module-batch s "batch-6"))
             (:seating-force-min-n (store/optical-module-batch s "batch-6")))
          "batch-6's real physics-2d-simulated seating force (under-pressed) falls below its own min acceptance band")
      (is (>= (:sim-peak-seating-force-n (store/optical-module-batch s "batch-1"))
              (:seating-force-min-n (store/optical-module-batch s "batch-1")))
          "batch-1's real physics-2d-simulated seating force clears its own min acceptance band")
      (is (<= (:sim-peak-seating-force-n (store/optical-module-batch s "batch-1"))
              (:seating-force-max-n (store/optical-module-batch s "batch-1")))
          "batch-1's real physics-2d-simulated seating force clears its own max acceptance band")
      (is (= 2.0 (:sim-peak-seating-force-n (store/optical-module-batch s "batch-1"))))
      (is (= 32.0 (:sim-peak-seating-force-n (store/optical-module-batch s "batch-3"))))
      (is (= 80.0 (:sim-peak-seating-force-n (store/optical-module-batch s "batch-5"))))
      (is (= 0.4 (:sim-peak-seating-force-n (store/optical-module-batch s "batch-6"))))
      (is (false? (:optical-module-batch-shipped? (store/optical-module-batch s "batch-1"))))
      (is (false? (:optical-certified? (store/optical-module-batch s "batch-1"))))
      (is (= ["batch-1" "batch-2" "batch-3" "batch-4" "batch-5" "batch-6"]
             (mapv :id (store/all-optical-module-batches s))))
      (is (nil? (store/eol-screen-of s "batch-1")))
      (is (nil? (store/optical-standard-verification-of s "batch-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/shipment-history s)))
      (is (= [] (store/certificate-history s)))
      (is (zero? (store/next-shipment-sequence s "SMARTPHONE-CAMERA")))
      (is (zero? (store/next-certificate-sequence s "SMARTPHONE-CAMERA")))
      (is (false? (store/batch-already-shipped? s "batch-1")))
      (is (false? (store/batch-already-certified? s "batch-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :optical-module-batch/upsert
                                 :value {:id "batch-1" :batch-name "Meridian Smartphone Camera-Module Batch OM-4401"}})
        (is (= "Meridian Smartphone Camera-Module Batch OM-4401" (:batch-name (store/optical-module-batch s "batch-1"))))
        (is (= "SMARTPHONE-CAMERA" (:jurisdiction (store/optical-module-batch s "batch-1"))) "unrelated field preserved"))
      (testing "robotics-sim result commits via :optical-module-batch/upsert and reads back"
        (store/commit-record! s {:effect :optical-module-batch/upsert
                                 :value {:id "batch-1" :robotics-sim-verified? true
                                        :robotics-sim-record {:mission-id "m-1" :passed? true}}})
        (is (true? (:robotics-sim-verified? (store/optical-module-batch s "batch-1"))))
        (is (= {:mission-id "m-1" :passed? true} (:robotics-sim-record (store/optical-module-batch s "batch-1"))))
        (is (= "SMARTPHONE-CAMERA" (:jurisdiction (store/optical-module-batch s "batch-1"))) "unrelated field still preserved"))
      (testing "verification / eol-screen payloads commit and read back"
        (store/commit-record! s {:effect :optical-standard-verification/set :path ["batch-1"]
                                 :payload {:jurisdiction "SMARTPHONE-CAMERA" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "SMARTPHONE-CAMERA" :checklist ["a" "b"]} (store/optical-standard-verification-of s "batch-1")))
        (store/commit-record! s {:effect :eol-screen/set :path ["batch-1"]
                                 :payload {:batch-id "batch-1" :verdict :resolved}})
        (is (= {:batch-id "batch-1" :verdict :resolved} (store/eol-screen-of s "batch-1"))))
      (testing "optical-module-batch shipment drafts a record and advances the sequence"
        (store/commit-record! s {:effect :optical-module-batch/mark-shipped :path ["batch-1"]})
        (is (= "SMARTPHONE-CAMERA-OMS-000000" (get (first (store/shipment-history s)) "record_id")))
        (is (= "optical-module-batch-shipment-draft" (get (first (store/shipment-history s)) "kind")))
        (is (true? (:optical-module-batch-shipped? (store/optical-module-batch s "batch-1"))))
        (is (= 1 (count (store/shipment-history s))))
        (is (= 1 (store/next-shipment-sequence s "SMARTPHONE-CAMERA")))
        (is (true? (store/batch-already-shipped? s "batch-1")))
        (is (false? (store/batch-already-shipped? s "batch-2"))))
      (testing "Optical Module Test Certificate drafts a record and advances the sequence"
        (store/commit-record! s {:effect :optical-module-batch/mark-certified :path ["batch-1"]})
        (is (= "SMARTPHONE-CAMERA-OMC-000000" (get (first (store/certificate-history s)) "record_id")))
        (is (= "optical-certificate-draft" (get (first (store/certificate-history s)) "kind")))
        (is (true? (:optical-certified? (store/optical-module-batch s "batch-1"))))
        (is (= 1 (count (store/certificate-history s))))
        (is (= 1 (store/next-certificate-sequence s "SMARTPHONE-CAMERA")))
        (is (true? (store/batch-already-certified? s "batch-1")))
        (is (false? (store/batch-already-certified? s "batch-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/optical-module-batch s "nope")))
    (is (= [] (store/all-optical-module-batches s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/shipment-history s)))
    (is (= [] (store/certificate-history s)))
    (is (zero? (store/next-shipment-sequence s "SMARTPHONE-CAMERA")))
    (is (zero? (store/next-certificate-sequence s "SMARTPHONE-CAMERA")))
    (store/with-optical-module-batches s {"x" {:id "x" :batch-name "n"
                                     :focus-back-distance-deviation-actual-um 0.5
                                     :focus-back-distance-deviation-min-um -3.0
                                     :focus-back-distance-deviation-max-um 3.0
                                     :optical-module-batch-defect-unresolved? false
                                     :optical-module-batch-shipped? false :optical-certified? false
                                     :jurisdiction "SMARTPHONE-CAMERA" :status :intake}})
    (is (= "n" (:batch-name (store/optical-module-batch s "x"))))))

(deftest seating-force-band-for-matches-the-stored-demo-bands
  (testing "the demo-data seating-force-min/max fields were seeded from robotics/seating-force-band-for -- keep them in sync"
    (is (= (robotics/seating-force-band-for :consumer-grade)
           {:min (:seating-force-min-n (store/optical-module-batch (store/seed-db) "batch-1"))
            :max (:seating-force-max-n (store/optical-module-batch (store/seed-db) "batch-1"))}))
    (is (= (robotics/seating-force-band-for :ruggedized-grade)
           {:min (:seating-force-min-n (store/optical-module-batch (store/seed-db) "batch-3"))
            :max (:seating-force-max-n (store/optical-module-batch (store/seed-db) "batch-3"))}))))
