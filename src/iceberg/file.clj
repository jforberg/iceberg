(ns iceberg.file
  (:require  [me.raynes.fs :as fs])
  (:import [java.security MessageDigest DigestInputStream]
           [java.io FileInputStream File]))

(declare build-map build-dir build-file calc-hash path-join)

(defrecord FileProp [size    mtime   hash])
;;                   Integer Integer Byte[]

(defn build-map [path]
  (reduce conj {}
    (fs/walk (fn [r d f] (build-dir r f))
             path)))

(defn build-dir [root files]
  (hash-map root
            (reduce conj {}
              (map (fn [file] (build-file (path-join root file)))
                   files))))
  
(defn build-file [file]
  (hash-map (.getName file)
            (FileProp. (fs/size file) (fs/mod-time file) (calc-hash file))))

(defn calc-hash [file]
  (let [stream (DigestInputStream. (FileInputStream. file)
                                   (MessageDigest/getInstance "SHA-1"))]
    (while (not= -1 (.read stream)))
    (.digest (.getMessageDigest stream))))
  
(defn path-join [root base]
  (File. root base))
