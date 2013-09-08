(ns iceberg.database
  (:require [clojure.string :as str
             iceberg.file :as file])
  (:use [korma.core
         korma.databse]))

(def db-spec (h2 {:db "tree.db"}))

(defdb db-spec db)

(defentity files
  (belongs-to directory))

(defentity directories
  (has-many file))

(defn lookup-file [path]

(defn read-db-tree [db] 
  nil)

(defn write-db-tree [db]
  nil)
