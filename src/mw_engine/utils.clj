;; Utility functions needed by MicroWorld and, specifically, in the interpretation of MicroWorld rule.

(ns mw-engine.utils
  (:require
;;    [clojure.core.reducers :as r]
    [clojure.math.combinatorics :as combo]))

(defn abs
  "Surprisingly, Clojure doesn't seem to have an abs function, or else I've
   missed it. So here's one of my own. Maps natural numbers onto themselves,
   and negative integers onto natural numbers. Also maps negative real numbers
   onto positive real numbers.

   * `n` a number, on the set of real numbers."
  [n]
  (if (neg? n) (- 0 n) n))

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

(defn map-world-n-n
  "Wholly non-parallel map world implementation"
  ([world function]
    (map-world-n-n world function nil))
  ([world function additional-args]
    (into []
           (map (fn [row]
                    (into [] (map
                             #(apply function
                                     (cons world (cons % additional-args)))
                             row)))
                  world))))

(defn map-world-p-p
  "Wholly parallel map world implementation"
  ([world function]
    (map-world-p-p world function nil))
  ([world function additional-args]
    (into []
           (pmap (fn [row]
                    (into [] (pmap
                             #(apply function
                                     (cons world (cons % additional-args)))
                             row)))
                  world))))


(defn map-world
  "Apply this `function` to each cell in this `world` to produce a new world.
   the arguments to the function will be the world, the cell, and any
   `additional-args` supplied. Note that we parallel map over rows but
   just map over cells within a row. That's because it isn't worth starting
   a new thread for each cell, but there may be efficiency gains in
   running rows in parallel."
  ([world function]
    (map-world world function nil))
  ([world function additional-args]
    (into []
           (pmap (fn [row]
                    (into [] (map
                             #(apply function
                                     (cons world (cons % additional-args)))
                             row)))
                  world))))

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

(def memo-get-neighbours
  "Memoised core primitive for `get-neighbours` for efficiency."
  (memoize
   (fn [world x y depth]
     (remove nil?
             (map #(get-cell world (first %) (first (rest %)))
                  (remove #(= % (list x y))
                          (combo/cartesian-product
                            (range (- x depth) (+ x depth 1))
                            (range (- y depth) (+ y depth 1)))))))))

(defn get-neighbours
    "Get the neighbours to distance depth of the cell at x, y in this world.

    * `world` a world, as described in world.clj;
    * `x` an integer representing an x coordinate in that world;
    * `y` an integer representing an y coordinate in that world;
    * `depth` an integer representing the distance from [x,y] that
      should be searched."
    ([world x y depth]
      (memo-get-neighbours world x y depth))
    ([world cell depth]
      "Get the neighbours to distance depth of this cell in this world.

      * `world` a world, as described in world.clj;
      * `cell` a cell within that world;
      * `depth` an integer representing the distance from [x,y] that
        should be searched."
      (memo-get-neighbours world (:x cell) (:y cell) depth))
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

(defn get-least-cell
  "Return the cell from among these `cells` which has the lowest numeric value
  for this `property`; if the property is absent or not a number, use this
  `default`"
  ([cells property default]
  (cond
   (empty? cells) nil
   true (let [downstream (get-least-cell (rest cells) property default)]
          (cond (<
                 (or (property (first cells)) default)
                 (or (property downstream) default)) (first cells)
                true downstream))))
  ([cells property]
   (get-least-cell cells property (Integer/MAX_VALUE))))


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

(defn merge-cell
  "Return a world like this `world`, but merge the values from this `cell` with
   those from the cell in the world with the same co-ordinates"
  [world cell]
  (if (in-bounds world (:x cell) (:y cell))
    (map-world world
               #(if 
                  (and
                    (= (:x cell)(:x %2))
                    (= (:y cell)(:y %2)))
                  (merge %2 cell)
                  %2))
    world))
