;;;; Totally boring code to handle Amazon's crazy signing system.

(ns iceberg.auth
  (:require [iceberg.util :as util]
            [clojure.string :as string])
  (:import [java.util Date]))

(declare sign-request auth-header signature signing-key string-to-sign
         canonical-request canonical-path canonical-query canonical-headers
         signed-headers rewrite-headers parse-query uri-encode uri-escape
         cred-scope extract-region iso8601-datetime iso8601-date)

(defrecord account [number region keyid apikey])

;;; The only interesting function in this module. Takes a request, does all 
;;; the stuff needed to sign it, and puts the signature in as a header.
(defn sign-request [req acct]
  "Add the proper Authorization header to a Glacier request."
  (assoc req
         :headers
         (merge (:headers req)
                {:authorization (auth-header req acct)})))

;;; The rest is just helper functions for `signed-request`.

(defn auth-header [req acct]
  "Construct the Authorization header for a Glacier request "
  (str
    "AWS4-HMAC-SHA256 "
    "Credential="
    (cred-scope req acct)
    ", SignedHeaders="
    (string/join ";" (signed-headers req))
    ", Signature="
    (signature req acct)))

(defn signature [req acct]
  "Calculate the signature for a Glacier request."
  (util/hex-enc 
    (util/sha256mac (signing-key req acct) 
                    (string-to-sign req))))

(defn signing-key [req acct]
  "Calculate the signing-key for a request/account combination."
  (let [mac util/sha256mac]
    (mac
      (mac
        (mac
          (mac 
            (str "AWS4" (:apikey acct)) 
            (iso8601-date (:date req)))
          (or (:aws-region req) (extract-region (:server-name req))))
        (or (:aws-service req) "glacier"))
      "aws4_request")))

(defn string-to-sign [req]
  "Calculate the AWS4 `string-to-sign` for a request."
  (string/join "\n"
               ["AWS4-HMAC-SHA256"
                (iso8601-datetime (:date req))
                (cred-scope req)
                (util/sha256 (canonical-request req))]))

(defn canonical-request [req]
  "Arrange a request into AWS4 `canonical form`."
  (string/join "\n"
    [(string/upper-case (name (:request-method req)))
     (canonical-path (:uri req))
     (canonical-query (:query-string req))
     (canonical-headers req)
     ""
     (string/join ";" (signed-headers req))
     (util/sha256 (:body req))]))

(defn canonical-path [uri]
  "Arrange a uri into AWS4 `canonical form`."
  (let [encoded-parts (map uri-encode (string/split uri #"/"))]
    (if (every? empty? encoded-parts)
      "/"
      (string/join "/" encoded-parts))))

(defn canonical-query [query]
  "Arrange a query string into AWS4 `canonical form`."
  (let [query-map (parse-query query)
        sorted-query (sort query-map)]
    (string/join "&"
                 (map (fn [[k v]] (str (uri-encode k) \= (uri-encode v)))
                      sorted-query))))

(defn canonical-headers [req]
  "Arrange the set of headers from a request into AWS4 `canonical form`."
  (if (not (contains? req :server-name))
    (throw (IllegalArgumentException. "Headers must contain a Host:"))
    (let [mod-headers (rewrite-headers req)
          sorted-headers (sort mod-headers)]
      (string/join "\n"
                   (map (fn [[k v]]
                          (str (string/lower-case (name k)) \: v))
                        sorted-headers)))))

(defn signed-headers [req]
  "Get the headers to be signed for a request."
  (sort 
    (map name (keys (rewrite-headers req)))))

(defn rewrite-headers [req]
  "Rewrite the headers of a request for signing."
  (merge (:headers req)
         {:host (:server-name req)
          :x-amz-date (iso8601-datetime (req :date))}))
 
(defn parse-query [query]
  "Parse a query string into name/value pairs."
  (if (nil? query)
    {}
    (let [query-pairs (map #(string/split % #"=")
                            (string/split query #"&"))]
      (if (every? #(every? empty? %) query-pairs)
        {}
        (into {} query-pairs)))))

(defn uri-encode [^String s]
  "Escape a string according to URL/URI format."
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
  "URI-escape a character, if necessary."
  (apply str 
    (map #(format "%%%02X" %)
          (util/utf8-decode strc))))

(defn cred-scope
  "Get the `scope` of a signature."
  ([req acct]
    (string/join "/"
                 [(:keyid acct)
                  (cred-scope req)]))
  ([req]
    (string/join "/"
                 [(iso8601-date (:date req))
                  (or (:aws-region req)
                      (extract-region (:server-name req)))
                  (or (:aws-service req)
                      "glacier")
                  "aws4_request"])))

(defn extract-region [host]
  "Get the AWS `region` from a hostname."
  (let [domains (string/split host #"\.")]
    (try
      (nth domains (- (count domains) 3))
      (catch IndexOutOfBoundsException ex
        nil))))

(defn iso8601-datetime [^Date date]
  "Print a date and time in ISO 8601 basic format."
  (util/strftime "yyyyMMdd'T'HHmmss'Z'" date))

(defn iso8601-date [^Date date]
  "Print a date only in ISO 8601 basic format."
  (util/strftime "yyyyMMdd" date))
