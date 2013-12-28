(ns iceberg.glacier
  (:require [iceberg.util :as util]
            [iceberg.auth :as auth]
            [clojure.string :as string]
            [clj-http.client :as http])
  (:import [java.util Date]))

(declare glacier-version glacier-endpoints basic-request list-vaults fire-req
         create-req validate encode-body add-date set-endpoint)

(def glacier-version  "2012-06-01")

(def glacier-endpoints
  (util/valmap #(string/replace "glacier.*.amazonaws.com" "*" %)
    ;; from http://docs.aws.amazon.com/general/latest/gr/rande.html#glacier_region
    {:virginia   "us-east-1"
     :oregon     "us-west-2"
     :california "us-west-1"
     :ireland    "eu-west-1"
     :tokyo      "ap-northeast-1"}))

(def basic-request
  ;; The template is modelled on Ring request maps. Some differences.
  ;; nil values here must be overriden before the request is made.
  {:request-method nil   ; :get, :post, :put
   :server-name    nil   ; "glacier.xxx.amazonaws.com"
   :uri            nil   ; "/", "/vaults", ...
   :query-string   ""    ; Any query.
   :scheme         :http ; Temporary, should be HTTPS before we're finished.
   :headers              ; Any additional headers: signature, content-length etc.
     {:x-amz-glacier-version glacier-version} ; The version of the protocol.
   :body           ""})  ; Payload as string.

(defn list-vaults [acct]
  (create-req acct
              {:method :get
               :uri (util/uri (:number acct) "vaults")}))

(defn fire-req [req-template]
  (http/request (create-req req-template)))

(defn create-req [req-template acct]
  (-> (util/rec-merge basic-request req-template)
    (set-endpoint acct)
    (encode-body)
    (add-date)
    (auth/sign-request acct) ; No changes beyond this point!
    (validate)))

(defn validate-req [req]
  req)

(defn encode-body [req-template]
  (let [body-enc (util/utf8-encode (:body req-template))]
    (-> req-template
      (assoc :body body-enc)
      (assoc-in [:headers :content-length] (count body-enc)))))

(defn add-date [req-template]
  (assoc-in req-template [:headers :date] (auth/iso8601-datetime (Date.))))

(defn set-endpoint [req-template acct]
  (assoc req-template :server-name ((:region acct) glacier-endpoints)))

