(ns iceberg.database
  (:require [clojure.string :as str]
            [iceberg.file :as file])
  (:use korma.core
        korma.db))

(declare db-spec db files directories lookup-file read-db-tree write-db-tree)

(def db-spec (h2 {:db "tree.db"}))

(defdb db db-spec)

(defentity files
  (belongs-to directories))

(defentity directories
  (has-many files))

(defn lookup-file [path]
  nil)

(defn read-db-tree [db] 
  nil)

(defn write-db-tree [db]
  nil)
