(ns mw-engine.utils)

(defn member? 
  "True if elt is a member of col."
  [elt col] (some #(= elt %) col)) 

(defn population [cell species]
  "Return the population of this species in this cell.
   Species is assumed to be a keyword whose value in a cell should be an
   integer."
  (or (get cell species) 0))