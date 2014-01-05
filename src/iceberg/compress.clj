(ns iceberg.compress
  (:require [iceberg.file :as file])
  (:import [java.io FileOutputStream FileInputStream]
           [org.tukaani.xz XZOutputStream XZInputStream LZMA2Options]))

(defn spit-xz [file s]
  (let [writer (FileOutputStream. file)
        xz-writer (XZOutputStream. writer (LZMA2Options.))]
    (spit xz-writer s)))

(defn slurp-xz [file]
  (let  [reader (FileInputStream. file)
         xz-reader (XZInputStream. reader)]
    (slurp xz-reader)))
