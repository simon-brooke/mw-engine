(ns mw-engine.flow-test
  (:require [clojure.test :refer [deftest is testing]]
            [mw-engine.flow :refer [coordinate? execute execute-flows flow?
                                    location?]]
            [mw-engine.utils :refer [get-cell merge-cell]]
            [mw-engine.world :refer [make-world]]))

(deftest coordinate-tests
  (testing "coordinates"
    (let [world (make-world 3 3)]
      (is (not (coordinate? -1 world)) "Not a coordinate: negative")
      (is (not (coordinate? 4 world)) "Not a coordinate: out of bounds")
      (is (not (coordinate? 3 world)) "Not a coordinate: boundary")
      (is (not (coordinate? :three world)) "Not a coordinate: keyword")
      (is (not (coordinate? 3.14 world)) "Not a coordinate: floating point")
      (is (coordinate? 0 world) "should be a coordinate: zero")
      (is (coordinate? 1 world) "should be a coordinate: middle"))))

(deftest location-tests
  (testing "locations"
    (let [world (make-world 3 3)
          in1 {:x 0 :y 0}
          in2 {:x 1 :y 2}
          out1 {:p 0 :q 0}
          out2 {:x -1 :y 2}]
      (is (location? in1 world) "should be a location: top left")
      (is (location? in2 world) "should be a location: middle bottom")
      (is (not (location? out1 world)) "should not be a location: wrong keys")
      (is (not (location? out2 world)) "should not be a location: negative coordinate"))))

(deftest flow-tests
  (testing "flows"
    (let [world (make-world 3 3)
          world' (merge-cell world {:x 0, :y 0, :state :new :q 5.3})
          valid {:source {:x 0 :y 0}
                 :destination {:x 1 :y 1}
                 :property :q
                 :quantity 2.4}]
      (is (flow? valid world))
      (let [transferred (execute valid world')
            source-q (:q (get-cell transferred 0 0))
            dest-q (:q (get-cell transferred 1 1))]
        (is (= source-q 2.9))
        (is (= dest-q 2.4)))
      (let [valid2 {:source {:x 1 :y 1}
                    :destination {:x 0  :y 1}
                    :property :q
                    :quantity 1}
            transferred (execute-flows (list valid valid2) world')
            source-q (:q (get-cell transferred 0 0))
            inter-q (:q (get-cell transferred 1 1))
            dest-q (:q (get-cell transferred 0 1))]
        (is (= source-q 2.9))
        (is (= inter-q 1.4))
        (is (= dest-q 1))))))