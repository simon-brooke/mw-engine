(defproject mw-engine "0.1.2-SNAPSHOT"
  :description "Cellular automaton world builder."
  :url "http://www.journeyman.cc/microworld/"
  :manifest {
             "build-signature-version" "unset"
             "build-signature-user" "unset"
             "build-signature-email" "unset"
             "build-signature-timestamp" "unset"
             "Implementation-Version" "unset"
             }

  :license {:name "GNU General Public License v2"
            :url "http://www.gnu.org/licenses/gpl-2.0.html"}
  :plugins [[lein-marginalia "0.7.1"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/math.combinatorics "0.0.7"]
                 [org.clojure/tools.trace "0.7.8"]
                 [net.mikera/imagez "0.3.1"]
                 [fivetonine/collage "0.2.0"]])
