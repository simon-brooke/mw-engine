(ns mw-engine.heightmap-test
  (:use clojure.java.io)
  (:require [clojure.test :refer :all]
            [mw-engine.heightmap :refer :all]
            [clojure.math.combinatorics :as combo]))

(deftest apply-heightmap-test
  (testing "Heightmap functionality"
           (let [world (apply-heightmap (as-file "resources/heightmaps/test9x9.png"))
                 altitudes (map #(:altitude %) (flatten world))
                 gradients (map #(:gradient %) (flatten world))]
             (is (= (count world) 9) "World should be 9x9")
             (is (= (count (first world)) 9) "World should be 9x9")
             (is (= (count (remove nil? altitudes)) 81)
                 "All cells should have altitude")
             (is (= (count (remove nil? gradients)) 81)
                 "All cells should have gradient")
             (is (> (apply max altitudes)
                    (apply min altitudes))
                 "There should be a range of altitudes")
             (is (> (apply + gradients) 0)
                 "At least some gradients must be positive, none should be negative")
           )))
           
