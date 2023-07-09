(ns mw-engine.flow
  "Allow flows of values between cells in the world."
  (:require [mw-engine.utils :refer [get-cell get-num merge-cell]]
            [taoensso.timbre :refer [warn]]))

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
;;;;
;;;; Functions to create and to print two dimensional cellular automata.
;;;; Nothing in this namespace should determine what states are possible within
;;;; the automaton, except for the initial state, :new.
;;;;
;;;; A cell is a map containing at least values for the keys :x, :y, and :state.
;;;;
;;;; A world is a two dimensional matrix (sequence of sequences) of cells, such
;;;; that every cell's :x and :y properties reflect its place in the matrix.
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; OK, the design here is: a flow object is a map with the following properties:
;; 1. :source, whose value is a location;
;; 2. :destination, whose value is a location;
;; 3. :property, whose value is a keyword;
;; 4. :quantity, whose value is a positive real number.
;; 
;; A location object is a map with the following properties:
;; 1. :x, whose value is a natural number not greater than the extent of the world;
;; 2. :y, whose value is a natural number not greater than the extent of the world.
;;
;; to execute a flow is transfer the quantity specified of the property specified
;; from the cell at the source specified to the cell at the destination specified;
;; if the source doesn't have sufficient of the property, then all it has should
;; be transferred, but no more. 

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
   this `world`, else `false`. Assumes square worlds."
  [o world]
  (try
    (and (map? o)
         (coordinate? (:x o) world)
         (coordinate? (:y o) world))
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
  [flow world]
  (try
    (let [source (get-cell world (-> flow :source :x) (-> flow :source :y))
        dest (get-cell world (-> flow :destination :x) (-> flow :destination :y))
        p (:property flow)
        q (min (:quantity flow) (get-num source p))
        s' (assoc source p (- (source p) q))
        d' (assoc dest p (+ (get-num dest p) q))]
    (merge-cell (merge-cell world s') d'))
    (catch Exception e
      (warn "Failed to execute flow %s: %s" flow (.getMessage e))
      ;; return the world unmodified.
      world)))

(defn execute-flows
  "Return a world like this `world`, but with each of these flows executed."
  [flows world]
  (reduce execute world (filter #(flow? % world) flows)))