(ns iceberg.t-glacier
  (:use midje.sweet)
  (:require [iceberg.glacier :as glacier]))

(facts "about `canonical-path`"
  (fact "Handles amazon example"
    (glacier/canonical-path "http://iam.amazonaws.com/") => "/")
  (fact "Handles random example"
    (glacier/canonical-path "prot://what.ever.com:1234/my/awesome/fÏlë")
        => "/my/awesome/f%C3%8Fl%C3%AB")
  (fact "Handles partially-escaped uri"
    (glacier/canonical-path "http://hey.com/%20-ö-%20") => "/%20-%C3%B6-%20")
  (fact "Handles empty uri"
    (glacier/canonical-path "") => "/"))

(facts "about `canonical-query`"
  (fact "Handles empty query"
    (glacier/canonical-query "http://hello.com/") => "")
  (fact "Handles example query"
    (glacier/canonical-query "http://hello.com/?b=1&F=2") => "F=2&b=1")
  (fact "Can escape parameters"
    (glacier/canonical-query "http://hello.com/?ö=2&1=å") => "1=%C3%A5&%C3%B6=2"))

(facts "about `canonical-headers`"
  (fact "Handles headers with only host"
    (glacier/canonical-headers {:host "me.com"}) => "host:me.com")
  (fact "Reports missing host"
    (glacier/canonical-headers {:whateva "my united states of"})
        => (throws IllegalArgumentException)))

(facts "about `parse-query`"
  (fact "Handles empty query"
    (glacier/parse-query "") => {})
  (fact "Handles nil"
    (glacier/parse-query nil) => {})
  (fact "Handles example query"
    (glacier/parse-query "a=1&b=2&  =3") => {"a" "1", "b" "2","  " "3"}))

(facts "about `uri-encode`"
  (fact "Unreserved chars aren't escaped"
    (let [unres "abcABC123-_.~%"]
      (glacier/uri-encode unres) => unres))
  (fact "Unicode chars are correctly escaped"
    (glacier/uri-encode "å-ä-ö") => "%C3%A5-%C3%A4-%C3%B6"))

