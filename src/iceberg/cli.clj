(ns iceberg.cli
  "Command-line interface."
  (:require [clojure.tools.cli :as clj-cli]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure.core.strint :refer [<<]]
            [iceberg.glacier :as glacier]
            [iceberg.file :as file]
            [iceberg.config :as config]
            [iceberg.log :as log]
            [iceberg.util :as util])
  (:gen-class))

(declare cli-options run-cli exit error-msg usage list-vaults
         log-error format-error gen-config)

(def cli-options
  [["-h" "--help" "Display usage information"]])

(defn run [args]
  (let [{:keys [options arguments errors summary]}
          (clj-cli/parse-opts args cli-options)
        config (config/get-config)
        command (first arguments)
        usage (usage summary)]
    ;; Check for errors.
    (cond
      (:help options) (exit 0 usage)
      errors (exit 1 (error-msg errors))
      (empty? arguments) (exit 1 usage))
    ;; Gen-config, early exit.
    (when (= "gen-config" command)
      (gen-config (rest arguments) options config)
      (exit 0))
    ;; Check config consistency.
    (let [config-errors (config/validate-config config)]
      (when config-errors 
        (exit 1 (str "Configuration error: " config-errors))))
    ;; Add additional configuration info.
    (let [config (assoc config :command-line (string/join \space args)
                               :interactive true)]
      ;; Initiate logging.
      (log/init! config)
      ;; Run program.
      (case command
        "list-vaults" (list-vaults (rest arguments) options config)
        (exit 1 (<< "No such command `~{command}`"))))))

(defn exit 
  ([status]
    (exit status ""))
  ([status msg]
    (when msg
      (log/write msg))
    (System/exit status)))

(defn error-msg [errors]
  (str "Error: invalid usage. See `iceberg -h` for help."
       (string/join \newline errors)))

(defn usage [options-summary]
  (->> ["Iceberg: Amazon Glacier client."
        ""
        "Usage: iceberg command [options]"
        ""
        "Commands:"
        "  list-vaults    List the vaults in your account"
        ""
        "Options:"
        options-summary]
        (string/join \newline)))

(defn list-vaults [arguments options config]
  (try
    (let [response (glacier/list-vaults config)
          body (glacier/deserialize-body response)
          vaults (-> body :body :VaultList)]
      (log/write (format "Listing %d vault(s)" (count vaults)))
      (doseq [[i v] (map vector (range) vaults)]
        (log/write (format "  %d. %s (%d archives, %d bytes as of %s)"
                           (inc i)
                           (:VaultName v)
                           (:NumberOfArchives v)
                           (:SizeInBytes v)
                           (or (:LastInventoryDate v) "creation")))))
    (catch clojure.lang.ExceptionInfo e
      (log-error e)
      (exit 1 nil))))

(defn gen-config [arguments options config]
  (try
    (let [f config/config-template-location]
      (if (file/exists? f)
        (println (format "Not overwriting existing file at %s." f))
        (do 
          (spit config/config-template-location
                config/config-stub)
          (println (format "Created template config in %s." f)))))
    (catch java.io.IOException e
      (log-error e)
      (exit 1 nil))))

(defn log-error [^clojure.lang.ExceptionInfo e]
  (log/write (format-error e)))

(defn format-error [^clojure.lang.ExceptionInfo e]
  (let [response (:object (ex-data e))
        body (:body (glacier/deserialize-body response))]
    (->> [""
          (str "Error HTTP/" (:status response) " Amazon/" (:code body))
          (log/pad-lines 6 (:message body))]
         (string/join \newline))))

