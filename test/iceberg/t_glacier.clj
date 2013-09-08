(ns iceberg.t-glacier
  (:use midje.sweet)
  (:require [iceberg.glacier :as glacier]))

(facts "about `uri-encode`"
  (fact "Unreserved chars aren't escaped"
    (let [unres "abcABC123-_.~"]
      (glacier/uri-encode unres) => unres))
  (fact "Unicode chars are correctly escaped"
    (glacier/uri-encode "å-ä-ö") => "%C3%A5-%C3%A4-%C3%B6"))

