(ns iceberg.t-auth
  (:use midje.sweet)
  (:require [iceberg.auth :as auth])
  (:import [java.util Date]))

(facts "about `canonical-path`"
  (fact "Handles amazon example"
    (auth/canonical-path "http://iam.amazonaws.com/") => "/")
  (fact "Handles random example"
    (auth/canonical-path "prot://what.ever.com:1234/my/awesome/fÏlë")
        => "/my/awesome/f%C3%8Fl%C3%AB")
  (fact "Handles partially-escaped uri"
    (auth/canonical-path "http://hey.com/%20-ö-%20") => "/%20-%C3%B6-%20")
  (fact "Handles empty uri"
    (auth/canonical-path "") => "/"))

(facts "about `canonical-query`"
  (fact "Handles empty query"
    (auth/canonical-query "http://hello.com/") => "")
  (fact "Handles example query"
    (auth/canonical-query "http://hello.com/?b=1&F=2") => "F=2&b=1")
  (fact "Can escape parameters"
    (auth/canonical-query "http://hello.com/?ö=2&1=å") => "1=%C3%A5&%C3%B6=2"))

(facts "about `canonical-headers`"
  (fact "Handles headers with only host"
    (auth/canonical-headers {:host "me.com"}) => "host:me.com")
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

(facts "about `iso8601-datetime`"
  (fact "Handles epoch"
    (auth/iso8601-datetime (Date. 0)) => "19700101T000000Z"))
