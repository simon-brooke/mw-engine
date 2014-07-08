;; Functions to apply a heightmap to a world.
;;
;; Heightmaps are considered only as greyscale images, so colour is redundent (will be
;; ignored). Darker shades are higher.

(ns mw-engine.heightmap
  (:import [java.awt.image BufferedImage])
  (:use mw-engine.utils
        ;; interestingly the imagez load-image is failing for me, while the
        ;; collage version is problem free.
        [mikera.image.core :only [filter-image get-pixels]]
        [mikera.image.filters]
        [fivetonine.collage.util]
        ))

(defn- abs 
  "Surprisingly, Clojure doesn't seem to have an abs function, or else I've 
   missed it. So here's one of my own. Maps natural numbers onto themselves,
   and negative integers onto natural numbers. Also maps negative real numbers
   onto positive real numbers.

   * `n` a number, on the set of real numbers."
  [n]
  (cond (< n 0) (- 0 n) true n))

(defn transform-altitude
  "Set the altitude of this cell from the corresponding pixel of this heightmap.
   If the heightmap you supply is smaller than the world, this will break.

   * `cell` a cell, as discussed in world.clj, q.v. Alternatively, a map;
   * `heightmap` an (ideally) greyscale image, whose x and y dimensions should
     exceed those of the world of which the `cell` forms part."
  [cell heightmap]
  (merge cell
         {:altitude
          (+ (get-int cell :altitude)
           (- 256
              (abs
               (mod
                (.getRGB heightmap
                         (get-int cell :x)
                         (get-int cell :y)) 256))))}))

(defn- apply-heightmap-row
  "Set the altitude of each cell in this sequence from the corresponding pixel 
   of this heightmap.
   If the heightmap you supply is smaller than the world, this will break.

   * `row` a row in a world, as discussed in world.clj, q.v. Alternatively, a
     sequence of maps;
   * `heightmap` an (ideally) greyscale image, whose x and y dimensions should
     exceed those of the world of which the `cell` forms part."  
  [row heightmap]
  (apply vector (map #(transform-altitude % heightmap) row)))

(defn apply-heightmap
  "Apply the image file loaded from this path to this world, and return a world whose
  altitudes are modified (added to) by the altitudes in the heightmap. It is assumed that
  the heightmap is at least as large in x and y dimensions as the world.

  * `world` a world, as defined in `world.clj`, q.v.;
  * `imagepath` a file path or URL which indicates an image file."
  [world imagepath]
  ;; bizarrely, the collage load-util is working for me, but the imagez version isn't.
  (let [heightmap (filter-image (grayscale)(load-image imagepath))]
    (apply vector (map #(apply-heightmap-row % heightmap) world))))
