(ns iceberg.t-auth
  (:use midje.sweet)
  (:require [iceberg.auth :as auth]
            [iceberg.util :as util])
  (:import [java.util Date]))

(def example-request-1
  ;; From http://docs.aws.amazon.com/amazonglacier/2012-06-01/dev/amazon-glacier-signing-requests.html
  { ; IP layer
   :server-name "glacier.us-east-1.amazonaws.com"
    ; HTTP layer
   :request-method :put
   :uri "/-/vaults/examplevault"
   :query-string ""
    ; Application layer
   :headers
     {:x-amz-glacier-version "2012-06-01"
      :host "glacier.us-east-1.amazonaws.com"}
   :body ""
   :meta
     {:date (Date. "Fri, 25 May 2012 00:24:53 GMT")}})

(def example-request-2
  ;; From http://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
  {:server-name "iam.amazonaws.com"
   :request-method :post
   :uri "/"
   :query-string ""
   :headers
     {:content-type "application/x-www-form-urlencoded; charset=utf-8"
      :host "iam.amazonaws.com"}
   :body "Action=ListUsers&Version=2010-05-08"
   :meta 
     {:date (Date. "09 September 2011 23:36:00 GMT")
      :region "us-east-1"
      :service "iam"}})

(def example-request-3
  {:server-name "me.com"
   :headers
     {:host "me.com"}
   :meta
     {:date (Date. 0)}})

(def example-request-4
  {:server-name "host.foo.com"
   :request-method :get
   :uri "/"
   :query-string "foo=Zoo&foo=aha"
   :headers
     {:host "host.foo.com"
      :date "Mon, 09 Sep 2011 23:36:00 GMT"}
   :meta
     {:date (Date. "Mon, 09 Sep 2011 23:36:00 GMT")
      :omit-x-amz-date true
      :service "host"
      :region "us-east-1"}})

(def example-acct-1
  {:number nil 
   :region nil 
   :keyid "AKIAIOSFODNN7EXAMPLE" 
   :apikey "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"})

(def example-acct-2
  {:number nil 
   :region :oregon 
   :keyid "AKIDEXAMPLE" 
   :apikey "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"})

(facts "about `signature`"
  (fact "Handles amazon example 2"
    (auth/signature example-request-2 example-acct-1)
       => "ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c")
  (fact "Handles amazon example 4"
    (auth/signature example-request-4 example-acct-2)
       => "be7148d34ebccdc6423b19085378aa0bee970bdc61d144bd1a8c48c33079ab09"))

(facts "about `signing-key`"
  (fact "Handles amazon example"
    (util/hex-enc
      (auth/signing-key example-request-2 example-acct-1))
       => "98f1d889fec4f4421adc522bab0ce1f82e6929c262ed15e5a94c90efd1e3b0e7"))

(facts "about `string-to-sign`"
  (fact "Handles amazon example 1"
    (auth/string-to-sign example-request-1)
        => "AWS4-HMAC-SHA256\n20120525T002453Z\n20120525/us-east-1/glacier/aws4_request\n5f1da1a2d0feb614dd03d71e87928b8e449ac87614479332aced3a701f916743")
  (fact "Handles amazon example 2"
    (auth/string-to-sign example-request-2)
        => "AWS4-HMAC-SHA256\n20110909T233600Z\n20110909/us-east-1/iam/aws4_request\n3511de7e95d28ecd39e9513b642aee07e54f4941150d8df8bf94b328ef7e55e2"))

(facts "about `canonical-request`"
  (fact "Handles amazon example 1"
    (auth/canonical-request example-request-1)
        => "PUT\n/-/vaults/examplevault\n\nhost:glacier.us-east-1.amazonaws.com\nx-amz-date:20120525T002453Z\nx-amz-glacier-version:2012-06-01\n\nhost;x-amz-date;x-amz-glacier-version\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
  (fact "Handles amazon example 2"
    (auth/canonical-request example-request-2)
        => "POST\n/\n\ncontent-type:application/x-www-form-urlencoded; charset=utf-8\nhost:iam.amazonaws.com\nx-amz-date:20110909T233600Z\n\ncontent-type;host;x-amz-date\nb6359072c78d70ebee1e81adcbab4f01bf2c23245fa365ef83fe8f1f955085e2")
  (fact "Handles amazon example 4"
    (auth/canonical-request example-request-4)
        => "GET\n/\nfoo=Zoo&foo=aha\ndate:Mon, 09 Sep 2011 23:36:00 GMT\nhost:host.foo.com\n\ndate;host\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"))

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
    (auth/canonical-query "ö=2&1=å") => "1=%C3%A5&%C3%B6=2")
  (fact "Handles duplicate parameters"
    (auth/canonical-query "a=1&a=2") => "a=1&a=2"))

(facts "about `canonical-headers`"
  (fact "Handles headers with only host"
    (auth/canonical-headers example-request-3)
        => "host:me.com\nx-amz-date:19700101T000000Z")
  (fact "Reports missing host"
    (auth/canonical-headers {:whateva "my united states of"})
        => (throws IllegalArgumentException)))

(facts "about `parse-query`"
  (fact "Handles empty query"
    (auth/parse-query "") => [])
  (fact "Handles nil"
    (auth/parse-query nil) => [])
  (fact "Handles example query"
    (auth/parse-query "a=1&b=2&  =3") => [["a" "1"] ["b" "2"] ["  " "3"]]))

(facts "about `uri-encode`"
  (fact "Unreserved chars aren't escaped"
    (let [unres "abcABC123-_.~%"]
      (auth/uri-encode unres) => unres))
  (fact "Unicode chars are correctly escaped"
    (auth/uri-encode "å-ä-ö") => "%C3%A5-%C3%A4-%C3%B6"))

(facts "about `cred-scope`"
  (fact "Handles unary case"
    (auth/cred-scope example-request-1) 
        => "20120525/us-east-1/glacier/aws4_request")
  (fact "Handles binary case"
    (auth/cred-scope example-request-1 example-acct-1)
        => "AKIAIOSFODNN7EXAMPLE/20120525/us-east-1/glacier/aws4_request")
  (fact "Handles amazon example 4"
    (auth/cred-scope example-request-4 example-acct-2)
        => "AKIDEXAMPLE/20110909/us-east-1/host/aws4_request"))

(facts "about `get-region`"
  #_ (fact "Handles bogus domain"
       (auth/get-region {:server-name "bogus"}) 
        => nil)
  (fact "Handles proper domain"
    (auth/get-region {:server-name "glacier.us-east-1.amazonaws.com"}) 
        => "us-east-1"))

(facts "about `iso8601-datetime`"
  (fact "Handles epoch"
    (auth/iso8601-datetime (Date. 0)) => "19700101T000000Z"))
