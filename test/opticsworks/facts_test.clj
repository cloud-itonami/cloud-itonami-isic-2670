(ns opticsworks.facts-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [opticsworks.facts :as facts]))

(deftest smartphone-camera-has-a-spec-basis
  (is (some? (facts/spec-basis "SMARTPHONE-CAMERA")))
  (is (string? (:provenance (facts/spec-basis "SMARTPHONE-CAMERA")))))

(deftest automotive-adas-has-a-spec-basis
  (is (some? (facts/spec-basis "AUTOMOTIVE-ADAS"))))

(deftest unknown-product-class-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "MEDDEV-OPTICS"))))

(deftest coverage-never-reports-a-missing-product-class-as-covered
  (let [report (facts/coverage ["SMARTPHONE-CAMERA" "MEDDEV-OPTICS" "AUTOMOTIVE-ADAS"])]
    (is (= 2 (:covered report)))
    (is (= ["MEDDEV-OPTICS"] (:missing-jurisdictions report)))
    (is (= ["AUTOMOTIVE-ADAS" "SMARTPHONE-CAMERA"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "SMARTPHONE-CAMERA")]
    (is (facts/required-evidence-satisfied? "SMARTPHONE-CAMERA" all))
    (is (not (facts/required-evidence-satisfied? "SMARTPHONE-CAMERA" (rest all))))
    (is (not (facts/required-evidence-satisfied? "MEDDEV-OPTICS" all)) "no spec-basis -> never satisfied")))

(deftest automotive-adas-supplementary-citation-is-disclosed-unconfident-and-not-required
  (let [sb (facts/spec-basis "AUTOMOTIVE-ADAS")]
    (is (some #(str/includes? % "SAE J3088") (:supplementary-citations-unconfident sb))
        "SAE J3088 is disclosed as a supplementary citation")
    (is (not-any? #(str/includes? % "SAE J3088") (:required-evidence sb))
        "SAE J3088 must never gate the hard evidence-checklist requirement")))
