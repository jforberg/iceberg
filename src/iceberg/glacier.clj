(ns iceberg.glacier
  (:require [clj-http.client :as client]
            [clojure.string  :as string])
  (:use [clojure.algo.generic.functor :only (fmap)])
  (:import (java.net URI URLEncoder)))

(declare glacier-version glacier-endpoints canonical-req canonical-path 
         canonical-query uri-encode uri-escape)

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

(defn canonical-req [req]
  (string/join "\n"
    [(string/upper-case (name (get req :method)))
     (canonical-path (get req :uri))
     (canonical-query (get req :uri))]))

(defn canonical-path [uri]
  (let [path (-> (URI. uri)
               (.normalize)
               (.getPath))]
    (if (string/blank? path)
      "/"
      (string/join "/" (map uri-encode (string/split "/" path))))))

(defn canoncial-query [uri]
  ; To be implemented
  "")

(defn uri-encode [s]
  (let [unreserved #"[\\041-\\05A\\061-\\07A0-9-_.~]"] ; Only US-ascii and some symbols
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



