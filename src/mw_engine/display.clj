(ns mw-engine.display
  (:use mw-engine.utils
        mw-engine.world)
  (:require [hiccup.core :refer [html]]))

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
      (map render-world-row world)))
