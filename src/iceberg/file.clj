(ns iceberg.file
  "Logic for working the local file system and identifying changes."
  (:import [java.security MessageDigest DigestInputStream]
           [java.io FileInputStream File FilenameFilter]))

(defrecord FileData [^long size ^long mtime ^String hashval])

(declare build-manifest traverse-reduce collect-dirs collect-dirs_ calc-hash
         create-filter create-file basename path-join paths-join as-file mkdir
         exists? file?  dir?)

(def id-filter (fn [_ _] true))
(def dot-filter (fn [_ dir] (and (seq dir) (not= \. (first dir)))))

(defn build-manifest [dir pred]
  (traverse-reduce (fn [acc ^File file] 
                     (assoc acc file (create-file file)))
                   {}
                   dir
                   pred))

(defn write-manifest [man]
  (pr-str (map (fn [[^File file ^FileData data]]
                 (list (.getPath file) (:size data) (:mtime data) (:hashval data)))
               man)))

(defn read-manifest [s]
  (let [lst (read-string s)]
    (into {} (map (fn [form] 
                    [(as-file (first form)) (apply ->FileData (rest form))])
                  lst))))

(defn traverse-reduce [f acc dir pred]
  (reduce f acc (collect-dirs dir pred)))

(defn collect-dirs [dir pred]
  (collect-dirs_ (as-file dir) (create-filter pred)))

(defn collect-dirs_ [^File dir ^FilenameFilter filt]
  (let [entries (.listFiles dir filt)
        files (filter file? entries)
        dirs (filter dir? entries)]
    (concat files (flatten (map #(collect-dirs_ % filt) dirs)))))

(defn calc-hash [file]
  (let [stream (DigestInputStream. (FileInputStream. file)
                                   (MessageDigest/getInstance "SHA-1"))]
    (while (not= -1 (.read stream)))
    (.digest (.getMessageDigest stream))))
  
(defn create-filter [f]
  (proxy [FilenameFilter] []
    (accept [dir file] (f dir file))))

(defn create-file [^File file]
  (->FileData (.length file)
              (.lastModified file)
              nil))

(defn basename [^File file]
  (.getName file))

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

(defn mkdir [file]
  (.mkdirs (as-file file)))

(defn exists? [^File file]
  (.exists file))

(defn file? [^File file]
  (.isFile file))

(defn dir? [^File file]
  (.isDirectory file))
