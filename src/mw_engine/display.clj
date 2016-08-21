(ns ^{:doc "Simple functions to allow a world to be visualised."
      :author "Simon Brooke"}
  mw-engine.display
  (:require [hiccup.core :refer [html]]
            mw-engine.utils
            mw-engine.world))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; mw-engine: the state/transition engine of MicroWorld.
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

(defn format-css-class [state]
  "Format this `state`, assumed to be a keyword indicating a state in the
   world, into a CSS class"
  (subs (str state) 1))


(defn format-image-path
  "Render this `state`, assumed to be a keyword indicating a state in the
   world, into a path which should recover the corresponding image file."
  [state]
  (format "img/tiles/%s.png" (format-css-class state)))


(defn format-mouseover [cell]
  (str cell))


(defn render-cell
  "Render this world cell as a Hiccup table cell."
  [cell]
  (let [state (:state cell)]
    [:td {:class (format-css-class state) :title (format-mouseover cell)}
     [:a {:href (format "inspect?x=%d&y=%d" (:x cell) (:y cell))}
      [:img {:alt (:state cell) :width 32 :height 32 :src (format-image-path state)}]]]))


(defn render-world-row
  "Render this world `row` as a Hiccup table row."
  [row]
  (apply vector (cons :tr (map render-cell row))))


(defn render-world-table
  "Render this `world` as a Hiccup table."
  [world]
  (apply vector
    (cons :table
      (map render-world-row world))))
