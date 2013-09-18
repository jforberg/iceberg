(ns iceberg.glacier
  (:require [iceberg.util :as util]
            [iceberg.auth :as auth]
            [clojure.string :as string]
            [clj-http.client :as http])
  (:import [java.util Date]))

(declare glacier-version glacier-endpoints list-vaults-req create-req validate-req)

(def glacier-version  "2012-06-01")

(def glacier-endpoints
  (util/valmap #(string/replace "glacier.*.amazonaws.com" "*" %)
    ; from http://docs.aws.amazon.com/general/latest/gr/rande.html#glacier_region
    {:virginia   "us-east-1"
     :oregon     "us-west-2"
     :california "us-west-1"
     :ireland    "eu-west-1"
     :tokyo      "ap-northeast-1"}))

(defn list-vaults-req [acct]
  (create-req acct
              {:method :get
               :uri (util/uri (:id acct) "vaults")}))

(defn perform-req [req]
  (http/request))

(defn create-req [acct base-req]
  (let [content-length (if (nil? (:body base-req))
                         0
                         (count (util/utf8-encode (:body base-req))))]
    (validate-req
      (auth/sign-request 
        (util/rec-merge
          {:server-name ((:region acct) glacier-endpoints)
           :date (Date.)
           :headers
             {:content-length content-length
              :x-amz-glacier-version glacier-version}
           :body ""}
          base-req)
        (:apikey acct)))))

(defn validate-req [req]
  req)

