;; Functions to transform a world and run rules.

(ns mw-engine.core
  (:require [mw-engine.world :as world]
        mw-engine.natural-rules
        mw-engine.utils))

;; Every rule is a function of two arguments, a cell and a world. If the rule
;; fires, it returns a new cell, which should have the same values for :x and
;; :y as the old cell. Anything else can be modified.
;;
;; A cell is a map containing at least values for the keys :x, :y, and :state;
;; a transformation should not alter the values of :x or :y, and should not
;; return a cell without a keyword as the value of :state. Anything else is
;; legal.
;;
;; A world is a two dimensional matrix (sequence of sequences) of cells, such
;; that every cell's :x and :y properties reflect its place in the matrix.
;; See `world.clj`.
;;
;; Rules are applied in turn until one matches.

(defn- transform-cell
  "Derive a cell from this cell of this world by applying these rules."
  [cell world rules]
  (cond (empty? rules) cell
    true (let [result (apply (eval (first rules)) (list cell world))]
           (cond result result
             true (transform-cell cell world (rest rules))))))

(defn- transform-world-row
  "Return a row derived from this row of this world by applying these rules to each cell."
  [row world rules]
  (map #(transform-cell % world rules) row))

(defn transform-world
  "Return a world derived from this world by applying these rules to each cell."
  [world rules]
  (map
    #(transform-world-row % world rules)
    world))

(defn- transform-world-state
  "Consider this single argument as a map of `:world` and `:rules`; apply the rules
   to transform the world, and return a map of the new, transformed `:world` and
   these `:rules`. As a side effect, print the world."
  [state]
  (let [world (transform-world (:world state) (:rules state))]
    (world/print-world world)
    {:world world :rules (:rules state)}))


(defn run-world
  "Run this world with these rules for this number of generations.

  * `world` a world as discussed above;
  * `init-rules` a sequence of rules as defined above, to be run once to initialise the world;
  * `rules` a sequence of rules as definied above, to be run iteratively for each generation;
  * `generations` an (integer) number of generations."
  [world init-rules rules generations]
  (let [state {:world (transform-world world init-rules) :rules rules}]
    (dorun (take generations (iterate transform-world-state state)))))

(defn animate-world
  "Run this world with these rules for this number of generations, and return nil
  to avoid cluttering the screen. Principally for debugging.

  * `world` a world as discussed above;
  * `init-rules` a sequence of rules as defined above, to be run once to initialise the world;
  * `rules` a sequence of rules as definied above, to be run iteratively for each generation;
  * `generations` an (integer) number of generations."
  [world init-rules rules generations]
  (let [state (list (transform-world world init-rules) rules)]
    (dorun
      (take generations (iterate transform-world-state state)))
    world))
