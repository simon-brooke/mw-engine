(ns ^{:doc "Experimental, probably of no interest to anyone else; attempt to
      compute drainage on a world, assumed to have altitudes already set
      from a heightmap."
      :author "Simon Brooke"}
  mw-engine.drainage
  (:require [mw-engine.core :refer [run-world]]
            [mw-engine.heightmap :as heightmap]
            [mw-engine.utils :refer [get-int-or-zero get-least-cell get-neighbours
                                     get-neighbours-with-property-value
                                     map-world]]))

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


(def ^:dynamic *sealevel* 10)

;; forward declaration of flow, to allow for a wee bit of mutual recursion.
(declare flow)

(defn rainfall
  "Compute rainfall for a cell with this `gradient` west-east, given
  `remaining` drops to distribute, and this overall map width."
  [gradient remaining map-width]
    (cond
      ;; if there's no rain left in the cloud, it can't fall;
      (zero? remaining)
      0
      (pos? gradient)
      ;; rain, on prevailing westerly wind, falls preferentially on rising ground;
      (int (rand gradient))
      ;; rain falls randomly across the width of the map...
      (zero? (int (rand map-width))) 1
      :else
      0))

(defn rain-row
  "Return a row like this `row`, across which rainfall has been distributed;
  if `rain-probability` is specified, it is the probable rainfall on a cell
  with no gradient."
  ([row]
   (rain-row row 1))
  ([row rain-probability]
   (rain-row row (count row) 0 (int (* (count row) rain-probability))))
  ([row map-width previous-altitude drops-in-cloud]
   (cond
     (empty? row) nil
     (pos? drops-in-cloud)
     (let [cell (first row)
           alt (or (:altitude cell) 0)
           rising (- alt previous-altitude)
           fall (rainfall rising drops-in-cloud map-width)]
       (cons
         (assoc cell :rainfall fall)
         (rain-row (rest row) map-width alt (- drops-in-cloud fall))))
     :else
     (map
       #(assoc % :rainfall 0)
       row))))


(defn rain-world
  "Simulate rainfall on this `world`. TODO: Doesn't really work just now - should
   rain more on west-facing slopes, and less to the east of high ground"
  [world]
  (map
    rain-row
    world))


(defn flow-contributors
  "Return a list of the cells in this `world` which are higher than this
  `cell` and for which this cell is the lowest neighbour, or which are at the
   same altitude and have greater flow"
  [cell world]
  (filter #(map? %)
          (map
            (fn [n]
              (cond
                (= cell (get-least-cell (get-neighbours world n) :altitude)) n
                (and (= (:altitude cell) (:altitude n))
                     (> (or (:flow n) 0) (or (:flow cell) 0))) n))
            (get-neighbours-with-property-value
              world (:x cell) (:y cell) 1 :altitude
              (or (:altitude cell) 0) >=))))


(defn is-hollow
  "Detects point hollows - that is, individual cells all of whose neighbours
   are higher. Return true if this `cell` has an altitude lower than any of
   its neighbours in this `world`"
  [world cell]
  ;; quicker to count the elements of the list and compare equality of numbers
  ;; than recursive equality check on members, I think. But worth benchmarking.
  (let [neighbours (get-neighbours world cell)
        altitude (get-int-or-zero cell :altitude)]
    (= (count neighbours)
       (count (get-neighbours-with-property-value
                world (:x cell) (:y cell) 1 :altitude altitude >)))))


(defn flood-hollow
  "Raise the altitude of a copy of this `cell` of this `world` to the altitude
   of the lowest of its `neighbours`."
  ([world cell neighbours]
    (let [lowest (get-least-cell neighbours :altitude)]
      (merge cell {:state :water :altitude (:altitude lowest)})))
  ([world cell]
    (flood-hollow world cell (get-neighbours world cell))))


(defn flood-hollows
  "Flood all local hollows in this `world`. At this stage only floods single
   cell hollows."
  [world]
  (map-world world
             #(if (is-hollow %1 %2) (flood-hollow %1 %2) %2)))


(def max-altitude 255)

(defn flow-nr
  "Experimental non recursive flow algorithm, needs to be run on a world as
   many times as there are distinct altitude values. This algorithm works only
   if applied sequentially from the highest altitude to the lowest, see
   `flow-world-nr`."
  [cell world]
  (when (= (- max-altitude (get-int-or-zero cell :generation))
         (get-int-or-zero cell :altitude))
    (merge cell
           {:flow (reduce +
                          (map
                            #(+ (get-int-or-zero % :rainfall)
                                (get-int-or-zero % :flow))
                            (flow-contributors cell world)))})))


(def flow
  "Compute the total flow upstream of this `cell` in this `world`, and return a cell identical
  to this one but having a value of its flow property set from that computation. The function is
  memoised because the consequence of mapping a recursive function across an array is that many
  cells will be revisited - potentially many times.

  Flow comes from a higher cell to a lower only if the lower is the lowest neighbour of the higher."
  (memoize
   (fn [cell world]
     (cond
      (not (nil? (:flow cell))) cell
      (<= (or (:altitude cell) 0) *sealevel*) cell
      :else
      (merge cell
             {:flow (+ (:rainfall cell)
                       (apply +
                              (map (fn [neighbour] (:flow (flow neighbour world)))
                                   (flow-contributors cell world))))})))))


(defn flow-world-nr
  "Experimental non-recursive flow-world algorithm"
  [world]
  (run-world world nil (list flow-nr) max-altitude))

(defn flow-world
  "Return a world like this `world`, but with cells tagged with the amount of
   water flowing through them."
  [world]
  (map-world (rain-world world) flow))

(defn explore-lake
  "Return a sequence of cells starting with this `cell` in this `world` which
  form a contiguous lake"
  [world cell]
  )

(defn is-lake?
  "If this `cell` in this `world` is not part of a lake, return nil. If it is,
  return a cell like this `cell` tagged as part of a lake."
  [world cell]
  (if
    ;; if it's already tagged as a lake, it's a lake
    (:lake cell) cell
    (let
      [outflow (apply min (map :altitude (get-neighbours world cell)))]
      (when-not
        (> (:altitude cell) outflow)
        (assoc cell :lake true)))))


(defn find-lakes
  [world]
  )

(defn run-drainage
  "Create a world from the heightmap `hmap`, rain on it, and then compute river
   flows."
  [hmap]
  (flow-world (rain-world (flood-hollows (heightmap/apply-heightmap hmap)))))
