(ns iceberg.t-util
  (:use midje.sweet)
  (:require [iceberg.util :as util])
  (:import [java.util Date]))

(facts "about `valmap`"
  (fact "Handles empty map"
    (util/valmap nil {}) => {}
  (fact "Handles numeric map"
    (util/valmap inc {:a 1 :b 2 :c 3}) => {:a 2 :b 3 :c 4})))

(facts "about `strftime`"
  (fact "Handles epoch, iso8601 format"
    (util/strftime "yyyyMMdd'T'HHmmss'Z'" (Date. 0)) => "19700101T000000Z"))

(facts "about `sha256`"
  (fact "Handles empty string"
    (util/sha256 "") 
        => "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
  (fact "Handles nil"
    (util/sha256 nil) => (util/sha256 ""))
  (fact "Handles 'hello world'"
    (util/sha256 "hello world")
        => "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9")
  (fact "Handles amazon example 1"
    (util/sha256 "PUT\n/-/vaults/examplevault\n\nhost:glacier.us-east-1.amazonaws.com\nx-amz-date:20120525T002453Z\nx-amz-glacier-version:2012-06-01\n\nhost;x-amz-date;x-amz-glacier-version\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        => "5f1da1a2d0feb614dd03d71e87928b8e449ac87614479332aced3a701f916743")
  (fact "Handles amazon example 2"
    (util/sha256 "Action=ListUsers&Version=2010-05-08")
        => "b6359072c78d70ebee1e81adcbab4f01bf2c23245fa365ef83fe8f1f955085e2"))

(facts "about `sha256mac`"
  (fact "Handles hello world, hello world"
    (let [bts (.getBytes "hello world" "ascii")]
      (util/hex-enc 
        (util/sha256mac bts bts))) 
        => "6ec035d91dc104db569a01a4d8c16fb13f125dc298992edfb8e66d3a837fe0c5"))

(facts "about `hex-enc`"
  (fact "Handles empty string"
    (util/hex-enc "") => "")
  (fact "Handles `deadbeef`"
    (util/hex-enc [0xDE 0xAD 0xBE 0xEF]) => "deadbeef"))
