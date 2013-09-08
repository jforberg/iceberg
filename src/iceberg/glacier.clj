(ns iceberg.glacier
  (:require [clj-http.client :as client]
            [clojure.string  :as string])
  (:use [clojure.algo.generic.functor :only (fmap)])
  (:import (java.net URI URLEncoder)))

(declare glacier-version glacier-endpoints canonical-request canonical-path 
         canonical-query parse-query uri-encode uri-escape)

(def glacier-version  "2012-06-01")

(def glacier-endpoints
  (fmap #(string/replace "glacier.*.amazonaws.com" "*" %)
    ; from http://docs.aws.amazon.com/general/latest/gr/rande.html#glacier_region
    {:virginia   "us-east-1"
     :oregon     "us-west-2"
     :california "us-west-1"
     :ireland    "eu-west-1"
     :tokyo      "ap-northeast-1"}))

(defrecord vault [account region name]) 

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

(defn uri-encode [s]
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



