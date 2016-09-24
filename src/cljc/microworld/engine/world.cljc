(ns ^{:doc "Functions to create and to print two dimensional cellular automata."
       :author "Simon Brooke"}
  microworld.engine.world
	(:require [clojure.string :as string :only [join]]
            [microworld.engine.utils :refer [population]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; microworld.engine: the state/transition engine of MicroWorld.
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


(defn- make-cell
  "Create a minimal default cell at x, y

  * `x` the x coordinate at which this cell is created;
  * `y` the y coordinate at which this cell is created."
  [x y]
  {:x x :y y :state :new})


(defn- make-world-row
  "Make the (remaining) cells in a row at this height in a world of this width.

  * `index` x coordinate of the next cell to be created;
  * `width` total width of the matrix, in cells;
  * `height` y coordinate of the next cell to be created."
  [index width height]
  (cond (= index width) nil
    true (cons (make-cell index height)
               (make-world-row (inc index) width height))))


(defn- make-world-rows
  "Make the (remaining) rows in a world of this width and height, from this
   index.

  * `index` y coordinate of the next row to be created;
  * `width` total width of the matrix, in cells;
  * `height` total height of the matrix, in cells."
  [index width height]
  (cond (= index height) nil
    true (cons (apply vector (make-world-row 0 width index))
               (make-world-rows (inc index) width height))))

(defn make-world
  "Make a world width cells from east to west, and height cells from north to
   south.

  * `width` a natural number representing the width of the matrix to be created;
  * `height` a natural number representing the height of the matrix to be created."
  [width height]
  (apply vector (make-world-rows 0 width height)))


(defn truncate-state
  "Truncate the print name of the state of this cell to at most limit characters."
  [cell limit]
  (let [s (:state cell)]
    (cond (> (count (str s)) limit) (subs s 0 limit)
      true s)))


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
