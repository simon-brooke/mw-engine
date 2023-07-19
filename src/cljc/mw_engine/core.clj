(ns ^{:doc "Functions to transform a world and run rules.
            
            Every rule is a function of two arguments, a cell and a world. If the rule
            fires, it returns a new cell, which should have the same values for `:x` and
            `:y` as the old cell. Anything else can be modified.

            While any function of two arguments can be used as a rule, a special high
            level rule language is provided by the `mw-parser` package, which compiles
            rules expressed in a subset of English rules into suitable functions.

            A cell is a map containing at least values for the keys :x, :y, and :state;
            a transformation should not alter the values of :x or :y, and should not
            return a cell without a keyword as the value of :state. Anything else is
            legal.

            A world is a two dimensional matrix (sequence of sequences) of cells, such
            that every cell's `:x` and `:y` properties reflect its place in the matrix.
            See `world.clj`.

            Each time the world is transformed (see `transform-world`), for each cell,
            rules are applied in turn until one matches. Once one rule has matched no
            further rules can be applied to that cell."
      :author "Simon Brooke"}
 mw-engine.core
  (:require [clojure.string :refer [starts-with?]]
            [mw-engine.flow :refer [flow-world]]
            [mw-engine.utils :refer [get-int-or-zero map-world rule-type]]
            [taoensso.timbre :as l]))

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

(defn apply-rule
  "Apply a single `rule` to a `cell`. What this is about is that I want to be able,
   for debugging purposes, to tag a cell with the rule text of the rule which
   fired (and especially so when an exception is thrown). "
  ;; as of version 0-3-0, metadata for rules is now passed around on the metadata
  ;; of the rule function itself. Yes, I know, this is obvious; but I'll confess
  ;; I didn't think of it before.
  [world cell rule]
  (let [result (apply rule (list cell world))]
    (when result
      (merge result (meta rule)))))

(defn- apply-rules
  "Derive a cell from this `cell` of this `world` by applying these `rules`."
  [world cell rules]
  (or
   (first (remove nil? (map #(apply-rule world cell %) rules)))
   cell))

(defn- transform-cell
  "Derive a cell from this `cell` of this `world` by applying these `rules`. If an
   exception is thrown, cache its message on the cell and set it's state to error"
  [world cell rules]
  (try
    (merge
     (apply-rules world cell rules)
     {:generation (+ (get-int-or-zero cell :generation) 1)})
    (catch Exception e
      (let [narrative (format "%s with message `%s` at generation %d when in state %s"
                              (-> e .getClass .getName)
                              (.getMessage e)
                              (:generation cell)
                              (:state cell))]
        (l/warn e narrative)
      (merge cell {:error narrative 
                   :stacktrace ;; (remove #(starts-with? % "clojure.") 
                                       (map #(.toString %) (.getStackTrace e))
                               ;;)
                   :state :error})))))

(defn transform-world
  "Return a world derived from this `world` by applying the production rules 
  found among these `rules` to each cell."
  [world rules]
  (map-world world transform-cell
             ;; Yes, that `list` is there for a reason! 
             (list
              (filter
               #(= :production (rule-type %))
               rules))))

(defn run-world
  "Run this world with these rules for this number of generations.

   * `world` a world as discussed above;
   * `init-rules` a sequence of rules as defined above, to be run once to initialise the world;
   * `rules` a sequence of rules as defined above, to be run iteratively for each generation;
   * `generations` an (integer) number of generations.
   
   **NOTE THAT** all rules **must** be tagged with `rule-type` metadata, or thet **will not**
   be executed.

   Return the final generation of the world."
  ([world rules generations]
   (run-world world rules rules (dec generations)))
  ([world init-rules rules generations]
   (reduce (fn [world iteration]
             (l/info "Running iteration " iteration)
             (let [w' (transform-world world rules)]
               (flow-world w' rules)))
           (transform-world world init-rules)
           (range generations))))
