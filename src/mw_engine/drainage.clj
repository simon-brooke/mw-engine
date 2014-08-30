;; Experimental, probably of no interest to anyone else; attempt to compute drainage on a world,
;; assumed to have altitudes already set from a heighmap.

(ns mw-engine.drainage
  (:require
    [clojure.core.reducers :as r])
  (:use mw-engine.utils
        mw-engine.world))

(def ^:dynamic *sealevel* 10)

(defn rain-world
  "Simulate rainfall on this `world`. TODO: Doesn't really work just now - should
   rain more on west-facing slopes, and less to the east of high ground"
  [world]
  (map-world world (fn [world cell] (merge cell {:rainfall 1}))))

(defn flow-contributors
  "Return a list of the cells in this `world` which are higher than this
  `cell` and for which this cell is the lowest neighbour"
  [world cell]
  (remove nil?
          (into []
            (r/map
             (fn [n]
               (cond (= cell (get-least-cell (get-neighbours world n) :altitude)) n))
             (get-neighbours-with-property-value world (:x cell) (:y cell) 1
                                                                  :altitude
                                                                  (or (:altitude cell) 0) >)))))

(defn flow
  "Compute the total flow upstream of this `cell` in this `world`, and return a cell identical
   to this one but having a value of its flow property set from that computation.

   Flow comes from a higher cell to a lower only if the lower is the lowest neighbour of the higher."
   [world cell]
   (cond
    (> (or (:altitude cell) 0) *sealevel*)
      (merge cell
           {:flow (+ (:rainfall cell)
               (apply +
                 (map (fn [neighbour] (:flow (flow world neighbour)))
                      (flow-contributors world cell))))})
    true cell))

(defn flow-world
  "Return a world like this `world`, but with cells tagged with the amount of
   water flowing through them."
  [world]
  (map-world (rain-world world) flow))
