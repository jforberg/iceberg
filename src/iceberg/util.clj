(ns iceberg.util
  (:require [clojure.string :as string])
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]
           [java.io InputStream]
           [java.security MessageDigest]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

(declare valmap rec-merge select uri strftime sha256 sha256mac hex-enc
         utf8-decode print-req fmt-req http-caps)

;;; General hash-map operations

(defn valmap [f hmap]
  "Map over the values of a hashmap."
  (reduce (fn [acc x]
            (assoc acc x (f (hmap x))))
          {}
          (keys hmap)))

(defn rec-merge [left right]
  "Merge two hash-maps recursively (including any nested maps)."
  (reduce (fn [acc r]
            (let [k (r 0)
                  rv (r 1)
                  lv (left k)]
              (if (every? #(instance? clojure.lang.Associative %) [lv rv])
                (assoc acc k (rec-merge lv rv))
                (assoc acc k rv))))
          left
          right))

(defn select [hmap ks]
  "Select certain keys from a hashmap, giving a new hashmap"
  (reduce (fn [acc k]
            (assoc acc k (get hmap k)))
          {}
          ks))

(defn uri [& parts]
  "Construct a unix-path style uri from parts."
  (if (empty? parts)
    "/"
    (string/join "/" (cons "" parts))))

;;; Date handling

(defn strftime [^String fmt ^Date date]
  "Like the C function, but with Java's SimpleDateFormat."
  (let [formatter (doto (SimpleDateFormat. fmt)
                    (.setTimeZone (TimeZone/getTimeZone "GMT")))]
    (.format formatter date)))

;;; Hashing functions

(defn sha256 [^String s]
  "Calculate the SHA-256 of a string."
  (if (nil? s)
    (sha256 "")
    (let [algo (MessageDigest/getInstance "SHA-256")]
      (hex-enc (.digest algo (utf8-decode s))))))

(defn sha256mac [k msg]
  "Calculate the SHA-256 MAC for a key/message pair."
  (let [k (if (instance? String k) (utf8-decode k) k)
        msg (if (instance? String msg) (utf8-decode msg) msg)
        algo "HmacSHA256"
        mac (doto (Mac/getInstance algo)
              (.init (SecretKeySpec. k algo)))]
    (.doFinal mac msg)))

;;; Encoding/decoding

(defn hex-enc [bts]
  "Encode a byte-string in lowercase hexadecimal, byte-for-byte."
  (apply str (map #(format "%02x" %) bts)))

(defn utf8-encode [bts]
  "Encode a byte-string as UTF-8."
  (String. bts "utf-8"))

(defn utf8-decode [^String s]
  "Decode a String to a UTF-8 byte-string."
  (.getBytes s "utf-8"))

;;; Functions for interactive use

(defn print-req [req]
  "Print a HTTP request map in conventional format."
  (print (fmt-req req)))

(defn fmt-req [req]
  "Convert a HTTP request map to conventional format."
  (let [headers (merge {:date (:date req)
                        :host (:server-name req)}
                       (:headers req))
        fmt-headers (sort (map #(str (http-caps (name (% 0))) ": " (% 1)) 
                               headers))]
    (string/join "\n"
                 [(string/join " "
                               [(string/upper-case (name (:method req)))
                                (:uri req)
                                "HTTP/1.1"])
                  (string/join "\n" fmt-headers)
                  ""
                  (:body req)])))

(defn http-caps [s]
  "Capitalise a string according to HTTP style."
  (string/join "-" (map string/capitalize (string/split s #"-"))))
