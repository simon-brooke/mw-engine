(ns mw-engine.core
  (:require mw-engine.world
        mw-engine.natural-rules
        mw-engine.utils))

;; every rule is a function of two arguments, a cell and a world. If the rule
;; fires, it returns a new cell, which should have the same values for :x and
;; :y as the old cell. Anything else can be modified.
;;
;; Rules are applied in turn until one matches.

(defn transform-cell 
  "Derive a cell from this cell of this world by applying these rules."
  [cell world rules]
  (cond (empty? rules) cell
    true (let [r (apply (eval (first rules)) (list cell world))]
           (cond r r
             true (transform-cell cell world (rest rules))))))

(defn transform-world-row 
  "Return a row derived from this row of this world by applying these rules to each cell."
  [row world rules]
  (map #(transform-cell % world rules) row))

(defn transform-world 
  "Return a world derived from this world by applying these rules to each cell."
  [world rules]
  (map
    #(transform-world-row % world rules)
    world))

(defn transform-world-state 
  "Consider this single argument as a list of world and rules; apply the rules
   to transform the world, and return a list of the new, transformed world and
   these rules. As a side effect, print the world."
  [state]
  (list
    (print-world (transform-world (first state) (first (rest state))))
    (first (rest state))))

(defn run-world 
  "Run this world with these rules for this number of generations."
  [world rules generations]
  (let [state (list world rules)]
    (take generations (iterate transform-world-state state))))

(defn animate-world
  [world rules generations]
  (let [state (list world rules)]
    (dorun 
      (take generations (iterate transform-world-state state)))
    nil))