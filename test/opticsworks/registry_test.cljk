(ns opticsworks.registry-test
  (:require [clojure.test :refer [deftest is]]
            [opticsworks.registry :as r]))

;; ----------------------------- optical-module-batch-focus-back-distance-out-of-range? -----------------------------

(deftest not-out-of-range-when-within-bounds
  (is (not (r/optical-module-batch-focus-back-distance-out-of-range? {:focus-back-distance-deviation-actual-um 0.5 :focus-back-distance-deviation-min-um -3.0 :focus-back-distance-deviation-max-um 3.0})))
  (is (not (r/optical-module-batch-focus-back-distance-out-of-range? {:focus-back-distance-deviation-actual-um -3.0 :focus-back-distance-deviation-min-um -3.0 :focus-back-distance-deviation-max-um 3.0})))
  (is (not (r/optical-module-batch-focus-back-distance-out-of-range? {:focus-back-distance-deviation-actual-um 3.0 :focus-back-distance-deviation-min-um -3.0 :focus-back-distance-deviation-max-um 3.0}))))

(deftest out-of-range-when-below-minimum-or-above-maximum
  (is (r/optical-module-batch-focus-back-distance-out-of-range? {:focus-back-distance-deviation-actual-um -4.0 :focus-back-distance-deviation-min-um -3.0 :focus-back-distance-deviation-max-um 3.0}))
  (is (r/optical-module-batch-focus-back-distance-out-of-range? {:focus-back-distance-deviation-actual-um 4.2 :focus-back-distance-deviation-min-um -3.0 :focus-back-distance-deviation-max-um 3.0})))

(deftest out-of-range-is-false-on-missing-fields
  (is (not (r/optical-module-batch-focus-back-distance-out-of-range? {})))
  (is (not (r/optical-module-batch-focus-back-distance-out-of-range? {:focus-back-distance-deviation-actual-um 4.2}))))

;; ----------------------------- register-optical-module-batch-shipment -----------------------------

(deftest shipment-is-a-draft-not-a-real-shipment
  (let [result (r/register-optical-module-batch-shipment "batch-1" "SMARTPHONE-CAMERA" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest shipment-assigns-shipment-number
  (let [result (r/register-optical-module-batch-shipment "batch-1" "SMARTPHONE-CAMERA" 7)]
    (is (= (get result "shipment_number") "SMARTPHONE-CAMERA-OMS-000007"))
    (is (= (get-in result ["record" "batch_id"]) "batch-1"))
    (is (= (get-in result ["record" "kind"]) "optical-module-batch-shipment-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest shipment-validation-rules
  (is (thrown? Exception (r/register-optical-module-batch-shipment "" "SMARTPHONE-CAMERA" 0)))
  (is (thrown? Exception (r/register-optical-module-batch-shipment "batch-1" "" 0)))
  (is (thrown? Exception (r/register-optical-module-batch-shipment "batch-1" "SMARTPHONE-CAMERA" -1))))

;; ----------------------------- register-optical-certificate -----------------------------

(deftest certificate-is-a-draft-not-real-certification
  (let [result (r/register-optical-certificate "batch-1" "SMARTPHONE-CAMERA" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest certificate-assigns-certificate-number
  (let [result (r/register-optical-certificate "batch-1" "SMARTPHONE-CAMERA" 3)]
    (is (= (get result "certificate_number") "SMARTPHONE-CAMERA-OMC-000003"))
    (is (= (get-in result ["record" "batch_id"]) "batch-1"))
    (is (= (get-in result ["record" "kind"]) "optical-certificate-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest certificate-validation-rules
  (is (thrown? Exception (r/register-optical-certificate "" "SMARTPHONE-CAMERA" 0)))
  (is (thrown? Exception (r/register-optical-certificate "batch-1" "" 0)))
  (is (thrown? Exception (r/register-optical-certificate "batch-1" "SMARTPHONE-CAMERA" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-optical-module-batch-shipment "batch-1" "SMARTPHONE-CAMERA" 0)
        hist (r/append [] c1)
        c2 (r/register-optical-module-batch-shipment "batch-2" "SMARTPHONE-CAMERA" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "SMARTPHONE-CAMERA-OMS-000000" (get-in hist2 [0 "record_id"])))
    (is (= "SMARTPHONE-CAMERA-OMS-000001" (get-in hist2 [1 "record_id"])))))
