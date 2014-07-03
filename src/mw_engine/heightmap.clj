;; Functions to apply a heightmap to a world.
;;
;; Heightmaps are considered only as greyscale images, so colour is redundent (will be
;; ignored. Darker shades are higher.

(ns mw-engine.heightmap
  (:import [java.awt.image BufferedImage])
  (:use mw-engine.utils
        ;; interestingly the imagez load-image is failing for me, while the
        ;; collage version is problem free.
        [mikera.image.core :only [filter-image get-pixels]]
        [mikera.image.filters]
        [fivetonine.collage.util]
        ))

(defn- abs [int]
  (cond (< int 0) (- 0 int) true int))

(defn transform-altitude
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
  [row heightmap]
  (apply vector (map #(transform-altitude %1 heightmap) row)))

(defn apply-heightmap
  "Apply the image file loaded from this path to this world, and return a world whose
  altitudes are modified (added to) by the altitudes in the heightmap. It is assumed that
  the heightmap is at least as large in x and y dimensions as the world, and actually will
  work correctly only if they are of the same x and y dimensions.

  * `world` a world, as defined in `world.clj`;
  * `imagepath` a file path or URL which indicates an image file."
  [world imagepath]
  ;; bizarrely, the collage load-util is working for me, but the imagez version isn't.
  (let [heightmap (filter-image (grayscale)(load-image imagepath))]
    (apply vector (map #(apply-heightmap-row %1 heightmap) world))))
