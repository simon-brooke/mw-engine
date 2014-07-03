;; Functions to create and to print two dimensional cellular automata. Nothing in this
;; file should determine what states are possible within the automaton, except for the
;; initial state, :new.
;;
;; A cell is a map containing at least values for the keys :x, :y, and :state.
;;
;; A world is a two dimensional matrix (sequence of sequences) of cells, such
;; that every cell's :x and :y properties reflect its place in the matrix.

(ns mw-engine.world
  (:use mw-engine.utils))

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
               (make-world-row (+ index 1) width height))))

(defn- make-world-rows [index width height]
  "Make the (remaining) rows in a world of this width and height, from this
   index.

  * `index` y coordinate of the next row to be created;
  * `width` total width of the matrix, in cells;
  * `height` total height of the matrix, in cells."
  (cond (= index height) nil
    true (cons (make-world-row 0 width index)
               (make-world-rows (+ index 1) width height))))

(defn make-world
  "Make a world width cells from east to west, and height cells from north to
   south.

  * `width` a natural number representing the width of the matrix to be created;
  * `height` a natural number representing the height of the matrix to be created."
  [width height]
  (make-world-rows 0 width height))

(defn truncate-state
  "Truncate the print name of the state of this cell to at most limit characters."
  [cell limit]
  (let [s (:state cell)]
    (cond (> (count (.toString s)) 10) (subs s 0 10)
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
  (apply str
         (map format-cell row)))

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
