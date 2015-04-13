(ns mw-engine.drainage-test
  (:require [clojure.test :refer :all]
            [mw-engine.world :as world]
            [mw-engine.utils :as utils]
            [mw-engine.drainage :refer :all]))

(deftest is-hollow-test
  (testing "detection of hollows"
           (let [world (utils/set-property
                   (utils/map-world 
                     (world/make-world 3 3)
                     #(merge %2 {:altitude 100}))
                   1 1 :altitude 90)]
             (is (is-hollow world (utils/get-cell world 1 1))
                 "Cell at 1, 1 should be a hollow"))))

(deftest flood-hollow-test
  (testing "Flooding of a single specified cell"
             (let [world (utils/set-property
                   (utils/map-world 
                     (world/make-world 3 3)
                     #(merge %2 {:altitude 100}))
                   1 1 :altitude 90)
                   cell (flood-hollow world (utils/get-cell world 1 1))]
               (is (= (:state cell) :water)
                   "State should be water")
               (is (= (:altitude cell) 100)
                   "Altitude should be 100"))))
               
(deftest flood-hollows-test
  (testing "Flooding of hollows"
           (let [world (utils/set-property
                   (utils/map-world 
                     (world/make-world 3 3)
                     #(merge %2 {:altitude 100}))
                   1 1 :altitude 90)
                 w2 (flood-hollows world)]
             (is (= (:state (utils/get-cell world 1 1)) :new)
                 "State of cell in original world should still be :new")
             (is (= (:state (utils/get-cell w2 1 1)) :water)
                 "State of cell in processed world should still be :water")
             (is (= (:altitude (utils/get-cell w2 1 1)) 100)
                 "Altitude of cell in processed world should still be 100"))))
             
                                  

  
  