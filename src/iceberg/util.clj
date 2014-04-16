(ns iceberg.util
  "Utility functions for general use."
  (:require [clojure.string :as string])
  (:import [java.util Date TimeZone Arrays]
           [java.text SimpleDateFormat]
           [java.io InputStream]
           [java.security MessageDigest]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

(declare valmap rec-merge select uri strftime sha256 sha256mac hex-enc
         utf8-decode utf8-encode print-req fmt-req http-caps)

;;; General hash-map operations

(defn valmap
  "Map over the values of a hashmap."
  [f hmap]
  (reduce (fn [acc x]
            (assoc acc x (f (hmap x))))
          {}
          (keys hmap)))

(defn keymap
  "Map over the keys of a hashmap"
  [f hmap]
  (reduce (fn [acc x]
            (assoc acc (f x) (hmap x)))
          {}
          (keys hmap)))

(defn rec-merge 
  "Merge two maps recursively, the second taking precedence."
  [left right]
  (reduce (fn [acc r]
            (let [k (r 0)
                  rv (r 1)
                  lv (left k)]
              (if (every? #(instance? clojure.lang.Associative %) [lv rv])
                (assoc acc k (rec-merge lv rv))
                (assoc acc k rv))))
          left
          right))

(defn select 
  "Select certain keys from a hashmap, giving a new hashmap"
  [hmap ks]
  (reduce (fn [acc k]
            (assoc acc k (get hmap k)))
          {}
          ks))

(defn uri 
  "Construct a unix-path style uri from parts."
  [& parts]
  (cond 
    (empty? parts) "/"
    (= \/ (first (first parts))) (string/join \/ parts)
    :else (string/join \/ (cons "" parts))))

;;; Date handling

(defn strftime 
  "Like the C function, but with Java's SimpleDateFormat."
  [^String fmt ^Date date]
  (let [formatter (doto (SimpleDateFormat. fmt)
                    (.setTimeZone (TimeZone/getTimeZone "GMT")))]
    (.format formatter date)))

;;; Hashing functions

(defn sha256
  "Calculate the SHA-256 of a string."
  [s]
  (let [algo (MessageDigest/getInstance "SHA-256")
        s    (or s (byte-array []))]
    (hex-enc (.digest algo (utf8-encode s)))))

(defn sha256mac 
  "Calculate the SHA-256 MAC for a key/message pair."
  [k msg]
  (let [k (if (instance? String k) (utf8-encode k) k)
        msg (if (instance? String msg) (utf8-encode msg) msg)
        algo "HmacSHA256"
        mac (doto (Mac/getInstance algo)
              (.init (SecretKeySpec. k algo)))]
    (.doFinal mac msg)))

;;; Encoding/decoding

(defn hex-enc
  "Encode a byte-string in lowercase hexadecimal, byte-for-byte."
  [bts]
  (apply str (map #(format "%02x" %) bts)))

(defn utf8-decode 
  "Decode a byte-string as UTF-8."
  [bts]
  (if (instance? String bts)
    bts
    (String. bts "utf-8")))

(defn utf8-encode 
  "Encode a String to a UTF-8 byte-string."
  [s]
  (if (instance? (class (byte-array [])) s)
    s
    (.getBytes s "utf-8")))

;;; Functions for interactive use

(defn print-req
  "Print a HTTP request map in conventional format."
  [req]
  (print (fmt-req req)))

(defn fmt-req
  "Convert a HTTP request map to conventional format."
  [req]
  (let [headers (:headers req)
        fmt-headers (sort (map #(str (http-caps (name (% 0))) ": " (% 1)) 
                               headers))]
    (string/join "\n"
                 [(string/join " "
                               [(string/upper-case (name (:request-method req)))
                                (:uri req)
                                "HTTP/1.1"])
                  (string/join "\n" fmt-headers)
                  ""
                  (:body req)])))

(defn http-caps
  "Capitalise a string according to HTTP style."
  [s]
  (string/join "-" (map string/capitalize (string/split s #"-"))))

(defn array=
  "Equality for java arrays"
  [a1 a2]
  (if (and (nil? a1) (nil? a2))
    true
    (Arrays/equals a1 a2)))

