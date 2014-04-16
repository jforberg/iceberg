(ns iceberg.file
  "Logic for working the local file system and identifying changes."
  (:require [iceberg.util :as util])
  (:import [java.security MessageDigest DigestInputStream]
           [java.io FileInputStream File FilenameFilter]))

(defrecord FileData [^long size ^long mtime ^bytes hashval])

(defrecord FileChange [ctype ^FileData new-data])
;; ctype in [ :new-file, :blob-changed, :meta-changed, :deleted-file ]

;;; Non-declared types:
;;    Manifest:  (man in bindings): map where keys are File and values FileData.
;;    Changeset: (changes in bindings): map where keys are File and values
;;               FileChange.

(declare build-manifest file-builder write-manifest read-manifest
         manifest-changes file-change traverse-reduce collect-dirs
         collect-dirs_ calc-hash create-filter file-data
         create-file-with-hash  basename path-join paths-join as-file mkdir
         exists? file? dir? meta-changed?)

(def id-filter (fn [_ _] true))
(def dot-filter (fn [_ dir] (and (seq dir) (not= \. (first dir)))))

(defn build-manifest 
  ([dir pred]
   (build-manifest dir pred {}))
  ([dir pred oldman]
   (traverse-reduce (file-builder oldman) {} dir pred)))

(defn file-builder [oldman]
  (fn [acc file]
    (let [oldfdata (get oldman file)
          file-data (file-data file oldfdata)]
      (assoc acc file file-data))))

(defn write-manifest [man]
  (pr-str (map (fn [[^File file ^FileData fdata]]
                 (list (.getPath file) (:size fdata) (:mtime fdata) (:hashval fdata)))
               man)))

(defn read-manifest [s]
  (let [lst (read-string s)]
    (into {} (map (fn [form] 
                    [(as-file (first form)) (apply ->FileData (rest form))])
                  lst))))

(defn manifest-changes [man1 man2]
  (let [files (distinct (concat (keys man1) (keys man2)))]
    (into {}
          (filter (comp not nil?) (map #(file-change % man1 man2) files)))))

(defn file-change [file man1 man2]
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
        (if (util/array= (:hashval fdata1) (:hashval fdata2))
          [file (->FileChange :meta-changed fdata2)]
          [file (->FileChange :blob-changed fdata2)]) ; Implies also :meta-changed
      :else
        ;; No change
        nil)))

(defn traverse-reduce [f acc dir pred]
  (reduce f acc (collect-dirs dir pred)))

(defn collect-dirs [dir pred]
  (collect-dirs_ (as-file dir) (create-filter pred)))

(defn collect-dirs_ [^File dir ^FilenameFilter filt]
  (let [entries (.listFiles dir filt)
        files (filter file? entries)
        dirs (filter dir? entries)]
    (concat files (flatten (map #(collect-dirs_ % filt) dirs)))))

(defn calc-hash [^File file]
  (let [stream (DigestInputStream. (FileInputStream. file)
                                   (MessageDigest/getInstance "SHA-1"))]
    (while (not= -1 (.read stream)))
    (.digest (.getMessageDigest stream))))
  
(defn create-filter [f]
  (proxy [FilenameFilter] []
    (accept [dir file] (f dir file))))

(defn file-data
  ([^File file]
   (->FileData (.length file)
               (.lastModified file)
               nil))
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

