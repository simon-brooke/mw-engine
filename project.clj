(defproject mw-engine "3.0.0-SNAPSHOT"
  :description "Cellular automaton world builder."
  :url "http://www.journeyman.cc/microworld/"
  :manifest {
             "build-signature-version" "unset"
             "build-signature-user" "unset"
             "build-signature-email" "unset"
             "build-signature-timestamp" "unset"
             "Implementation-Version" "unset"
             }
  :jvm-opts ["-Xmx4g"]
  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources"]
  :license {:name "GNU General Public License v2"
            :url "http://www.gnu.org/licenses/gpl-2.0.html"}
  :plugins [[lein-marginalia "0.7.1"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.3"]
                 [org.clojure/tools.trace "0.7.8"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [hiccup "1.0.5"]
                 [net.mikera/imagez "0.11.0"]])
