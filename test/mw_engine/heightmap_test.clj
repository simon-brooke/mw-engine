(ns mw-engine.heightmap-test
  (:require [clojure.java.io :refer [as-file]]
            [clojure.test :refer [deftest is testing]]
            [mw-engine.heightmap :refer [apply-heightmap apply-valuemap]]
            [mw-engine.world :refer [make-world]]))

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
                 "At least some gradients must be positive, none should be negative"))
           ;; alternate means of making the world, same tests.
           (let [world (apply-heightmap (make-world 9 9) (as-file "resources/heightmaps/test9x9.png"))
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
    (let [image (as-file "resources/heightmaps/test9x9.png")
          world (apply-valuemap (apply-heightmap image) image :arbitrary)
          altitudes (map #(:altitude %) (flatten world))
          arbitraries (map #(:arbitrary %) (flatten world))]
      (is (= altitudes arbitraries) "Altitudes and arbitraries are derived from same map so should be identical.")
      )))
