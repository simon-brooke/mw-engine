(ns microworld.engine.heightmap-test
  (:use clojure.java.io)
  (:require [clojure.test :refer :all]
            [microworld.engine.heightmap :refer :all]
            [microworld.engine.world :as world :only [make-world]]
            [clojure.math.combinatorics :as combo]))

(deftest apply-heightmap-test
  (testing "Heightmap functionality"
           (let [world (apply-heightmap "heightmaps/test9x9.png")
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
                 "At least some gradients must be positive, none should be negative"))
           ;; alternate means of making the world, same tests.
           (let [world (apply-heightmap (world/make-world 9 9) "heightmaps/test9x9.png")
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



(deftest apply-valuemap-test
  (testing "Valuemap functionality"
    (let [image "heightmaps/test9x9.png"
          world (apply-valuemap (apply-heightmap image) image :arbitrary)
          altitudes (map #(:altitude %) (flatten world))
          arbitraries (map #(:arbitrary %) (flatten world))]
      (is (= altitudes arbitraries) "Altitudes and arbitraries are derived from same map so should be identical.")
      )))
