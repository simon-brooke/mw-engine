(ns mw-engine.natural-rules
  (:use mw-engine.utils
        mw-engine.world))

;; rules describing the natural ecosystem

(def treeline 10)

;; one in fifty chance of lightning strike
(def lightning-probability 50)

;; rules describing vegetation
(def vegetation-rules
  (list 
    ;; Randomly, birds plant tree seeds into pasture.
    (fn [cell world] (cond (and (= (:state cell) :pasture)(< (rand 10) 1))(merge cell {:state :scrub})))
    ;; Scrub below the treeline grows gradually into forest, providing browsing pressure is not to high
    (fn [cell world] 
      (cond (and 
              (= (:state cell) :scrub)
              ;; browsing limit really ought to vary with soil fertility, but...
              (< (+ (population cell :deer)(or (:sheep cell) 0)) 6)
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
    (fn [cell world] (cond (and (= (:state cell) :climax)(< (rand lightning-probability) 1)) (merge cell {:state :fire})))
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

;; rules describing animal behaviour
(def predation-rules
  (list
    ;; deer arrive occasionally at the edge of the map.
    (fn [cell world]
      (cond (and (< (count (get-neighbours world cell)) 8)
                 (< (rand 50) 1)
                 (= (population cell :deer) 0))
        (merge cell {:deer 2})))
    ;; if there are too many deer for the fertility of the area to sustain, 
    ;; some die or move on.
    (fn [cell world]
      (cond (> (* (population cell :deer) 10) (:fertility cell))
        (merge cell {:deer (int (/ (:fertility cell) 10))})))
    ;; deer gradually spread through the world by breeding or migrating.
    (fn [cell world]
      (let [n (apply + (map #(population % :deer) (get-neighbours world cell)))]
        (cond (and
                (= (population cell :deer) 0)
                (>= n 2))
          (merge cell {:deer (int (/ n 2))}))))
    ;; deer breed.
    (fn [cell world]
      (cond 
        (>= (population cell :deer) 2)
        (merge cell {:deer (int (* (:deer cell) 4))})))
    ;; wolves arrive occasionally at the edge of the map.
;;    (fn [cell world]
;;      (cond (and (< (count (get-neighbours world cell)) 8)
;;                 (< (rand 50) 1)
;;                 (= (population cell :wolves) 0))
;;        (merge cell {:wolves 2})))
    ;; if there are not enough deer to sustain the population of wolves, 
    ;; some wolves die or move on.
    (fn [cell world]
      (cond (> (population cell :wolves) (population cell :deer))
        (merge cell {:wolves 0})))
    ;; wolves gradually spread through the world by breeding or migrating.
    (fn [cell world]
      (let [n (apply + (map #(population % :wolves) (get-neighbours world cell)))]
        (cond (and
                (= (population cell :wolves) 0)
                (>= n 2))
          (merge cell {:wolves 2}))))
    ;; wolves breed.
    (fn [cell world]
      (cond 
        (>= (population cell :wolves) 2)
        (merge cell {:wolves (int (* (:wolves cell) 2))})))
    ;; wolves eat deer
    (fn [cell world]
      (merge cell {:deer (- (population cell :deer) (population cell :wolves))}))
    ))

(def natural-rules (flatten (list vegetation-rules predation-rules)))
