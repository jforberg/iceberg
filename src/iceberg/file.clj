(ns iceberg.file
  (:require  [me.raynes.fs :as fs])
  (:import [java.security MessageDigest DigestInputStream]
           [java.io FileInputStream File]))

(defrecord FileProp [size    mtime   hash])
;                    Integer Integer Byte[]

;; Filesystem model: Directories are keys a flat hash-map. Each directory
;; is then a hash-map with filenames as keys. Behind these are FileProp 
;; records describing some properties of each file, used for comparisons. 

(declare build-map build-dir build-file calc-hash path-join)

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
            (FileProp. (fs/size file) (fs/mod-time file) nil)))

(defn calc-hash [file]
  (let [stream (DigestInputStream. (FileInputStream. file)
                                   (MessageDigest/getInstance "SHA-1"))]
    (while (not= -1 (.read stream)))
    (.digest (.getMessageDigest stream))))

(defn lookup-file [tree path]
  (let [file (as-file path)
        dir  (.getParent file)
        base (.getName file)]
    (get (get tree (as-file dir))
         base)))
  
(defn path-join [root base]
  (File. (as-file root) base))

(defn as-file [maybe-file]
  (if (instance? File maybe-file)
    maybe-file
    (File. maybe-file)))
