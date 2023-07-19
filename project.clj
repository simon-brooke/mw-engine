(defproject mw-engine "0.3.0-SNAPSHOT"
  :cloverage {:output "docs/cloverage"}
  :codox {:metadata {:doc "**TODO**: write docs"
                     :doc/format :markdown}
          :output-path "docs/codox"
          :source-uri "https://github.com/simon-brooke/mw-engine/blob/master/{filepath}#L{line}"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60" :scope "provided"]
                 [org.clojure/math.combinatorics "0.2.0"]
                 [org.clojure/tools.trace "0.7.11"]
                 [org.clojure/tools.namespace "1.4.4"]
                 [com.taoensso/timbre "6.2.1"]
                 [fivetonine/collage "0.3.0"]
                 [hiccup "1.0.5"]
                 [net.mikera/imagez "0.12.0"]]
  :description "Cellular automaton world builder."
  :jvm-opts ["-Xmx4g"]
  :license {:name "GNU General Public License v2"
            :url "http://www.gnu.org/licenses/gpl-2.0.html"}

  :min-lein-version "2.0.0"
  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-cloverage "1.2.2"]
            [lein-codox "0.10.8"]
            [lein-kibit "0.1.2"]
            [lein-marginalia "0.7.1"]]
  :resource-paths ["resources" "target/cljsbuild"]
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :url "http://www.journeyman.cc/microworld/")
