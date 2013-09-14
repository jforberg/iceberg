(ns iceberg.t-util
  (:use midje.sweet)
  (:require [iceberg.util :as util]))

(facts "about `valmap`"
  (fact "Handles empty map"
    (util/valmap nil {}) => {}
  (fact "Handles numeric map"
    (util/valmap inc {:a 1 :b 2 :c 3}) => {:a 2 :b 3 :c 4})))

(facts "about `strftime`")

(facts "about `sha256`"
  (fact "Handles empty string"
    (util/sha256 "") 
        => "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
  (fact "Handles nil"
    (util/sha256 nil) => (util/sha256 ""))
  (fact "Handles 'hello world'"
    (util/sha256 "hello world")
        => "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9")
  (fact "Handles amazon example"
    (util/sha256 "PUT\n/-/vaults/examplevault\n\nhost:glacier.us-east-1.amazonaws.com\nx-amz-date:20120525T002453Z\nx-amz-glacier-version:2012-06-01\n\nhost;x-amz-date;x-amz-glacier-version\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        => "5f1da1a2d0feb614dd03d71e87928b8e449ac87614479332aced3a701f916743"))


