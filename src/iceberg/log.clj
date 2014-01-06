(ns iceberg.log
  (:require [clojure.string :as string]
            [iceberg.auth :as auth]
            [clojure.java.io :as io])
  (:import [java.util Date]
           [java.io OutputStream]))

(def ^:dynamic *logfile* nil)
(def ^:dynamic *console* nil)

(declare init! write)

(defn init! [config]
  (def ^:dynamic *logfile* (io/writer (:log-file config) :append true))
  (binding [*out* *logfile*]
    (println)
    (write (format "Init (iceberg %s)" (:command-line config))))
  (when (:interactive config)
    (def ^:dynamic *console* *err*)))

(defn write [^String s]
  (binding [*out* *logfile*]
    (let [lines (string/split-lines s)]
      (printf "[%s]  %s\n" (auth/iso8601-datetime (Date.)) (first lines))
      (when-let [remaining (seq (rest lines))]
        (print (apply str (map #(format "                    %s\n" %)
                               remaining)))))
    (flush))
  (when *console*
    (binding [*out* *console*]
      (println s)
      (flush))))

