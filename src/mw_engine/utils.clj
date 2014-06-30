(ns mw-engine.utils)

(defn member? 
  "True if elt is a member of col."
  [elt col] (some #(= elt %) col)) 