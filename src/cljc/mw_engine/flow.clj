(ns mw-engine.flow
  "Allow flows of values between cells in the world.
   
   The design here is: a flow object is a map with the following properties:
   1. :source, whose value is a location;
   2. :destination, whose value is a location;
   3. :property, whose value is a keyword;
   4. :quantity, whose value is a positive real number.

   A location object is a map with the following properties:
   1. :x, whose value is a natural number not greater than the extent of the world;
   2. :y, whose value is a natural number not greater than the extent of the world.

   To execute a flow is transfer the quantity specified of the property specified
   from the cell at the source specified to the cell at the destination specified;
   if the source doesn't have sufficient of the property, then all it has should
   be transferred, but no more: properties to be flowed cannot be pulled negative.
   
   Flowing values through the world is consequently a two stage process: firstly
   there's a planning stage, in which all the flows to be executed are computed
   without changing the world, and then an execution stage, where they're all 
   executed. This namespace deals with mainly with execution."
  (:require [mw-engine.utils :refer [get-cell get-num in-bounds? merge-cell]]
            [taoensso.timbre :refer [info warn]]))

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

(defn coordinate?
  "Return `true` if this object `o` is a valid coordinate with respect to
     this `world`, else `false`. Assumes square worlds."
  [o world]
  (try
    (and (or (zero? o) (pos-int? o))
         (< o (count world)))
    (catch Exception e
      (warn (format "Not a valid coordinate: %s; %s" o (.getMessage e)))
      false)))

(defn location?
  "Return `true` if this object `o` is a location as defined above with respect to
   this `world`, else `false`."
  [o world]
  (try
    (and (map? o)
         (integer? (:x o))
         (integer? (:y o))
         (in-bounds? world (:x o) (:y o)))
    (catch Exception e
      (warn (format "Not a valid location: %s; %s" o (.getMessage e)))
      false)))

(defn flow?
  "Return `true` if this object `o` is a flow as defined above with respect to
   this `world`, else `false`. Assumes square worlds."
  [o world]
  (try
    (and (map? o)
         (location? (:source o) world)
         (location? (:destination o) world)
         (keyword? (:property o))
         (pos? (:quantity o)))
    (catch Exception e
      (warn (format "Not a valid flow: %s; %s" o (.getMessage e)))
      false)))

(defn execute
  "Return a world like this `world`, except with the quantity of the property
   described in this `flow` object transferred from the source of that flow
   to its destination."
  [world flow]
  (try
    (let [sx (-> flow :source :x)
          sy (-> flow :source :y)
          source (get-cell world sx sy)
          dx (-> flow :destination :x)
          dy (-> flow :destination :y)
          dest (get-cell world dx dy)
          p (:property flow)
          q (min (:quantity flow) (get-num source p))
          s' (assoc source p (- (source p) q))
          d' (assoc dest p (+ (get-num dest p) q))]
      (info (format "Moving %f units of %s from %d,%d to %d,%d"
                    (float q) (name p) sx sy dx dy))
      (merge-cell (merge-cell world s') d'))
    (catch Exception e
      (warn (format "Failed to execute flow %s: %s" flow (.getMessage e)))
      ;; return the world unmodified.
      world)))

(defn execute-flows
  "Return a world like this `world`, but with each of these flows executed."
  [world flows]
  (reduce execute world (filter #(flow? % world) flows)))