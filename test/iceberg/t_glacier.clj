(ns iceberg.t-glacier
  (:use midje.sweet)
  (:require [iceberg.glacier :as glacier]))

(def account {:region :tokyo
              :number "123"
              :keyid "AKIA123"
              :apikey "ABC123"})

(facts "about `list-vaults-req`")

(facts "about `deserialize-body`"
  (fact "handles the empty (string) case"
    (glacier/deserialize-body {:body "{}"})
      => {:body {}})

  (fact "handles the empty (map) case"
    (glacier/deserialize-body {:body {}}) => {:body {}}))

(facts "about `encode-body`"
  (fact "handles request with no body"
    (glacier/encode-body {:test 123}) => {:test 123})

  (fact "handles empty body"
    (let [req (glacier/encode-body {:body ""})]
      (vec (:body req)) => []
      (:headers req) => {:content-length "0"}))
  
  (fact "handles non-empty body"
    (let [req (glacier/encode-body {:body "message"})]
      (vec (:body req)) => (map byte "message")
      (:headers req) => {:content-length "7"})))
