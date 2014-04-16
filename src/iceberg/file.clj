(ns iceberg.file
  "Logic for working the local file system and identifying changes."
  (:require [iceberg.util :as util])
  (:import [java.security MessageDigest DigestInputStream]
           [java.io FileInputStream File FilenameFilter]))

;;; Metadata for a file, not including its name.
(defrecord FileData [^long size ^long mtime ^String hashval])

;;; Changes in a file: type of change and new metadata.
;;;   ctype in [ :new-file, :blob-changed, :meta-changed, :deleted-file ]
(defrecord FileChange [ctype ^FileData new-data])

;;; Non-declared types:
;;    Manifest:  (man in bindings): map where keys are File and values FileData.
;;    Changeset: (changes in bindings): map where keys are File and values
;;               FileChange.

(declare build-manifest file-builder write-manifest read-manifest
         manifest-changes file-change traverse-reduce collect-dirs
         collect-dirs_ calc-hash create-filter file-data
         create-file-with-hash  basename path-join paths-join as-file mkdir
         exists? file? dir? meta-changed?)

(def id-filter (constantly true))

;;; In the `build` methods, oldman is a reference to the previous manifest.

(defn build-manifest 
  "Build the manifest for files in `dir` matching `pred`."
  ([dir pred]
   (build-manifest dir pred {}))
  ([dir pred oldman]
   (traverse-reduce (file-builder oldman) {} dir pred)))

(defn file-builder 
  "Build data record for a single file"
  [oldman]
  (fn [acc file]
    (let [oldfdata (get oldman file)
          file-data (file-data file oldfdata)]
      (assoc acc file file-data))))

(defn write-manifest 
  "Write manifest `man` to string in our format."
  [man]
  (pr-str (map (fn [[^File file ^FileData fdata]]
                 (list (.getPath file) (:size fdata) (:mtime fdata) (:hashval fdata)))
               man)))

(defn read-manifest 
  "Parse manifest from `s`."
  [s]
  (let [lst (read-string s)]
    (into {} (map (fn [form] 
                    [(as-file (first form)) (apply ->FileData (rest form))])
                  lst))))

(defn manifest-changes 
  "Compare two manifests and return the changeset."
  [man1 man2]
  (let [files (distinct (concat (keys man1) (keys man2)))]
    (into {}
          (filter (comp not nil?) (map #(file-change % man1 man2) files)))))

(defn file-change 
  "Return changes, or `nil` if none, for a single file."
  [file man1 man2]
  (let [fdata1 (get man1 file)
        fdata2 (get man2 file)]
    (cond 
      (and (nil? fdata1) (nil? fdata2)) 
        ;; Unknown file
        nil
      (nil? fdata1) 
        ;; File addition
        [file (->FileChange :new-file fdata2)]
      (nil? fdata2)
        ;; File deletion
        [file (->FileChange :deleted-file nil)]
      (not= fdata1 fdata2)
        ;; File mutation
        (if (= (:hashval fdata1) (:hashval fdata2))
          [file (->FileChange :meta-changed fdata2)]
          [file (->FileChange :blob-changed fdata2)]) ; Implies also :meta-changed
      :else
        ;; No change
        nil)))

(defn traverse-reduce 
  "Reduce over a directory tree, ignoring files not matching `pred`."
  [f acc dir pred]
  (reduce f acc (collect-dirs dir pred)))

(defn collect-dirs 
  "Accumulate directories, á la UNIX find(1)."
  [dir pred]
  (collect-dirs_ (as-file dir) (create-filter pred)))

(defn collect-dirs_ [^File dir ^FilenameFilter filt]
  (let [entries (.listFiles dir filt)
        files (filter file? entries)
        dirs (filter dir? entries)]
    (concat files (flatten (map #(collect-dirs_ % filt) dirs)))))

(defn calc-hash 
  "Return SHA-1 sum of `file` as byte[]."
  [^File file]
  (let [stream (DigestInputStream. (FileInputStream. file)
                                   (MessageDigest/getInstance "SHA-1"))]
    (while (not= -1 (.read stream)))
    (-> stream
        .getMessageDigest
        .digest
        util/hex-enc)))
  
(defn create-filter 
  "Get FilenameFilter from predicate."
  [pred]
  (proxy [FilenameFilter] []
    (accept [dir file] (pred dir file))))

(defn file-data
  "Get metadata from `file`, possibly copying hash values from `oldfdata`."
  ([^File file]
   (->FileData (.length file)
               (.lastModified file)
               (calc-hash file)))
  ([^File file ^FileData oldfdata]
   (let [fdata (file-data file)]
     (assoc fdata :hashval (if (meta-changed? fdata oldfdata)
                             (calc-hash file)
                             (:hashval oldfdata))))))

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
  (and (not (nil? file)) (.isFile file)))

(defn dir? [^File file]
  (and (not (nil? file)) (.isDirectory file)))

(defn meta-changed? [^FileData fdata ^FileData oldfdata]
  (not= (dissoc fdata :hashval)
        (dissoc oldfdata :hashval)))

