(ns iceberg.file
  (:import [java.security MessageDigest DigestInputStream]
           [java.io FileInputStream File FilenameFilter]))

(defrecord file [^long size ^long mtime ^String hashval])

(declare traverse traverse_ calc-hash create-filter create-file basename
         path-join paths-join as-file)

(defn traverse [dir f]
  (traverse_ (as-file dir) (create-filter f)))

(defn traverse_ [dir f]
  (let [entries (.listFiles dir f)]
    (if (nil? entries)
      (create-file dir)
      (reduce conj {} (map (fn [e] 
                             { (basename e) (traverse_ e f)})
                           entries)))))

(defn calc-hash [file]
  (let [stream (DigestInputStream. (FileInputStream. file)
                                   (MessageDigest/getInstance "SHA-1"))]
    (while (not= -1 (.read stream)))
    (.digest (.getMessageDigest stream))))
  
(defn create-filter [f]
  (proxy [FilenameFilter] []
    (accept [dir file] (f dir file))))

(defn create-file [f]
  (file. (.length f)
         (.lastModified f)
         nil))

(defn basename [f]
  (.getName f))

(defn path-join [root base]
  (File. (as-file root) base))

(defn paths-join [paths]
  (if (empty? paths)
    (File. "")
    (reduce path-join paths)))

(defn as-file [maybe-file]
  (if (instance? File maybe-file)
    maybe-file
    (File. maybe-file)))
