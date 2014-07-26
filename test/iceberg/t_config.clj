(ns iceberg.t-config
  (:use midje.sweet)
  (:require [iceberg.config :as config]
            [clojure.java.io :as io])
  (:import  [java.io StringReader]))

(facts "about `read-config`"
  (fact "handles the empty config"
    (let [input (StringReader. ";comment\n{};morecomment")]
      (config/read-config input) => {}))

  (fact "handles sample non-empty config"
    (let [input (StringReader. "{ :region :test-region } :comment")]
      (config/read-config input) => { :region :test-region })))

(facts "about `validate-config`"
  (let [valid-conf {:region :tokyo
                    :number "123123123"
                    :keyid "AKAI123123123"
                    :apikey "Erbsbsbssbsbsbs123123123"}]
    (fact "valid config passes validation"
      (config/validate-config valid-conf) => nil)

    (fact "rejects invalid region"
      (config/validate-config (assoc valid-conf :region :neverland)) 
        => #"Region")

    (fact "rejects invalid number"
      (config/validate-config (assoc valid-conf :number "fail")) 
        => #"Account number")

    (fact "rejects missing key-id"
      (config/validate-config (dissoc valid-conf :keyid))
        => #"ID")

    (fact "rejcts missing secret key"
      (config/validate-config (dissoc valid-conf :apikey))
        => #"Key")))

