;;;; Totally boring code to handle Amazon's crazy signing system.

(ns iceberg.auth
  (:require [iceberg.util :as util]
            [iceberg.glacier :as glacier]
            [clojure.string :as string])
  (:import [java.util Date]))

(declare signed-request auth-header signature signing-key string-to-sign
         req-hash canonical-request canonical-path canonical-query
         canonical-headers signed-headers rewrite-headers parse-query
         uri-encode uri-escape cred-scope extract-region iso8601-datetime
         iso8601-date)

;;; The only interesting function in this module. Takes a request, does all 
;;; the stuff needed to sign it, and puts the signature in as a header.
(defn signed-request [req sec-key]
  (assoc req
         :headers
         (merge (req :headers)
                {:authorization (auth-header req sec-key)})))

;;; The rest is just helper functions for `signed-request`.

(defn auth-header [req sec-key]
  (str
    "AWS4-HMAC-SHA256 "
    "Credential="
    (cred-scope req sec-key)
    ", SignedHeaders="
    (string/join ";" (signed-headers req))
    ", Signature="
    (signature req sec-key)))

(defn signature [req sec-key]
  (util/hex-enc 
    (util/sha256mac (signing-key req sec-key) (string-to-sign req))))

(defn signing-key [req sec-key]
  (let [mac util/sha256mac]
    (mac
      (mac
        (mac
          (mac 
            (str "AWS4" sec-key (iso8601-date (req :date))))
          (extract-region (req :server-name)))
        "glacier")
      "aws4_request")))

(defn string-to-sign [req]
  (string/join "\n"
               ["AWS4-HMAC-SHA256"
                (iso8601-datetime (req :date))
                (cred-scope req)
                (util/sha256 (canonical-request req))]))

(defn canonical-request [req]
  (string/join "\n"
    [(string/upper-case (name (req :request-method)))
     (canonical-path (req :uri))
     (canonical-query (req :query-string))
     (canonical-headers req)
     ""
     (string/join ";" (signed-headers req))
     (util/sha256 (req :body))]))

(defn canonical-path [uri]
  (let [encoded-parts (map uri-encode (string/split uri #"/"))]
    (if (every? empty? encoded-parts)
      "/"
      (string/join "/" encoded-parts))))

(defn canonical-query [query]
  (let [query-map (parse-query query)
        sorted-query (sort query-map)]
    (string/join "&"
                 (map (fn [[k v]] (str (uri-encode k) \= (uri-encode v)))
                      sorted-query))))

(defn canonical-headers [req]
  (if (not (contains? req :server-name))
    (throw (IllegalArgumentException. "Headers must contain a Host:"))
    (let [mod-headers (rewrite-headers req)
          sorted-headers (sort mod-headers)]
      (string/join "\n"
                   (map (fn [[k v]]
                          (str (string/lower-case (name k)) \: v))
                        sorted-headers)))))

(defn signed-headers [req]
  (sort 
    (map name (keys (rewrite-headers req)))))

(defn rewrite-headers [req]
  (merge (req :headers)
         {:host (req :server-name)
          :x-amz-date (iso8601-datetime (req :date))}))
 
(defn parse-query [query]
  (if (nil? query)
    {}
    (let [query-pairs (map #(string/split % #"=")
                            (string/split query #"&"))]
      (if (every? #(every? empty? %) query-pairs)
        {}
        (into {} query-pairs)))))

(defn uri-encode [^String s]
  (let [unreserved #"[a-zA-Z0-9-_.~%]"] ; Only US-ascii and some symbols
    ; Trust s to contain no invalid escapes already (URI catches these)
    (reduce (fn [acc c]
              (let [strc (str c)]
                (str acc
                     (if (re-matches unreserved strc)
                       strc
                       (uri-escape strc)))))
            ""
            s)))

(defn uri-escape [strc]
  (apply str 
    (map #(format "%%%02X" %)
          (.getBytes strc "UTF-8"))))

(defn cred-scope
  ([req k]
    (string/join "/"
                 k
                 (cred-scope req)))
  ([req]
    (string/join "/"
                 [(iso8601-date (req :date))
                  (extract-region (req :server-name))
                  "glacier"
                  "aws4_request"])))

(defn extract-region [host]
  (let [domains (string/split host #"\.")]
    (try
      (nth domains (- (count domains) 3))
      (catch IndexOutOfBoundsException ex
        nil))))

(defn iso8601-datetime [^Date date]
  (util/strftime "yyyyMMdd'T'HHmmss'Z'" date))

(defn iso8601-date [^Date date]
  (util/strftime "yyyyMMdd" date))
