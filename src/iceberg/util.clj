(ns iceberg.util
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]
           [java.security MessageDigest]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

(declare valmap strftime sha256 sha256mac hex-enc)

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
    (let [bts (.getBytes s "utf-8")
          algo (MessageDigest/getInstance "SHA-256")
          dig (.digest algo bts)]
      (hex-enc dig))))

(defn sha256mac [k msg]
  (let [algo "HmacSHA256"
        mac (doto (Mac/getInstance algo)
              (.init (SecretKeySpec. k algo)))]
    (.doFinal mac msg)))

(defn hex-enc [bts]
  (apply str (map #(format "%02x" %) bts)))

