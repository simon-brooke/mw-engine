;; Functions to apply a heightmap to a world.
;;
;; Heightmaps are considered only as greyscale images, so colour is redundent (will be
;; ignored). Darker shades are higher.

(ns mw-engine.heightmap
  (:import [java.awt.image BufferedImage])
  (:use mw-engine.utils
        mw-engine.world)
  (:require [fivetonine.collage.util :as collage :only [load-image]]
            [mikera.image.core :as imagez :only [filter-image get-pixels]]
            [mikera.image.filters :as filters]))

(defn- abs 
  "Surprisingly, Clojure doesn't seem to have an abs function, or else I've 
   missed it. So here's one of my own. Maps natural numbers onto themselves,
   and negative integers onto natural numbers. Also maps negative real numbers
   onto positive real numbers.

   * `n` a number, on the set of real numbers."
  [n]
  (cond (< n 0) (- 0 n) true n))

(defn tag-gradient
  "Set the `gradient` property of this `cell` of this `world` to the difference in
   altitude between its highest and lowest neghbours."
  [world cell]
  (let [heights (remove nil? (map #(:altitude %) (get-neighbours world cell)))
        highest (cond (empty? heights) 0 ;; shouldn't happen
                  true (apply max heights))
        lowest (cond (empty? heights) 0 ;; shouldn't
                 true (apply min heights))
        gradient (- highest lowest)]
    (merge cell {:gradient gradient})))

(defn tag-gradients 
  "Set the `gradient` property of each cell in this `world` to the difference in
   altitude between its highest and lowest neghbours."
  [world]
  (map-world world tag-gradient))

(defn tag-altitude
  "Set the altitude of this cell from the corresponding pixel of this heightmap.
   If the heightmap you supply is smaller than the world, this will break.

   * `world` not actually used, but present to enable this function to be
     passed as an argument to `mw-engine.utils/map-world`, q.v.
   * `cell` a cell, as discussed in world.clj, q.v. Alternatively, a map;
   * `heightmap` an (ideally) greyscale image, whose x and y dimensions should
     exceed those of the world of which the `cell` forms part."
  ([world cell heightmap]
    (tag-altitude cell heightmap))
  ([cell heightmap]
    (merge cell
           {:altitude
            (+ (get-int cell :altitude)
               (- 256
                  (abs
                    (mod
                      (.getRGB heightmap
                        (get-int cell :x)
                        (get-int cell :y)) 256))))})))

(defn apply-heightmap
  "Apply the image file loaded from this path to this world, and return a world whose
  altitudes are modified (added to) by the altitudes in the heightmap. It is assumed that
  the heightmap is at least as large in x and y dimensions as the world.

  * `world` a world, as defined in `world.clj`, q.v.; if world is not supplied,
    a world the size of the heightmap will be created.
  * `imagepath` a file path or URL which indicates an image file."
  ([world imagepath]
    (let [heightmap (imagez/filter-image 
                      (filters/grayscale)
                      (collage/load-image imagepath))]
      (map-world
        (map-world world tag-altitude (list heightmap))
        tag-gradient)))
   ([imagepath]
    (let [heightmap (imagez/filter-image 
                      (filters/grayscale)
                      (collage/load-image imagepath))
          world (make-world (.getWidth heightmap) (.getHeight heightmap))]
      (map-world 
        (map-world world tag-altitude (list heightmap)) 
        tag-gradient))))
