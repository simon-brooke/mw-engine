(ns ^{:doc "Functions to apply a heightmap to a world."
      :author "Simon Brooke"}
  microworld.engine.heightmap
  (:import [java.awt.image BufferedImage])
  (:require [fivetonine.collage.util :as collage :only [load-image]]
            [mikera.image.core :as imagez :only [filter-image get-pixels]]
            [mikera.image.filters :as filters]
            [microworld.engine.utils :refer [abs get-int get-neighbours map-world]]
            [microworld.engine.world :refer [make-world]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; microworld.engine: the state/transition engine of MicroWorld.
;;;;
;;;; This program is free software; you can redistribute it and/or
;;;; modify it under the terms of the GNU General Public License
;;;; as published by the Free Software Foundation; either version 2
;;;; of the License, or (at your option) any later version.
;;;;
;;;; This program is distributed in the hope that it will be useful,
;;;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;;;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;;; GNU General Public License for more details.
;;;;
;;;; You should have received a copy of the GNU General Public License
;;;; along with this program; if not, write to the Free Software
;;;; Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
;;;; USA.
;;;;
;;;; Copyright (C) 2014 Simon Brooke
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; Heightmaps are considered only as greyscale images, so colour is redundent
;;;; (will be ignored). Darker shades are higher.
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn tag-property
  "Set the value of this `property` of this cell from the corresponding pixel of this `heightmap`.
   If the heightmap you supply is smaller than the world, this will break.

   * `world` not actually used, but present to enable this function to be
     passed as an argument to `microworld.engine.utils/map-world`, q.v.
   * `cell` a cell, as discussed in world.clj, q.v. Alternatively, a map;
   * `property` the property (normally a keyword) whose value will be set on the cell.
   * `heightmap` an (ideally) greyscale image, whose x and y dimensions should
     exceed those of the world of which the `cell` forms part."
  ([world cell property heightmap]
    (tag-property cell property heightmap))
  ([cell property heightmap]
    (merge cell
           {property
            (+ (get-int cell property)
               (- 256
                  (abs
                    (mod
                      (.getRGB heightmap
                        (get-int cell :x)
                        (get-int cell :y)) 256))))})))


(defn tag-gradient
  "Set the `gradient` property of this `cell` of this `world` to the difference in
   altitude between its highest and lowest neghbours."
  [world cell]
  (let [heights (remove nil? (map :altitude (get-neighbours world cell)))
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
     passed as an argument to `microworld.engine.utils/map-world`, q.v.;
   * `cell` a cell, as discussed in world.clj, q.v. Alternatively, a map;
   * `heightmap` an (ideally) greyscale image, whose x and y dimensions should
     exceed those of the world of which the `cell` forms part."
  ([world cell heightmap]
    (tag-property cell :altitude heightmap))
  ([cell heightmap]
    (tag-property cell :altitude heightmap)))


(defn apply-heightmap
  "Apply the image file loaded from this path to this world, and return a world whose
  altitudes are modified (added to) by the altitudes in the heightmap. It is assumed that
  the heightmap is at least as large in x and y dimensions as the world. Note that, in
  addition to setting the `:altitude` of each cell, this function also sets the `:gradient`.

  * `world` a world, as defined in `world.clj`, q.v.; if world is not supplied,
    a world the size of the heightmap will be created;
  * `imagepath` a file path or URL which indicates an (ideally greyscale) image file."
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


(defn apply-valuemap
  "Generalised from apply-heightmap, set an arbitrary property on each cell
   of this `world` from the values in this (ideally greyscale) heightmap.

   * `world` a world, as defined in `world.clj`, q.v.;
   * `imagepath` a file path or URL which indicates an (ideally greyscale) image file;
   * `property` the property of each cell whose value should be added to from the
      intensity of the corresponding cell of the image."
  [world imagepath property]
    (let [heightmap (imagez/filter-image
                      (filters/grayscale)
                      (collage/load-image imagepath))]
      (map-world world tag-property (list property heightmap))))
