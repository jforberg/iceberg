(ns iceberg.t-file
  (:use midje.sweet)
  (:require [iceberg.file :as file])
  (:import [java.io File]))

(facts "about `path-join`"
  (fact "handles root"
    (file/path-join (File. "/") "dir") => (File. "/dir"))
  (fact "handles example"
    (file/path-join (File. "/dir") "file") => (File. "/dir/file")))

(facts "about `paths-join"
  (fact "handles empty list"
    (file/paths-join []) => (File. ""))
  (fact "handles non-empty list"
    (file/paths-join ["stairway" "to" "heaven"]) => (File. "stairway/to/heaven")))

(facts "about `basename`"
  (fact "handles root"
    (file/basename (File. "/")) => "")
  (fact "handles example"
    (file/basename (File. "/home/me/stuff")) => "stuff"))
