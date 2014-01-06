(ns iceberg.glacier
  "The main interface to the Glacier API."
  (:require [clojure.string :as string]
            [clojure.data.json :as json]
            [clj-http.client :as http]
            [iceberg.auth :as auth]
            [iceberg.log :as log]
            [iceberg.util :as util])
  (:import [java.util Date]))

(declare glacier-version glacier-endpoints basic-request list-vaults
         list-vaults-req fire-req create-req validate deserialize-body
         encode-body add-date add-host set-endpoint stringify-headers
         log-request format-request)

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
   :query-params   {}    ; Query part of the URL.
   :scheme         :http ; Temporary, should be HTTPS before we're finished.
   :headers              ; Any additional headers: signature, content-length etc.
     {:x-amz-glacier-version glacier-version} ; The version of the protocol.
   :body           nil    ; Payload as string, byte-array or InputStream.
   :meta                 ; Any internal data not related to HTTP.
     {:date nil}})       ; Date for authentication

(defn list-vaults [acct]
  (deserialize-body
    (fire-req (list-vaults-req acct))))

(defn list-vaults-req [acct]
  (create-req {:request-method :get
               :uri (util/uri (:number acct) "vaults")}
              acct))

(defn fire-req [req]
  (log-request req)
  (http/request req))

(defn create-req [req-template acct]
  (-> (util/rec-merge basic-request req-template)
    (set-endpoint acct)
    (encode-body)
    (add-date)
    (add-host)
    (auth/sign-request acct) ; No changes beyond this point!
    (stringify-headers)
    (validate)))

(defn validate [req]
  req)

(defn deserialize-body [resp]
  (let [body (:body resp)]
    (assoc resp :body (if (map? body) 
                        body 
                        (json/read-str body :key-fn keyword)))))

(defn encode-body [req-template]
  (if (not (:body req-template))
    req-template
    (let [body-enc (util/utf8-encode (:body req-template))]
      (-> req-template
        (assoc :body body-enc)
        (assoc-in [:headers :content-length] (str (count body-enc)))))))

(defn add-date [req-template]
  (let [date (or (auth/get-date req-template) (Date.))]
    (-> req-template
      (assoc-in [:meta :date] date))))

(defn add-host [req-template]
  (assoc-in req-template [:headers :host] (:server-name req-template)))

(defn set-endpoint [req-template acct]
  (assoc req-template :server-name ((:region acct) glacier-endpoints)))

(defn stringify-headers [req-template]
  (assoc req-template :headers (util/keymap name (:headers req-template))))

(defn log-request [req]
  (log/write (format-request req)))

(defn format-request [req]
  (str (string/upper-case (name (:request-method req)))
       \space
       (:uri req)))
