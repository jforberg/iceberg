(defproject iceberg "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "BSD 2-clause License"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/algo.generic "0.1.1"]
                 [me.raynes/fs "1.4.5"]
                 [korma "0.3.0-RC5"]
                 [org.xerial/sqlite-jdbc "3.7.2"]
                 [clj-http "0.7.6"]]
  :profiles {:dev {:plugins [[lein-midje "3.1.0"]]
                   :dependencies [[midje "1.5.1"]
                                  [org.clojure/tools.trace "0.7.6"]]}}
  :main iceberg.core)
