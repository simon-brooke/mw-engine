;; Utility functions needed by MicroWorld and, specifically, in the interpretation of MicroWorld rule.

(ns mw-engine.utils
  (:require [clojure.math.combinatorics :as combo]))

(defn member?
  "True if elt is a member of col."
  [elt col] (some #(= elt %) col))

(defn in-bounds
  "True if x, y are in bounds for this world (i.e., there is a cell at x, y)
   else false.

  * `world` a world as defined above;
  * `x` a number which may or may not be a valid x coordinate within that world;
  * `y` a number which may or may not be a valid y coordinate within that world."
  [world x y]
  (and (>= x 0)(>= y 0)(< y (count world))(< x (count (first world)))))

(defn map-world 
  "Apply this `function` to each cell in this `world` to produce a new world.
   the arguments to the function will be the world, the cell, and any 
   `additional-args` supplied"
  ([world function]
    (map-world world function nil))
  ([world function additional-args]
    (apply vector ;; vectors are more efficient for scanning, which we do a lot.
         (for [row world]
           (apply vector 
                  (map #(apply function (cons world (cons % additional-args))) 
                       row))))))

(defn get-cell
  "Return the cell a x, y in this world, if any.

  * `world` a world as defined above;
  * `x` a number which may or may not be a valid x coordinate within that world;
  * `y` a number which may or may not be a valid y coordinate within that world."
  [world x y]
  (cond (in-bounds world x y)
    (nth (nth world y) x)))

(defn get-int
  "Get the value of a property expected to be an integer from a map; if not present (or not an integer) return 0.
  
   * `map` a map;
   * `key` a symbol or keyword, presumed to be a key into the `map`."
  [map key]
  (cond (map? map)
    (let [v (map key)]
      (cond (and v (integer? v)) v
            true 0))
        true (throw (Exception. "No map passed?"))))

(defn population
  "Return the population of this species in this cell. Currently a synonym for
   `get-int`, but may not always be (depending whether species are later 
   implemented as actors)

  * `cell` a map;
  * `species` a keyword representing a species which may populate that cell."
  [cell species]
  (get-int cell species))


(defn get-neighbours
    "Get the neighbours to distance depth of the cell at x, y in this world.

    * `world` a world, as described in world.clj;
    * `x` an integer representing an x coordinate in that world;
    * `y` an integer representing an y coordinate in that world;
    * `depth` an integer representing the distance from [x,y] that
      should be searched."
    ([world x y depth]
      (remove nil?
              (map #(get-cell world (first %) (first (rest %)))
                   (remove #(= % (list x y))
                           (combo/cartesian-product
                             (range (- x depth) (+ x depth 1))
                             (range (- y depth) (+ y depth 1)))))))
    ([world cell depth] 
      "Get the neighbours to distance depth of this cell in this world.

      * `world` a world, as described in world.clj;
      * `cell` a cell within that world;
      * `depth` an integer representing the distance from [x,y] that
        should be searched."
      (get-neighbours world (:x cell) (:y cell) depth))
    ([world cell]
      "Get the immediate neighbours of this cell in this world

      * `world` a world, as described in world.clj;
      * `cell` a cell within that world."
      (get-neighbours world cell 1)))

(defn get-neighbours-with-property-value
  "Get the neighbours to distance depth of the cell at x, y in this world which
   have this value for this property.

    * `world` a world, as described in `world.clj`;
    * `cell` a cell within that world;
    * `depth` an integer representing the distance from [x,y] that
      should be searched;
    * `property` a keyword representing a property of the neighbours;
    * `value` a value of that property (or, possibly, the name of another);
    * `op` a comparator function to use in place of `=`.

   It gets messy."
  ([world x y depth property value op]
    (filter 
      #(eval 
         (list op 
               (or (get % property) (get-int % property)) 
               value)) 
      (get-neighbours world x y depth)))
  ([world x y depth property value]
    (get-neighbours-with-property-value world x y depth property value =))
  ([world cell depth property value]
    (get-neighbours-with-property-value world (:x cell) (:y cell) depth 
                                        property value))
  ([world cell property value]
    (get-neighbours-with-property-value world cell 1 
                                        property value)))

(defn get-neighbours-with-state
  "Get the neighbours to distance depth of the cell at x, y in this world which
   have this state.

    * `world` a world, as described in `world.clj`;
    * `cell` a cell within that world;
    * `depth` an integer representing the distance from [x,y] that
      should be searched;
    * `state` a keyword representing a state in the world."
  ([world x y depth state]
    (filter #(= (:state %) state) (get-neighbours world x y depth)))
  ([world cell depth state]
    (get-neighbours-with-state world (:x cell) (:y cell) depth state))
  ([world cell state]
    (get-neighbours-with-state world cell 1 state)))

(defn- set-cell-property
  "If this `cell`s x and y properties are equal to these `x` and `y` values,
   return a cell like this cell but with the value of this `property` set to
   this `value`. Otherwise, just return this `cell`."
  [cell x y property value]
  (cond 
    (and (= x (:x cell)) (= y (:y cell)))
    (merge cell {property value :rule "Set by user"})
    true
    cell))

(defn set-property
  "Return a world like this `world` but with the value of exactly one `property`
   of one `cell` changed to this `value`"
  ([world cell property value]
    (set-property world (:x cell) (:y cell) property value))
  ([world x y property value]
    (apply 
      vector ;; we want a vector of vectors, not a list of lists, for efficiency
      (map
        (fn [row]
          (apply
            vector
            (map #(set-cell-property % x y property value)
                 row)))
        world))))


