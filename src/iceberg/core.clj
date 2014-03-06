(ns iceberg.core
  "A cross-platform backup tool targeting Amazon's Glacier service"
  (:require [iceberg.cli :as cli]
            [iceberg.glacier :as glacier]))

(def ^:dynamic *config* nil)

(defn -main [& args]
  (cli/run args))

(defmacro with-config [config & more]
  `(binding [*config* ~config]
     ~@more))

(defn set-config [config]
  (def ^:dynamic *config* config))

(defn list-vaults []
  (glacier/list-vaults *config*))
