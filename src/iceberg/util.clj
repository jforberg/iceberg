(ns iceberg.util
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]
           [java.io InputStream]
           [java.security MessageDigest]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

(declare valmap strftime sha256 sha256mac hex-enc utf8-decode)

(defn valmap [f hmap]
    (reduce (fn [acc x]
              (assoc acc x (f (hmap x))))
            {}
            (keys hmap)))

(defn strftime [^String fmt ^Date date]
  (let [formatter (doto (SimpleDateFormat. fmt)
                    (.setTimeZone (TimeZone/getTimeZone "GMT")))]
    (.format formatter date)))

(defn sha256 [^String s]
  (if (nil? s)
    (sha256 "")
    (let [algo (MessageDigest/getInstance "SHA-256")]
      (hex-enc (.digest algo (utf8-decode s))))))

(defn sha256mac [k msg]
  (let [k (if (instance? String k) (utf8-decode k) k)
        msg (if (instance? String msg) (utf8-decode msg) msg)
        algo "HmacSHA256"
        mac (doto (Mac/getInstance algo)
              (.init (SecretKeySpec. k algo)))]
    (.doFinal mac msg)))

(defn hex-enc [bts]
  (apply str (map #(format "%02x" %) bts)))

(defn utf8-encode [bts]
  (String. bts "utf-8"))

(defn utf8-decode [^String s]
  (.getBytes s "utf-8"))
