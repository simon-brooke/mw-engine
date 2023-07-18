(ns mw-engine.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [mw-engine.core :refer [apply-rule transform-world]]
            [mw-engine.world :refer [make-world]]))

(deftest apply-rule-test
  (testing "Application of a single rule"
    (let [afn (vary-meta
               (eval
               (fn [cell _world]
                 (cond
                   (= (:state cell) :new)
                   (merge cell {:state :grassland}))))
               merge {:rule-type :production})
          pair (list afn "Test source")]
      (is (nil? (apply-rule nil {:state :water} afn))
          "Rule shouldn't fire when state is wrong")
      (is (nil? (apply-rule nil {:state :water} pair))
          "Rule shouldn't fire when state is wrong")
      (is (= (:state (apply-rule nil {:state :new} afn)) :grassland)
          "Rule should fire when state is correct")
      (is (= (:state (apply-rule nil {:state :new} pair)) :grassland)
          "Rule should fire when state is correct")
      (is (nil? (:rule (apply-rule nil {:state :new} afn)))
          "No rule text if not provided")
      (is (= (:rule (apply-rule nil {:state :new} pair)) "Test source")
          "Rule text cached on cell if provided"))))

(deftest transform-world-tests
  (testing "Application of a single rule"
    (let [afn (vary-meta
               (eval
                (fn [cell _world]
                  (cond
                    (= (:state cell) :new)
                    (merge cell {:state :grassland}))))
               merge {:rule-type :production})
          world (make-world 3 3)
          expected [[{:y 0, :state :grassland, :x 0, :generation 1} {:y 0, :state :grassland, :x 1, :generation 1} {:y 0, :state :grassland, :x 2, :generation 1}] 
                    [{:y 1, :state :grassland, :x 0, :generation 1} {:y 1, :state :grassland, :x 1, :generation 1} {:y 1, :state :grassland, :x 2, :generation 1}]
                    [{:y 2, :state :grassland, :x 0, :generation 1} {:y 2, :state :grassland, :x 1, :generation 1} {:y 2, :state :grassland, :x 2, :generation 1}]]
          actual (transform-world world (list afn))]
      (is (= actual expected)))))