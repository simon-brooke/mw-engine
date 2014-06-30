(ns mw-engine.world
  (:require [clojure.math.combinatorics :as combo]))

(defn make-cell 
  "Create a default cell at x, y"
  [x y]
  {:x x :y y :altitude 1 :state :pasture})

(defn make-world-row 
  "Make the (remaining) cells in a row at this height in a world of this width."
  [index width height]
  (cond (= index width) nil
    true (cons (make-cell index height)
               (make-world-row (+ index 1) width height))))

(defn make-world-rows [index width height]
  "Make the (remaining) rows in a world of this width and height, from this
   index."
  (cond (= index height) nil
    true (cons (apply vector (make-world-row 0 width index))
               (make-world-rows (+ index 1) width height))))

(defn make-world 
  "Make a world width cells from east to west, and height cells from north to
   south."
  [width height]
  (apply vector (make-world-rows 0 width height)))

(defn in-bounds   
  "True if x, y are in bounds for this world (i.e., there is a cell at x, y)
   else false."
  [world x y]
  (and (>= x 0)(>= y 0)(< y (count world))(< x (count (first world)))))

(defn get-cell 
  "Return the cell a x, y in this world, if any."
  [world x y]
  (cond (in-bounds world x y)
    (nth (nth world y) x)))

(defn get-neighbours 
  "Get the neighbours to distance depth of the cell at x, y in this world."
  [world x y depth]
  (map #(get-cell world (first %) (first (rest %))) 
       (combo/cartesian-product 
         (range (- x depth) (+ x depth)) 
         (range (- y depth) (+ y depth)))))

(defn truncate-state
  "Truncate the print name of the state of this cell to at most limit characters."
  [cell limit]
  (let [s (:state cell)]
    (cond (> (count (.toString s)) 10) (subs s 0 10)
      true s)))

(defn format-world-row
  "Format one row in the state of a world for printing"
  [row]
  (apply str  
         (map #(format "%10s" (truncate-state % 10)) row)))

(defn print-world
  "Print the current state of this world."
  [world]
  (dorun (map #(println (format-world-row %)) world)) nil)
