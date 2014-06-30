(ns mw-engine.core
  (:use mw-engine.world
        mw-engine.utils))

;; every rule is a function of two arguments, a cell and a world. If the rule
;; fires, it returns a new cell, which should have the same values for :x and
;; :y as the old cell. Anything else can be modified.
;;
;; Rules are applied in turn until one matches.


(def treeline 10)

(def natural-rules
  (list 
    ;; Randomly, birds plant tree seeds into pasture.
    (fn [cell world] (cond (and (= (:state cell) :pasture)(< (rand 10) 1))(merge cell {:state :scrub})))
    ;; Scrub below the treeline grows gradually into forest
    (fn [cell world] 
      (cond (and 
              (= (:state cell) :scrub)
              (< (:altitude cell) treeline)) 
        (merge cell {:state :scrub2})))
    (fn [cell world] (cond (= (:state cell) :scrub2) (merge cell {:state :forest})))
    ;; Forest on fertile land at low altitude grows to climax
    (fn [cell world] 
      (cond 
        (and 
          (= (:state cell) :forest) 
          (> (:fertility cell) 10)) 
        (merge cell {:state :climax})))
    ;; Climax forest occasionally catches fire (e.g. lightning strikes)
    (fn [cell world] (cond (and (= (:state cell) :climax)(< (rand 10) 1)) (merge cell {:state :fire})))
    ;; Climax forest neighbouring fires is likely to catch fire
    (fn [cell world]
      (cond 
        (and (= (:state cell) :climax)
             (< (rand 3) 1)
             (not (empty? (get-neighbours-with-state world (:x cell) (:y cell) 1 :fire))))
        (merge cell {:state :fire})))
    ;; After fire we get waste
    (fn [cell world] (cond (= (:state cell) :fire) (merge cell {:state :waste})))
    ;; And after waste we get pioneer species; if there's a woodland seed 
    ;; source, it's going to be scrub, otherwise grassland.
    (fn [cell world]
      (cond
        (and (= (:state cell) :waste)
             (not 
               (empty? 
                 (flatten 
                   (list 
                     (get-neighbours-with-state world (:x cell) (:y cell) 1 :scrub2)
                     (get-neighbours-with-state world (:x cell) (:y cell) 1 :forest)
                     (get-neighbours-with-state world (:x cell) (:y cell) 1 :climax))))))
        (merge cell {:state :scrub})))
    (fn [cell world]
      (cond (= (:state cell) :waste)
        (merge cell {:state :pasture})))
    ;; Forest increases soil fertility
    (fn [cell world]
      (cond (member? (:state cell) '(:forest :climax))
        (merge cell {:fertility (+ (:fertility cell) 1)})))
  ))

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
