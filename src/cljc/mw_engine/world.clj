(ns ^{:doc "Functions to create and to print two dimensional cellular automata.
            
            Nothing in this namespace should determine what states are possible within
            the automaton, except for the initial state, :new.

            A cell is a map containing at least values for the keys `:x`, `:y`, and `:state`.

            A world is a two dimensional matrix (sequence of sequences) of cells, such
            that every cell's `:x` and `:y` properties reflect its place in the matrix."
      :author "Simon Brooke"}
 mw-engine.world
  (:require [clojure.string :as string]
            [mw-engine.utils :refer [population]]))

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

(defn cell?
  "Return `true` if `obj` is a cell, as understood by MicroWorld, else `false`."
  [obj]
  (and (map? obj) ;; it's a map...
       ;; TODO: it's worth checking (and this does not) that cells have the
       ;; right co-ordinates!
       (pos-int? (:x obj)) ;; with an x co-ordinate...
       (pos-int? (:y obj)) ;; and a y co-ordinate...
       (keyword? (:state obj)))) ;; and a state which is a keyword.

(defn world?
  "Return `true` if `obj` is a world, as understood by MicroWorld, else `false`."
  [obj]
  (and (coll? obj) ;; it's a collection...
       (every? coll? obj) ;; of collections...
       (= 1 (count (set (map count obj)))) ;; all of which are the same length...
       (every? cell? (flatten obj)))) ;; and every element of each of those is a cell.

(defmacro make-cell
  "Create a minimal default cell at x, y

  * `x` the x coordinate at which this cell is created;
  * `y` the y coordinate at which this cell is created."
  [x y]
  `{:x ~x :y ~y :state :new})

(defn make-world
  "Make a world width cells from east to west, and height cells from north to
   south.

  * `width` a natural number representing the width of the matrix to be created;
  * `height` a natural number representing the height of the matrix to be created."
  [width height]
  (apply vector
         (map (fn [h]
                (apply vector (map #(make-cell % h) (range width))))
              (range height))))

(defn truncate-state
  "Truncate the print name of the state of this cell to at most limit characters."
  [cell limit]
  (let [s (:state cell)]
    (cond (> (count (str s)) limit) (subs s 0 limit)
          :else s)))

(defn format-cell
  "Return a formatted string summarising the current state of this cell."
  [cell]
  (format "%10s(%2d/%2d)"
          (truncate-state cell 10)
          (population cell :deer)
          (population cell :wolves)))

(defn- format-world-row
  "Format one row in the state of a world for printing."
  [row]
  (string/join (map format-cell row)))

(defn print-world
  "Print the current state of this world, and return nil.

  * `world` a world as defined above."
  [world]
  (println)
  (dorun
   (map
    #(println
      (format-world-row %))
    world))
  nil)
