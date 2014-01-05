(ns iceberg.cli
  "Command-line interface."
  (:require [clojure.tools.cli :as clj-cli]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [iceberg.glacier :as glacier]
            [iceberg.file :as file]
            [iceberg.util :as util])
  (:gen-class))

(declare cli-options default-config default-config-file config-dir config-file
         run-cli exit error-msg usage validate-config get-config list-vaults
         format-error decode-body)

(def cli-options
  [["-h" "--help" "Display usage information"]])

(def default-config
  {})

(def default-config-file
  (->> [";;;; Config file for Iceberg."
        "{"
        " :region :my-region  ; One of :virgina, :oregon, :california, :ireland, :tokyo."
        " :number \"00000000\" ; Your AWS account number."
        " :keyid  \"ABCD1234\"  ; Your AWS public key ID."
        " :apikey \"ABcd1234\"  ; Your AWS secret key."
        "}"]
       (string/join \newline)))

(def config-dir (util/uri (System/getProperty "user.home") ".iceberg"))

(def config-file (util/uri config-dir "config.clj"))

(defn run-cli [args]
  (let [{:keys [options arguments errors summary]}
          (clj-cli/parse-opts args cli-options)
        config (util/rec-merge default-config (get-config))]
    ;; Check for errors.
    (cond
      (:help options) (exit 0 (usage summary))
      (empty? arguments) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Check config consistency.
    (let [config-errors (validate-config config)]
      (when config-errors 
        (exit 1 (str "Configuration error: " config-errors))))
    ;; Run program.
    (case (first arguments)
      "list-vaults" (list-vaults (rest arguments) options config))))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn error-msg [errors]
  (str "Error: invalid usage. See `iceberg -h` for help."
       (string/join \newline errors)))

(defn usage [options-summary]
  (->> ["Iceberg: Amazon Glacier client for UNIX."
        ""
        "Usage: iceberg command [options]"
        ""
        "Options:"
        options-summary
        ""
        "Commands:"
        "  list-vaults    List the vaults in your account"]
        (string/join \newline)))

(defn validate-config [config]
  (let [{:keys [region number keyid apikey]} config]
    (cond
      (or (not (contains? glacier/glacier-endpoints region))
          (not (instance? clojure.lang.Keyword region))) "Region missing or invalid."
      (not (instance? String number)) "Account number missing or invalid."
      (re-matches #"-" number) "Account number must not contain dashes."
      (not (instance? String keyid)) "Key ID missing or invalid."
      (not (instance? String apikey)) "Key missing or invalid."
      :else nil)))

(defn get-config []
  (if (not (file/exists? config-file))
    (do (file/mkdir config-dir)
        (spit config-file default-config-file)
        (exit 1 (str "Error: No config file found. I've created one for you at " config-file)))
    (read-string (slurp config-file))))

(defn list-vaults [arguments options config]
  (try
    (let [response (glacier/list-vaults config)
          body (decode-body response)]
      (map println (:VaultList body)))
    (catch Exception e
      (exit 1 (format-error e)))))

(defn format-error [e]
  (let [response (:object (ex-data e))
        body (decode-body response)]
    (->> [(str "Error HTTP/" (:status response) " AWS/" (:code body))
          (:message body)]
         (string/join \newline))))

(defn decode-body [response]
  (json/read-str (:body response)
                 :key-fn keyword))
