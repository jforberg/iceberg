(ns iceberg.core
  "A cross-platform backup tool targeting Amazon's Glacier service"
  (:require [iceberg.cli :as cli]))

(defn -main [& args]
  (cli/run-cli args))
