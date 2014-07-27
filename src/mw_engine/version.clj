(ns mw-engine.version
  (:gen-class))

(defn get-implementation-version 
  "Get the implementation version from the package of this namespace, which must
   be compiled into a class (see clojure.java.interop)"
  [namespace-class]
  (.getImplementationVersion (.getPackage namespace-class)))

(defn -main []
  (get-implementation-version (eval 'mw-engine.version)))
