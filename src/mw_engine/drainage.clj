;; Experimental, probably of no interest to anyone else; attempt to compute drainage on a world,
;; assumed to have altitudes already set from a heighmap.

(ns mw-engine.drainage
  (:use mw-engine.utils
        mw-engine.world)
  (:require [mw-engine.heightmap :as heightmap]))

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
  [world cell]
  (remove nil?
          (into []
            (map
             (fn [n]
               (cond (= cell (get-least-cell (get-neighbours world n) :altitude)) n
                 (and (= (:altitude cell) (:altitude n))(> (or (:flow n) 0) (or (:flow cell) 0))) n))
             (get-neighbours-with-property-value world (:x cell) (:y cell) 1
                                                                  :altitude
                                                                  (or (:altitude cell) 0) >=)))))

(defn flood-hollow
  "Raise the altitude of a copy of this `cell` of this `world` to one unit above the lowest of these `neighbours`, and reflow."
  [cell world neighbours]
  (let [lowest (get-least-cell neighbours :altitude)]
    (flow world (merge cell {:altitude (+ (:altitude lowest) 1)}))))
;;  cell)

(def flow
  "Compute the total flow upstream of this `cell` in this `world`, and return a cell identical
  to this one but having a value of its flow property set from that computation. The function is
  memoised because the consequence of mapping a recursive function across an array is that many
  cells will be revisited - potentially many times.

  Flow comes from a higher cell to a lower only if the lower is the lowest neighbour of the higher."
  (memoize
   (fn [world cell]
     (cond
      (not (nil? (:flow cell))) cell
      (<= (or (:altitude cell) 0) *sealevel*) cell
      true
      (let [contributors (flow-contributors world cell)]
;;        (if
;;          (= (count contributors) 8)
          ;; local lowspot - lake bottom
;;          (flood-hollow cell world contributors)
          ;; otherwise...
          (merge cell
                 {:flow (+ (:rainfall cell)
                           (apply +
                                  (map (fn [neighbour] (:flow (flow world neighbour)))
                                       (flow-contributors world cell))))}))))))

(defn flow-world
  "Return a world like this `world`, but with cells tagged with the amount of
   water flowing through them."
  [world]
  (map-world (rain-world world) flow))

(defn run-drainage
  [hmap]
  "Create a world from the heightmap `hmap`, rain on it, and then compute river 
   flows."
  (flow-world (rain-world (heightmap/apply-heightmap hmap))))
