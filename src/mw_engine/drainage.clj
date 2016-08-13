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
;;
;; This program is free software; you can redistribute it and/or
;; modify it under the terms of the GNU General Public License
;; as published by the Free Software Foundation; either version 2
;; of the License, or (at your option) any later version.
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with this program; if not, write to the Free Software
;; Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
;; USA.
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def ^:dynamic *sealevel* 10)

;; forward declaration of flow, to allow for a wee bit of mutual recursion.
(declare flow)

(defn rain-world
  "Simulate rainfall on this `world`. TODO: Doesn't really work just now - should
   rain more on west-facing slopes, and less to the east of high ground"
  [world]
  (map-world world (fn [world cell] (merge cell {:rainfall 1}))))


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
  (if (= (- max-altitude (get-int-or-zero cell :generation))
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
      true
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


(defn run-drainage
  [hmap]
  "Create a world from the heightmap `hmap`, rain on it, and then compute river
   flows."
  (flow-world (rain-world (flood-hollows (heightmap/apply-heightmap hmap)))))
