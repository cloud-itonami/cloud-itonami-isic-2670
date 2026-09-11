(ns opticsworks.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:actuation/ship-optical-module-batch`/`:actuation/issue-
  optical-certificate` must NEVER be a member of any phase's `:auto`
  set."
  (:require [clojure.test :refer [deftest is testing]]
            [opticsworks.phase :as phase]))

(deftest ship-optical-module-batch-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in future entries, auto-commits a real robot batch shipment"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/ship-optical-module-batch))
          (str "phase " n " must not auto-commit :actuation/ship-optical-module-batch")))))

(deftest issue-optical-certificate-never-auto-at-any-phase
  (testing "structural invariant: no phase auto-commits a real Optical Module Test Certificate"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/issue-optical-certificate))
          (str "phase " n " must not auto-commit :actuation/issue-optical-certificate")))))

(deftest end-of-line-quality-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :end-of-line-quality/screen))
          (str "phase " n " must not auto-commit :end-of-line-quality/screen")))))

(deftest robotics-simulate-lens-seating-test-never-auto-at-any-phase
  (testing "the robot lens-seating-verification mission carries no direct capital risk, but is still never auto-eligible, matching every sibling verification op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :robotics/simulate-lens-seating-test))
          (str "phase " n " must not auto-commit :robotics/simulate-lens-seating-test")))))

(deftest robotics-simulate-lens-seating-test-enabled-from-phase-2
  (is (contains? (:writes (get phase/phases 2)) :robotics/simulate-lens-seating-test))
  (is (contains? (:writes (get phase/phases 3)) :robotics/simulate-lens-seating-test))
  (is (not (contains? (:writes (get phase/phases 1)) :robotics/simulate-lens-seating-test))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":optical-module-batch/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:optical-module-batch/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :optical-module-batch/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/ship-optical-module-batch} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/issue-optical-certificate} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :optical-module-batch/intake} :commit)))))
