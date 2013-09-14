(ns iceberg.t-auth
  (:use midje.sweet)
  (:require [iceberg.auth :as auth])
  (:import [java.util Date]))

(def example-request
  { ; IP layer, mandatory
   :server-name "glacier.us-east-1.amazonaws.com"
   :server-port 80
   :remote-addr "127.0.0.1"
    ; HTTP layer, mandatory
   :scheme :http
   :request-method :put
   :uri "/-/vaults/examplevault"
   :query-string ""
    ; Iceberg-specific
   :date (Date. "Fri, 25 May 2012 00:24:53 GMT")
    ; Application layer, optional
   :headers
     {:x-amz-glacier-version "2012-06-01"}
   :body
     nil})

(facts "about `string-to-sign`"
  (fact "Handles amazon example"
    (auth/string-to-sign example-request)
        => "AWS4-HMAC-SHA256\n20120525T002453Z\n20120525/us-east-1/glacier/aws4_request\n5f1da1a2d0feb614dd03d71e87928b8e449ac87614479332aced3a701f916743"))

(facts "about `canonical-request`"
  (fact "Handles amazon example"
    (auth/canonical-request example-request)
        => "PUT\n/-/vaults/examplevault\n\nhost:glacier.us-east-1.amazonaws.com\nx-amz-date:20120525T002453Z\nx-amz-glacier-version:2012-06-01\n\nhost;x-amz-date;x-amz-glacier-version\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))

(facts "about `canonical-path`"
  (fact "Handles root uri"
    (auth/canonical-path "/") => "/")
  (fact "Handles random example"
    (auth/canonical-path "/my/awesome/fÏlë")
        => "/my/awesome/f%C3%8Fl%C3%AB")
  (fact "Handles partially-escaped uri"
    (auth/canonical-path "/%20-ö-%20") => "/%20-%C3%B6-%20")
  (fact "Handles empty uri"
    (auth/canonical-path "") => "/"))

(facts "about `canonical-query`"
  (fact "Handles empty query"
    (auth/canonical-query "") => "")
  (fact "Handles example query"
    (auth/canonical-query "b=1&F=2") => "F=2&b=1")
  (fact "Can escape parameters"
    (auth/canonical-query "ö=2&1=å") => "1=%C3%A5&%C3%B6=2"))

(facts "about `canonical-headers`"
  (fact "Handles headers with only host"
    (auth/canonical-headers {:server-name "me.com" :date (Date. 0)}) 
        => "host:me.com\nx-amz-date:19700101T000000Z")
  (fact "Reports missing host"
    (auth/canonical-headers {:whateva "my united states of"})
        => (throws IllegalArgumentException)))

(facts "about `parse-query`"
  (fact "Handles empty query"
    (auth/parse-query "") => {})
  (fact "Handles nil"
    (auth/parse-query nil) => {})
  (fact "Handles example query"
    (auth/parse-query "a=1&b=2&  =3") => {"a" "1", "b" "2","  " "3"}))

(facts "about `uri-encode`"
  (fact "Unreserved chars aren't escaped"
    (let [unres "abcABC123-_.~%"]
      (auth/uri-encode unres) => unres))
  (fact "Unicode chars are correctly escaped"
    (auth/uri-encode "å-ä-ö") => "%C3%A5-%C3%A4-%C3%B6"))

(facts "about `extract-region`"
  (fact "Handles bogus domain"
    (auth/extract-region "bogus") => nil)
  (fact "Handles proper domain"
    (auth/extract-region "glacier.us-east-1.amazonaws.com") => "us-east-1"))

(facts "about `iso8601-datetime`"
  (fact "Handles epoch"
    (auth/iso8601-datetime (Date. 0)) => "19700101T000000Z"))
