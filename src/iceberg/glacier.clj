(ns iceberg.glacier
  (:require [iceberg.util :as util]
            [iceberg.auth :as auth]
            [clojure.string :as string]
            [clj-http.client :as http])
  (:import [java.util Date]))

(declare glacier-version glacier-endpoints list-vaults-req create-req
         process-req validate-req)

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
               :uri (util/uri (:number acct) "vaults")}))

(defn perform-req [req]
  (http/request))

(defn create-req [acct base-req]
  (validate-req
    (auth/sign-request 
      (process-req
        (util/rec-merge
          {:server-name ((:region acct) glacier-endpoints)
           :headers
           :date (Date.)
              {:x-amz-glacier-version glacier-version}
           :body ""}
          base-req)
        (:apikey acct)))))

(defn process-req [req]
  (-> req
    (assoc :url (str (name (:scheme)) 
                     "://"
                     (:server-name req)
                     (:server-port req)
                     (:uri req)
                     (:query-string req)))
    (assoc :headers
           {:x-amz-date (auth/iso8601-datetime (:date req))
            :content-length (or (:content-length req)
                                (count (util/utf8-encode (:body req))))})))
  
; (let [content-length (if (nil? (:body req))
;                         0
;                        (count (util/utf8-encode (:body req))))]

(defn validate-req [req]
  req)

