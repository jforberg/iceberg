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
  (fact "Handles 'hello world'"
    (util/sha256 "hello world")
        => "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"))

