(ns iceberg.t-file
  (:use midje.sweet)
  (:require [iceberg.file :as file]
            [iceberg.util :as util])
  (:import [java.io File]))

(defn new-test-file [^File dir]
  (doto (File/createTempFile "iceberg-test." nil dir)
    .deleteOnExit))

(defn new-test-dir []
  (doto (File. (str "/tmp/iceberg-test." (rand-int 1e9)))
    .mkdirs
    .deleteOnExit))

(def example-dir-1 (new-test-dir))
(def example-dir-2 (new-test-dir)) ; empty

(def example-file-1 (new-test-file example-dir-1))
(def example-mtime-1 (.lastModified example-file-1))
(def example-file-2 (new-test-file example-dir-1))
(def example-mtime-2 (.lastModified example-file-2))

(spit example-file-2 "test string")

(facts "about `build-manifest`"
  (fact "handles empty dir"
    (file/build-manifest example-dir-2 (fn [_ _] true)) => {})
  (fact "handles non-empty dir"
    (file/build-manifest example-dir-1 (fn [_ _] true))
        => {example-file-1 (file/->FileData 0 example-mtime-1 nil)
            example-file-2 (file/->FileData 11 example-mtime-2 nil)}))

(facts "about `write-manifest`"
  (fact "handles empty manifest"
    (file/write-manifest {}) => "()")
  (fact "handles non-empty manifest"
    (file/write-manifest {(File. "file") (file/->FileData 1 2 "hash")})
        => "((\"file\" 1 2 \"hash\"))"))

(facts "about `read-manifest`"
  (fact "handles empty manifest"
    (file/read-manifest "()") => {})
  (fact "handles non-empty manifest"
    (file/read-manifest "((\"file\" 1 2 \"hash\"))")
        => {(File. "file") (file/->FileData 1 2 "hash")}))

(facts "about `traverse-reduce`"
  (fact "handles empty dir"
    (file/traverse-reduce (fn [_ _]) {} example-dir-2 (fn [_ _]))
        => {})
  (fact "handles non-empty dir"
    (file/traverse-reduce conj #{} example-dir-1 (fn [_ _] true))
        => (set [example-file-1 example-file-2])))

(facts "about `collect-dirs`"
  (fact "handles empty dir"
    (file/collect-dirs example-dir-2 file/id-filter) => empty?)
  (fact "handles non-empty dir"
    (let [collected (file/collect-dirs example-dir-1 file/id-filter)]
      (set collected) => #{example-file-1 example-file-2})))

(facts "about `calc-hash`"
  (fact "handles empty file"
    (util/hex-enc
      (file/calc-hash example-file-1)) => "da39a3ee5e6b4b0d3255bfef95601890afd80709")
  (fact "handles test string"
    (util/hex-enc
      (file/calc-hash example-file-2)) => "661295c9cbf9d6b2f6428414504a8deed3020641"))

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

(facts "about `exists?`"
  (fact "handles nonexistent"
    (file/exists? (File. "nonexistent")) => false)
  (fact "handles existent"
    (file/exists? example-file-1) => true))

(facts "about `file?`"
  (fact "handles dir"
    (file/file? example-dir-1) => false)
  (fact "handles file"
    (file/file? example-file-1) => true))

(facts "about `dir?`"
  (fact "handles dir"
    (file/dir? example-dir-1) => true)
  (fact "handles file"
    (file/dir? example-file-1) => false))
