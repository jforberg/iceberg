(ns iceberg.t-log
  (:use midje.sweet)
  (:require [iceberg.log :as log]))

(facts "about `newline-terminated`"
  (fact "empty string gives empty string"
    (log/newline-terminated "") => "")
  (fact "newline-terminated gives identical"
    (let [s "somegeneralstring\n"]
      (log/newline-terminated s) => s))
  (fact "general strings are newline-terminated"
    (let [s "somegeneralstring"]
      (log/newline-terminated s) => (str s \newline))))
