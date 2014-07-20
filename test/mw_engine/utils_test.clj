(ns mw-engine.utils-test
  (:use [mw-engine.world :as world])
  (:require [clojure.test :refer :all]
            [mw-engine.utils :refer :all]))

(deftest get-neighbours-test
  (testing "Gross functionality of get-neighbours: checks the right number of 
            neighbours returned, doesn't actually check they're the right ones."
           (let [world (make-world 9 9)
                 corner (get-cell world 0 0)
                 midside (get-cell world 0 4)
                 centre (get-cell world 4 4)]
             (is (= (count (get-neighbours world corner 1)) 3))
             (is (= (count (get-neighbours world midside 1)) 5))
             (is (= (count (get-neighbours world centre 1)) 8))
             (is (= (count (get-neighbours world corner 2)) 8))
             (is (= (count (get-neighbours world midside 2)) 14))
             (is (= (count (get-neighbours world centre 2)) 24))
             (is (= (count (get-neighbours world corner 3)) 15))
             (is (= (count (get-neighbours world midside 3)) 27))
             (is (= (count (get-neighbours world centre 3)) 48))
             (is (= (count (get-neighbours world corner 4)) 24))
             (is (= (count (get-neighbours world midside 4)) 44))
             (is (= (count (get-neighbours world centre 4)) 80))
             )))
             

(deftest get-neighbours-with-property-value-test
  (testing "Testing the action of the over-complicated utility function"
           (let [world '(({ :altitude 13, :x 0, :y 0, }
                           { :altitude 20, :x 1, :y 0, }
                           { :altitude 29, :x 2, :y 0, }
                           { :altitude 39, :x 3, :y 0, }
                           { :altitude 51, :x 4, :y 0, })
                          ({ :altitude 19, :x 0, :y 1, }
                            { :altitude 29, :x 1, :y 1, }
                            { :altitude 41, :x 2, :y 1, }
                            { :altitude 55, :x 3, :y 1, }
                            { :altitude 72, :x 4, :y 1, })
                          ({ :altitude 27, :x 0, :y 2, }
                            { :altitude 41, :x 1, :y 2, }
                            { :altitude 55, :x 2, :y 2, }
                            { :altitude 72, :x 3, :y 2, }
                            { :altitude 91, :x 4, :y 2, })
                          ({ :altitude 33, :x 0, :y 3, }
                            { :altitude 47, :x 1, :y 3, }
                            { :altitude 68, :x 2, :y 3, }
                            { :altitude 91, :x 3, :y 3, }
                            { :altitude 111, :x 4, :y 3, })
                          ({ :altitude 36, :x 0, :y 4, }
                            { :altitude 53, :x 1, :y 4, }
                            { :altitude 75, :x 2, :y 4, }
                            { :altitude 100, :x 3, :y 4, }
                            { :altitude 123, :x 4, :y 4, }))]
                 (is (= (get-neighbours-with-property-value world 3 3 1 :altitude 100 >) 
                       '({ :altitude 111, :x 4, :y 3, } 
                         { :altitude 123, :x 4, :y 4, })))
                 (is (= (get-neighbours-with-property-value world 3 3 1 :altitude 100 <)
                       '({ :altitude 55, :x 2, :y 2, } 
                          { :altitude 68, :x 2, :y 3, } 
                          { :altitude 75, :x 2, :y 4, } 
                          { :altitude 72, :x 3, :y 2, } 
                          { :altitude 91, :x 4, :y 2, })))
                 (is (= (get-neighbours-with-property-value world 3 3 1 :altitude 100)
                       '({ :altitude 100, :x 3, :y 4, }))))
           (let [world '(({ :altitude 13, :x 0, :y 0, :deer 0}
                           { :altitude 20, :x 1, :y 0, :deer 0}
                           { :altitude 29, :x 2, :y 0, :deer 0}
                           { :altitude 39, :x 3, :y 0, :deer 0}
                           { :altitude 51, :x 4, :y 0, :deer 0})
                          ({ :altitude 19, :x 0, :y 1, :deer 0}
                            { :altitude 29, :x 1, :y 1, :deer 0}
                            { :altitude 41, :x 2, :y 1, :deer 0}
                            { :altitude 55, :x 3, :y 1, :deer 0}
                            { :altitude 72, :x 4, :y 1, :deer 0})
                          ({ :altitude 27, :x 0, :y 2, :deer 0}
                            { :altitude 41, :x 1, :y 2, :deer 0}
                            { :altitude 55, :x 2, :y 2, :deer 0}
                            { :altitude 72, :x 3, :y 2, :deer 2}
                            { :altitude 91, :x 4, :y 2, :deer 0})
                          ({ :altitude 33, :x 0, :y 3, :deer 0}
                            { :altitude 47, :x 1, :y 3, :deer 0}
                            { :altitude 68, :x 2, :y 3, :deer 4}
                            { :altitude 91, :x 3, :y 3, :deer 0}
                            { :altitude 111, :x 4, :y 3, :deer 4})
                          ({ :altitude 36, :x 0, :y 4, :deer 0}
                            { :altitude 53, :x 1, :y 4, :deer 0}
                            { :altitude 75, :x 2, :y 4, }
                            { :altitude 100, :x 3, :y 4, :deer 2}
                            { :altitude 123, :x 4, :y 4,}))]
                 (is (= (get-neighbours-with-property-value world 3 3 1 :deer 2
                                                            >)
                        '({:altitude 68, :x 2, :y 3, :deer 4} 
                           {:altitude 111, :x 4, :y 3, :deer 4})))
                 (is (= (get-neighbours-with-property-value world 3 3 1 :deer 2
                                                            <)
                        '({:altitude 55, :x 2, :y 2, :deer 0} 
                           {:altitude 75, :x 2, :y 4} 
                           {:altitude 91, :x 4, :y 2, :deer 0} 
                           {:altitude 123, :x 4, :y 4})))
                 (is (= (get-neighbours-with-property-value world 3 3 1 :deer 2)
                        '({:altitude 72, :x 3, :y 2, :deer 2} 
                           {:altitude 100, :x 3, :y 4, :deer 2}))))))
