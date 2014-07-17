;; A set of MicroWorld rules describing a simplified natural ecosystem.
;;
;; Since the completion of the rule language this is more or less obsolete - 
;; there are still a few things that you can do with rules written in Clojure
;; that you can't do in the rule language, but not many and I doubt they're
;; important.

(ns mw-engine.natural-rules
  (:use mw-engine.utils
        mw-engine.world))

;; treeline at arbitrary altitude.
(def treeline 150)

;; waterline also at arbitrary altitude.
(def waterline 10)

;; and finally snowline is also arbitrary.
(def snowline 200)

;; Rare chance of lightning strikes
(def lightning-probability 500)

;; rules describing vegetation
(def vegetation-rules
  (list
    ;; Randomly, birds plant tree seeds into grassland.
    (fn [cell world] (cond (and (= (:state cell) :grassland)(< (rand 10) 1))(merge cell {:state :heath})))
    ;; heath below the treeline grows gradually into forest, providing browsing pressure is not to high
    (fn [cell world]
      (cond (and
              (= (:state cell) :heath)
              ;; browsing limit really ought to vary with soil fertility, but...
              (< (+ (get-int cell :deer)(get-int cell :sheep)) 6)
              (< (get-int cell :altitude) treeline))
        (merge cell {:state :scrub})))
    (fn [cell world] (cond (= (:state cell) :scrub) (merge cell {:state :forest})))
    ;; Forest on fertile land grows to climax
    (fn [cell world]
      (cond
        (and
          (= (:state cell) :forest)
          (> (get-int cell :fertility) 10))
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
    ;; source, it's going to be heath, otherwise grassland.
    (fn [cell world]
      (cond
        (and (= (:state cell) :waste)
             (not
               (empty?
                 (flatten
                   (list
                     (get-neighbours-with-state world (:x cell) (:y cell) 1 :scrub)
                     (get-neighbours-with-state world (:x cell) (:y cell) 1 :forest)
                     (get-neighbours-with-state world (:x cell) (:y cell) 1 :climax))))))
        (merge cell {:state :heath})))
    (fn [cell world]
      (cond (= (:state cell) :waste)
        (merge cell {:state :grassland})))
    ;; Forest increases soil fertility
    (fn [cell world]
      (cond (member? (:state cell) '(:forest :climax))
        (merge cell {:fertility (+ (get-int cell :fertility) 1)})))
  ))

;; rules describing herbivore behaviour
(def herbivore-rules
  (list
    ;; if there are too many deer for the fertility of the area to sustain,
    ;; some die or move on.
    (fn [cell world]
      (cond (> (get-int cell :deer) (get-int cell :fertility))
        (merge cell {:deer (get-int cell :fertility)})))
    ;; deer arrive occasionally at the edge of the map.
    (fn [cell world]
      (cond (and (< (count (get-neighbours world cell)) 8)
                 (< (rand 50) 1)
                 (> (get-int cell :fertility) 0)
                 (= (get-int cell :deer) 0))
        (merge cell {:deer 2})))
    ;; deer gradually spread through the world by breeding or migrating.
    (fn [cell world]
      (let [n (apply + (map #(get-int % :deer) (get-neighbours world cell)))]
        (cond (and
                (> (get-int cell :fertility) 0)
                (= (get-int cell :deer) 0)
                (>= n 2))
          (merge cell {:deer (int (/ n 2))}))))
    ;; deer breed.
    (fn [cell world]
      (cond
        (>= (get-int cell :deer) 2)
        (merge cell {:deer (int (* (:deer cell) 2))})))))

  ;; rules describing predator behaviour
  (def predator-rules
    (list
     ;; wolves eat deer
     (fn [cell world]
      (cond
       (>= (get-int cell :wolves) 1)
       (merge cell {:deer (max 0 (- (get-int cell :deer) (get-int cell :wolves)))})))
;;      ;; not more than eight wolves in a pack, for now (hack because wolves are not dying)
;;      (fn [cell world]
;;        (cond (> (get-int cell :wolves) 8) (merge cell {:wolves 8})))
    ;; if there are not enough deer to sustain the get-int of wolves,
    ;; some wolves die or move on. (doesn't seem to be working?)
    (fn [cell world]
       (cond (> (get-int cell :wolves) (get-int cell :deer))
         (merge cell {:wolves 0})))
    ;; wolves arrive occasionally at the edge of the map.
    (fn [cell world]
      (cond (and (< (count (get-neighbours world cell)) 8)
                 (< (rand 50) 1)
                 (not (= (:state cell) :water))
                 (= (get-int cell :wolves) 0))
        (merge cell {:wolves 2})))
    ;; wolves gradually spread through the world by breeding or migrating.
    (fn [cell world]
      (let [n (apply + (map #(get-int % :wolves) (get-neighbours world cell)))]
        (cond (and
                (not (= (:state cell) :water))
                (= (get-int cell :wolves) 0)
                (>= n 2))
          (merge cell {:wolves 2}))))
    ;; wolves breed.
    (fn [cell world]
      (cond
        (>= (get-int cell :wolves) 2)
        (merge cell {:wolves (int (* (:wolves cell) 2))})))
    ))

  ;; rules which initialise the world
  (def init-rules
    (list
     ;; below the waterline, we have water.
     (fn [cell world]
       (cond (and (= (:state cell) :new) (< (get-int cell :altitude) waterline)) (merge cell {:state :water})))
     ;; above the snowline, we have snow.
     (fn [cell world]
       (cond (and (= (:state cell) :new) (> (get-int cell :altitude) snowline)) (merge cell {:state :snow})))
     ;; in between, we have a wasteland.
     (fn [cell world] (cond (= (:state cell) :new) (merge cell {:state :grassland}))
     )))

(def natural-rules (flatten
                    (list
                     vegetation-rules
                     herbivore-rules
                     ;; predator-rules
                     )))
