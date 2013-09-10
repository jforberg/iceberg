(ns iceberg.glacier
  (:require [iceberg.util :as util]
            [clojure.string :as string]
            [clj-http.client :as client]))

(declare glacier-version glacier-endpoints)

(def glacier-version  "2012-06-01")

(def glacier-endpoints
  (util/valmap #(string/replace "glacier.*.amazonaws.com" "*" %)
    ; from http://docs.aws.amazon.com/general/latest/gr/rande.html#glacier_region
    {:virginia   "us-east-1"
     :oregon     "us-west-2"
     :california "us-west-1"
     :ireland    "eu-west-1"
     :tokyo      "ap-northeast-1"}))

(defrecord vault [account region name]) 

