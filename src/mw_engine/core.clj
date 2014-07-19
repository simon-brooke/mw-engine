;; Functions to transform a world and run rules.

(ns mw-engine.core
  (:require [mw-engine.world :as world]
        mw-engine.natural-rules
        mw-engine.utils))

;; Every rule is a function of two arguments, a cell and a world. If the rule
;; fires, it returns a new cell, which should have the same values for :x and
;; :y as the old cell. Anything else can be modified.
;;
;; While any function of two arguments can be used as a rule, a special high
;; level rule language is provided by the `mw-parser` package, which compiles
;; rules expressed in a subset of English rules into suitable functions.
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
;; Each time the world is transformed (see `transform-world`, for each cell, 
;; rules are applied in turn until one matches. Once one rule has matched no
;; further rules can be applied.


(defn apply-rule 
  "Apply a single rule to a cell. What this is about is that I want to be able,
   for debugging purposes, to tag a cell with the rule text of the rule which
   fired (and especially so when an exception is thrown. So a rule may be either
   an ifn, or a list (ifn source-text). This function deals with despatching
   on those two possibilities."
  ([cell world rule]
   (cond
     (ifn? rule) (apply-rule cell world rule nil)
     (seq? rule) (let [[afn src] rule] (apply-rule cell world afn src))))
                  ;; {:afn afn :src src})))
     ;; (apply-rule cell world (first rule) (first (rest rule)))))
  ([cell world rule source]
    (try
      (let [result (apply rule (list cell world))]
        (cond 
          (and result source) (merge result {:rule source})
          true result))
      (catch Exception e
        (merge cell {:error (format "%s at generation %d when in state %s"
                           (.getMessage e)
                           (:generation cell)
                           (:state cell))
                     :error-rule source})))))

(defn- apply-rules
  "Derive a cell from this cell of this world by applying these rules."
  [cell world rules]
  (cond (empty? rules) cell
    true (let [result (apply-rule cell world (first rules))]
           (cond result result
             true (apply-rules cell world (rest rules))))))

(defn- transform-cell
  "Derive a cell from this cell of this world by applying these rules. If an
   exception is thrown, cache its message on the cell and set state to error"
  [cell world rules]
  (try
    (merge 
      (apply-rules cell world rules) 
      {:generation (+ (or (:generation cell) 0) 1)})
    (catch Exception e 
      (merge cell {:error 
                   (format "%s at generation %d when in state %s"
                           (.getMessage e)
                           (:generation cell)
                           (:state cell))})))) 
 
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
    (take generations (iterate transform-world-state state))))
