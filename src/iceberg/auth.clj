(ns iceberg.auth
  (:require [iceberg.util :as util]
            [clojure.string :as string])
  (:import [java.net URI]
           [java.util Date]))

(declare string-to-sign req-hash canonical-request canonical-path
         canonical-query canonical-headers parse-query uri-encode uri-escape
         iso8601-date)

(defn string-to-sign [req region]
  (string/join ["AWS4-HMAC-SHA256"
                (iso8601-date (get req :date))
                (str
                  (iso8601-date (get req :date)) \/
                  region \/
                  "glacier" \/
                  "aws4_request")
                (req-hash (canonical-request req))]))

(defn req-hash [req]
  nil)

(defn canonical-request [req]
  (string/join "\n"
    [(string/upper-case (name (get req :method)))
     (canonical-path (get req :uri))
     (canonical-query (get req :uri))
     (canonical-headers req)]))

(defn canonical-path [uri]
  (let [path (-> (URI. uri) (.normalize) (.getPath))
        encoded-parts (map uri-encode (string/split path #"/"))]
    (if (every? empty? encoded-parts)
      "/"
      (string/join "/" encoded-parts))))

(defn canonical-query [uri]
  (let [query-map (parse-query
                    (-> (URI. uri) (.getQuery)))
        sorted-query (sort query-map)]
    (string/join "&"
                 (map (fn [[k v]] (str (uri-encode k) \= (uri-encode v)))
                      sorted-query))))

(defn canonical-headers [header-map]
  (if (not (contains? header-map :host))
    (throw (IllegalArgumentException. "Headers must contain a Host:"))
    (let [sorted-headers (sort header-map)]
      (string/join "\n"
                   (map (fn [[k v]]
                          (str (string/lower-case (name k)) \: v))
                        header-map)))))
 
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

(defn iso8601-datetime [^Date date]
  (util/strftime "yyyyMMdd'T'HHmmss'Z'" date))

(defn iso8601-date [^Date date]
  (util/strftime "yyyyMMdd"))
