(ns iceberg.log
  (:require [clojure.string :as string]
            [iceberg.auth :as auth]
            [clojure.java.io :as io])
  (:import [java.util Date]
           [java.io OutputStream]))

(declare ^:dynamic *logfile* ^:dynamic *verbosity* init!  write first-template
         rest-template format-entry newline-terminated)

(def ^:dynamic *logfile* nil)
(def ^:dynamic *verbosity* 0)

(defn init! [config]
  (def ^:dynamic *logfile* (io/writer (:log-file config) :append true))
  (binding [*out* *logfile*]
    (println)
    (write (format "Init (iceberg %s)" (:command-line config))))
  (when (:interactive config)
    (def ^:dynamic *console* *err*))
  (when (:verbose config)
    (def ^:dynamic *verbosity* 1)))

(defn write
  ([^String s]
   (write s 0))
  ([^String s level]
    ;; Print to logfile
    (when *logfile*
      (binding [*out* *logfile*]
        (print (format-entry s))
        (flush)))
    ;; Print to stdout if appropriate verbosity
    (when (<= level *verbosity*)
      (print (newline-terminated s))
      (flush))))

(def first-template "[%s] %s\n")
(def rest-template "                    %s\n")

(defn format-entry [^String s]
  (let [lines (string/split-lines s)
        today (auth/iso8601-datetime (Date.))]
    (str (format first-template today (first lines))
         (if-let [remaining (seq (rest lines))]
           (apply str (map #(format rest-template %)
                           remaining))
           ""))))

(defn newline-terminated [^String s]
  (cond (empty? s) ""
        (= \newline (last s)) s
        :else (str s \newline)))

(defn pad-lines [spaces lines]
  (let [padding (apply str (take spaces (repeat \space)))]
    (string/join \newline (map #(str padding %) 
                                (string/split-lines lines)))))
                     
