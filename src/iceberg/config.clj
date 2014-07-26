(ns iceberg.config
  "Configuration handling"
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [iceberg.glacier :as glacier]
            [iceberg.file :as file]))

(declare default-config config-stub config-files config-template-location
         get-config read-config validate-config)

(def default-config
  {:log-file (io/file "." ".iceberg" "iceberg.log")})

(def config-stub
  (string/join \newline  
    [";;;; Config file for Iceberg."
     "{"
     " :region :my-region  ; One of :virgina, :oregon, :california, :ireland, :tokyo."
     " :number \"00000000\"  ; Your AWS account number."
     " :keyid  \"ABCD1234\"  ; Your AWS public key ID."
     " :apikey \"ABcd1234\"  ; Your AWS secret key."
     "}"]))

(def config-files [(io/file "." ".iceberg" "iceberg.edn")
                   (io/file (System/getProperty "user.home") ".iceberg" "iceberg.edn")])

(def config-template-location (io/file (System/getProperty "user.home")
                                       ".iceberg" "iceberg.edn"))

(defn get-config []
  (let [config (first (filter (complement nil?) 
                              (map read-config config-files)))]
    (if (nil? config)
      nil
      (merge default-config config))))

(defn read-config [f]
  (try
    (with-open [rdr (java.io.PushbackReader. (io/reader f))]
      (read rdr))
    (catch java.io.IOException e)))

(defn validate-config [config]
  (if (nil? config)
    "Failed to load config.\n\nUse 'iceberg gen-config' to generate a template."
    (let [{:keys [region number keyid apikey]} config]
      (cond
        (not (contains? glacier/glacier-endpoints region))
          "Region missing or invalid"
        (not (instance? String number)) 
          "Account number missing or invalid"
        (not (number? (read-string number)))
          "Account number is not a number"
        (re-matches #"-" number) 
          "Account number must not contain dashes."
        (not (instance? String keyid)) 
          "Key ID missing or invalid."
        (not (instance? String apikey)) 
          "Key missing or invalid."
        :else 
          nil))))
