(ns iceberg.t-file
  (:use midje.sweet)
  (:require [iceberg.file :as file]
            [iceberg.util :as util])
  (:import [java.io File]))

;;; Helper defs

(defn new-test-file [^File dir]
  (doto (File/createTempFile "iceberg-test." nil dir)
    .deleteOnExit))

(defn new-test-dir []
  (doto (File. (str "/tmp/iceberg-test/dir." (rand-int 1e9)))
    .mkdirs
    .deleteOnExit))

#_ (defn fdata= [fdata1 fdata2]
  (and
    (= (:size fdata1) (:size fdata2))
    (= (:mtime fdata1) (:mtime fdata2))
    (util/array= (:hashval fdata1) (:hashval fdata2))))

(def example-dir-1 (new-test-dir))
(def example-dir-2 (new-test-dir)) ; empty

(def example-file-1 (new-test-file example-dir-1))
(def example-mtime-1 (.lastModified example-file-1))
(def example-file-2 (new-test-file example-dir-1))
(def example-mtime-2 (.lastModified example-file-2))

(spit example-file-1 "")
(spit example-file-2 "test string")

(def example-hash-1 "da39a3ee5e6b4b0d3255bfef95601890afd80709")
(def example-hash-2 "661295c9cbf9d6b2f6428414504a8deed3020641")

;;; Test facts

(facts "about `build-manifest`"
  (fact "handles empty dir"
    (file/build-manifest example-dir-2 (fn [_ _] true)) => {})
  (fact "handles non-empty dir"
    (file/build-manifest example-dir-1 (fn [_ _] true))
      => {example-file-1 (file/->FileData 0 example-mtime-1 example-hash-1)
          example-file-2 (file/->FileData 11 example-mtime-2 example-hash-2)}
  (fact "generates equal manifest for equal files"
    ;; It's conceivable this may fail due to someone modifying the test files...
    (file/build-manifest example-dir-1 file/id-filter) 
        => (file/build-manifest example-dir-1 file/id-filter)
    (file/build-manifest example-dir-2 file/id-filter) 
        => (file/build-manifest example-dir-2 file/id-filter))))

(facts "about `file-builder`"
  (fact "handles case of empty oldman"
    ((file/file-builder {}) {} example-file-1)
        => {example-file-1 (file/file-data example-file-1)}))

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

(facts "about `manifest-changes`"
  (let [test-file (File. "interesting") ; File used for testing
        test-data (file/->FileData 1 2 "abc") ; FileData for testing
        test-man {(File. "irrelevant") (file/->FileData 0 0 "def")
                  test-file test-data}] ; Also file just along for the ride
    (fact "handles new file"
      (let [new-file (File. "new")
            new-data (file/->FileData 1 2 "ghi")]
        (file/manifest-changes test-man (assoc test-man new-file new-data))
            => {new-file (file/->FileChange :new-file new-data)}))
    (fact "handles deleted file"
      (file/manifest-changes test-man (dissoc test-man test-file))
          => {test-file (file/->FileChange :deleted-file nil)})
    (fact "handles modified metadata"
      (let [new-data (file/->FileData 3 4 (:hashval test-data))]
        (file/manifest-changes test-man (assoc test-man test-file new-data))
            => {test-file (file/->FileChange :meta-changed new-data)}))
    (fact "handles modified blob"
      (let [new-data (assoc test-data :hashval "jkl")]
        (file/manifest-changes test-man (assoc test-man test-file new-data))
            => {test-file (file/->FileChange :blob-changed new-data)}))))

(facts "about `file-change`"
  (let [file (File. "testfile")
        test-data (file/->FileData 1 2 "abc")]
    (fact "handles empty case"
      (file/file-change file {} {}) => nil)
    (fact "handles no change"
      (let [man {file test-data}]
        (file/file-change file man man) => nil))
    (fact "handles addition of one file"
      (file/file-change file {} {file test-data})
          => [file (file/->FileChange :new-file test-data)])
    (fact "handles removal of one file"
      (file/file-change file {file test-data} {})
          => [file (file/->FileChange :deleted-file nil)])
    (fact "handles metadata modification for one file"
      (let [mod-data (assoc test-data :mtime 123123)]
      (file/file-change file {file test-data} {file mod-data})
          => [file (file/->FileChange :meta-changed mod-data)]))
    (fact "handles blob modification for one file"
      (let [mod-data (assoc test-data :hashval "def")]
      (file/file-change file {file test-data} {file mod-data})
          => [file (file/->FileChange :blob-changed mod-data)]))))

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
    (file/calc-hash example-file-1) => example-hash-1)
  (fact "handles example file"
    (file/calc-hash example-file-2) => example-hash-2))

(facts "about `create-filter`"
  (fact "handles example"
    (let [func #(= "hej" %2)
          filt (file/create-filter func)]
      (.accept filt (File. "/onedir") "other") => false
      (.accept filt (File. "/twodir") "hej")) => true))

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

(facts "about `meta-changed?`"
  (let [test-data (file/->FileData 1 2 "")]
    (fact "handles unchanged meta"
      (file/meta-changed? test-data test-data) => false)
    (fact "handles changed meta"
      (file/meta-changed? test-data (assoc test-data :mtime 123)) => true)))

