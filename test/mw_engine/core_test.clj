(ns mw-engine.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [mw-engine.core :refer [apply-rule transform-world]]
            [mw-engine.utils :refer [map-world]]
            [mw-engine.world :refer [make-world]]))

(deftest apply-rule-test
  (testing "Application of a single rule"
    (let [afn (vary-meta
               (eval
               (fn [cell _world]
                 (cond
                   (= (:state cell) :new)
                   (merge cell {:state :grassland}))))
               merge {:rule-type :production
                      :rule "Test source"})]
      (is (nil? (apply-rule nil {:state :water} afn))
          "Rule shouldn't fire when state is wrong")
      (is (= (:state (apply-rule nil {:state :new} afn)) :grassland)
          "Rule should fire when state is correct") 
      (is (seq? (:history (apply-rule nil {:state :new} afn)))
          "Event cached on history of cell"))))

(deftest transform-world-tests
  (testing "Application of a single rule"
    (let [afn (vary-meta
               (eval
                (fn [cell _world]
                  (cond
                    (= (:state cell) :new)
                    (merge cell {:state :grassland}))))
               merge {:rule-type :production
                      :rule "Test source"})
          world (make-world 3 3)
          expected [[{:y 0, :state :grassland, :x 0} {:y 0, :state :grassland, :x 1} {:y 0, :state :grassland, :x 2}] 
                     [{:y 1, :state :grassland, :x 0} {:y 1, :state :grassland, :x 1} {:y 1, :state :grassland, :x 2}] 
                     [{:y 2, :state :grassland, :x 0} {:y 2, :state :grassland, :x 1} {:y 2, :state :grassland, :x 2}]]
          actual (map-world (transform-world world (list afn)) (fn [_ c] (select-keys c [:x :y :state])))]
      (is (= actual expected)))))