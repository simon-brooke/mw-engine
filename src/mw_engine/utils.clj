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

(defn get-cell
  "Return the cell a x, y in this world, if any.

  * `world` a world as defined above;
  * `x` a number which may or may not be a valid x coordinate within that world;
  * `y` a number which may or may not be a valid y coordinate within that world."
  [world x y]
  (cond (in-bounds world x y)
    (nth (nth world y) x)))

(defn get-int
  "Get the value of a property expected to be an integer from a map; if not present (or not an integer) return 0."
  [map key]
  (cond map
    (let [v (map key)]
      (cond (and v (integer? v)) v
            true 0))
        true (throw (Exception. "No map passed?"))))

(defn population
  "Return the population of this species in this cell.

  * `cell` a map;
  * `species` a keyword representing a species which may populate that cell."
  [cell species]
  (get-int cell species))


(defn get-neighbours
  ([world x y depth]
    "Get the neighbours to distance depth of the cell at x, y in this world."
    (map #(get-cell world (first %) (first (rest %)))
       (remove #(= % (list x y))
               (combo/cartesian-product
                 (range (- x depth) (+ x depth))
                 (range (- y depth) (+ y depth))))))
  ([world cell depth]
    "Get the neighbours to distance depth of this cell in this world."
    (get-neighbours world (:x cell) (:y cell) depth))
  ([world cell]
    "Get the immediate neighbours of this cell in this world"
    (get-neighbours world cell 1)))

(defn get-neighbours-with-state
  "Get the neighbours to distance depth of the cell at x, y in this world which
   have this state."
  [world x y depth state]
  (filter #(= (:state %) state) (get-neighbours world x y depth)))
