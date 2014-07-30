(ns mw-engine.version
  (:gen-class))

(defn get-implementation-version 
  "Get the implementation version from the package of this namespace, which must
   be compiled into a class (see clojure.java.interop). See 
   http://stackoverflow.com/questions/12599889/how-to-get-runtime-access-to-version-number-of-a-running-clojure-application
   TODO: doesn't work yet."
  []
  (try 
    (.getImplementationVersion (.getPackage (eval 'mw-engine.version)))
    (catch Exception any "Unknown")
    ))

(defn -main []
  (get-implementation-version ))
