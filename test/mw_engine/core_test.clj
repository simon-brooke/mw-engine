(ns mw-engine.core-test
  (:require [clojure.test :refer :all]
            [mw-engine.core :refer :all]))

(deftest apply-rule-test
  (testing "Application of a single rule"
           (let [afn (eval 
                       (fn [cell world]
                         (cond 
                           (= (:state cell) :new) 
                           (merge cell {:state :grassland}))))
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