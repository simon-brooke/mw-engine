(ns mw-engine.world-test
  (:require [clojure.test :refer [deftest is testing]]
            [mw-engine.world :refer [make-world]]
            [clojure.math.combinatorics :as combo]))

(deftest genesis-test
  (testing "World creation."
           (is 
             (empty?
               (remove 
                 true? 
                 (flatten
                   (map 
                     (fn [size] 
                       (let [world (make-world size size)]
                         (is (= (count world) size)
                             "World should be NxN matrix")
                         (is (empty? (remove #(= % size) (map count world)))
                             "World should be NxN matrix")
                         (map #(let [[x y] %
                                     cell (nth (nth world y) x)]
                                 (is (= (:x cell) x) "Checking x coordinate")
                                 (is (= (:y cell) y) "Checking y coordinate")
                                 (is (= (:state cell) :new) "Checking state is new"))
                              (combo/cartesian-product (range size) (range size)))
                         ))
                     (range 1 10)))))
             "Comprehensive new world test")))