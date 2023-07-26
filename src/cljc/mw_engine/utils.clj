(ns ^{:doc " Utility functions needed by MicroWorld and, specifically, in the
      interpretation of MicroWorld rule."
      :author "Simon Brooke"}
 mw-engine.utils
  (:require [clojure.math.combinatorics :as combo]
            [clojure.string :refer [join]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; mw-engine: the state/transition engine of MicroWorld.
;;;;
;;;; This program is free software; you can redistribute it and/or
;;;; modify it under the terms of the GNU General Public License
;;;; as published by the Free Software Foundation; either version 2
;;;; of the License, or (at your option) any later version.
;;;;
;;;; This program is distributed in the hope that it will be useful,
;;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;;; GNU General Public License for more details.
;;;;
;;;; You should have received a copy of the GNU General Public License
;;;; along with this program; if not, write to the Free Software
;;;; Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
;;;; USA.
;;;;
;;;; Copyright (C) 2014 Simon Brooke
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn member?
  "Return 'true' if elt is a member of col, else 'false'."
  [elt col]
  (contains? (set col) elt))

(defn get-int-or-zero
  "Return the value of this `property` from this `map` if it is a integer;
   otherwise return zero."
  [map property]
  (let [value (map property)]
    (if (integer? value) value 0)))

(defn init-generation
  "Return a cell like this `cell`, but having a value for :generation, zero if
   the cell passed had no integer value for generation, otherwise the value
   taken from the cell passed. The `world` argument is present only for
   consistency with the rule engine and is ignored."
  [_ cell]
  (merge cell {:generation (get-int-or-zero cell :generation)}))

(defn in-bounds
  "True if x, y are in bounds for this world (i.e., there is a cell at x, y)
   else false. *DEPRECATED*: it's a predicate, prefer `in-bounds?`.

  * `world` a world as defined in [world.clj](mw-engine.world.html);
  * `x` a number which may or may not be a valid x coordinate within that world;
  * `y` a number which may or may not be a valid y coordinate within that world."
  {:deprecated "1.1.7"}
  [world x y]
  (and (>= x 0) (>= y 0) (< y (count world)) (< x (count (first world)))))

(defn in-bounds?
  "True if x, y are in bounds for this world (i.e., there is a cell at x, y)
   else false.

  * `world` a world as defined in [world.clj](mw-engine.world.html);
  * `x` a number which may or may not be a valid x coordinate within that world;
  * `y` a number which may or may not be a valid y coordinate within that world."
  {:added "1.1.7"}
  [world x y]
  (and (>= x 0) (>= y 0) (< y (count world)) (< x (count (first world)))))

(defn map-world-n-n
  "Wholly non-parallel map world implementation; see documentation for `map-world`."
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
  "Wholly parallel map-world implementation; see documentation for `map-world`."
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

  * `world` a world as defined in [world.clj](mw-engine.world.html);
  * `x` a number which may or may not be a valid x coordinate within that world;
  * `y` a number which may or may not be a valid y coordinate within that world."
  [world x y]
  (when (in-bounds? world x y)
    (nth (nth world y) x)))

(defn get-int
  "Get the value of a property expected to be an integer from a map; if not
   present (or not an integer) return 0.

  * `map` a map;
  * `key` a symbol or keyword, presumed to be a key into the `map`."
  [map key]
  (if (map? map)
    (let [v (map key)]
      (cond (and v (integer? v)) v
            :else 0))
    (throw (Exception. "No map passed?"))))

(defmacro get-num
  "Get the value of a property expected to be a number from a map; if not
   present (or not a number) return 0.

  * `map` a map;
  * `key` a symbol or keyword, presumed to be a key into the `map`."
  [map key]
  `(if (map? ~map)
     (let [~'v (~map ~key)]
       (cond (and ~'v (number? ~'v)) ~'v
             :else 0))
     (throw (Exception. "No map passed?"))))

(defn population
  "Return the population of this species in this cell. Currently a synonym for
   `get-int`, but may not always be (depending whether species are later
   implemented as actors)

  * `cell` a map;
  * `species` a keyword representing a species which may populate that cell."
  [cell species]
  (get-int cell species))

(def memo-get-neighbours
  "Memoised get neighbours is more efficient when running deeply recursive
   algorithms on the same world. But it's less efficient when running the
   engine in its normal iterative style, because then we will rarely call
   get naighbours on the same cell of the same world twice."
  (memoize
   (fn [world x y depth]
     (remove nil?
             (map #(get-cell world (first %) (first (rest %)))
                  (remove #(= % (list x y))
                          (combo/cartesian-product
                           (range (- x depth) (+ x depth 1))
                           (range (- y depth) (+ y depth 1)))))))))

(defn get-neighbours
  "Get the neighbours to distance depth of a cell in this world.

    Several overloads:
    * `world` a world, as described in [world.clj](mw-engine.world.html);
    * `cell` a cell within that world
    Gets immediate neighbours of the specified cell.

    * `world` a world, as described in[world.clj](mw-engine.world.html);
    * `cell` a cell within that world
    * `depth` an integer representing the depth to search from the
      `cell`
    Gets neighbours within the specified distance of the cell.

    * `world` a world, as described in[world.clj](mw-engine.world.html);
    * `x` an integer representing an x coordinate in that world;
    * `y` an integer representing an y coordinate in that world;
    * `depth` an integer representing the distance from [x,y] that
      should be searched
    Gets the neighbours within the specified distance of the cell at
    coordinates [x,y] in this world."
  ([world x y depth]
   (memo-get-neighbours world x y depth))
  ([world cell depth]
   (memo-get-neighbours world (:x cell) (:y cell) depth))
  ([world cell]
   (memo-get-neighbours world (:x cell) (:y cell) 1)))

(defn get-neighbours-with-property-value
  "Get the neighbours to distance depth of the cell at x, y in this world which
   have this value for this property.

    * `world` a world, as described in [world.clj](mw-engine.world.html);
    * `cell` a cell within that world;
    * `depth` an integer representing the distance from [x,y] that
      should be searched (optional);
    * `property` a keyword representing a property of the neighbours;
    * `value` a value of that property (or, possibly, the name of another);
    * `op` a comparator function to use in place of `=` (optional).

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

    * `world` a world, as described in [world.clj](mw-engine.world.html);
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
  for this `property`."
  [cells property]
  (first (sort-by property (filter #(number? (property %)) cells))))

(defn get-most-cell
  "Return the cell from among these `cells` which has the highest numeric value
  for this `property`."
  [cells property]
  (last (sort-by property (filter #(number? (property %)) cells))))

(defn- set-cell-property
  "If this `cell`s x and y properties are equal to these `x` and `y` values,
   return a cell like this cell but with the value of this `property` set to
   this `value`. Otherwise, just return this `cell`."
  [cell x y property value]
  (cond
    (and (= x (:x cell)) (= y (:y cell)))
    (merge cell {property value :rule "Set by user"})
    :else cell))

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
  (if (in-bounds? world (:x cell) (:y cell))
    (map-world world
               #(if
                 (and
                  (= (:x cell) (:x %2))
                  (= (:y cell) (:y %2)))
                  (merge %2 cell)
                  %2))
    world))

(defn rule-type
  "Return the rule-type of this compiled `rule`."
  [rule]
  (:rule-type (meta rule)))

(defn add-history-event
  "If `cell` is non-nil, expect it to be a map representing a cell; add
   to its history an an event recording the firing of this rule. If
   `detail` is passed, treat it as a map of additional data to be
   added to the event."
  ([cell rule]
   (when cell (add-history-event cell rule {})))
  ([result rule detail]
   (when result
     (let [rule-meta (meta rule)
           event {:rule (:source rule-meta)
                  :rule-type (:rule-type rule-meta)
                  :generation (get-int-or-zero
                               result
                               :generation)}
           event' (if detail (merge event detail) event)]
       (merge result
              {:history (concat
                         (:history result)
                         (list event'))})))))

(defn- event-narrative [event]
  (case (:rule-type event)
    :production (:rule event)
    :flow (format "%s %f units of %s %s %d,%d:\n    %s"
                  (name (:direction event))
                  (:quantity event)
                  (:property event)
                  (if (= :sent (:direction event)) "to" "from")
                  (:x (:other event))
                  (:y (:other event))
                  (:rule event))))

(defn history-string
  "Return the history of this `cell` as a string for presentation to the user."
  [cell]
  (join "\n"
        (map #(format "%6d: %s" (:generation %) (event-narrative %))
             (:history cell))))

(defn- extend-summary [summary rs rl event]
  (str summary
       (if rs (format "%d-%d (%d occurances): %s\n" rs
                      (:generation event)
                      rl
                      (event-narrative event))
           (format "%d: %s\n" (:generation event)
                   (event-narrative event)))))

(defn summarise-history
  "Return, as a string, a shorter summary of the history of this cell"
  [cell]
  (loop [history (rest (:history cell))
         event (first (:history cell))
         prev nil
         rs nil
         rl 0
         summary ""]
    (cond (nil? event) (extend-summary summary rs rl prev)
          (= (:rule event) (:rule prev)) (recur
                                          (rest history)
                                          (first history)
                                          event
                                          (or rs (:generation event))
                                          (inc rl)
                                          summary)
          :else (recur (rest history)
                       (first history)
                       event
                       nil
                       0
                       (extend-summary summary rs (inc rl) event)))))
