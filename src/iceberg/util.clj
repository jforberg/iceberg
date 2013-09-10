(ns iceberg.util
  (:import [java.util Date TimeZone]
           [java.text SimpleDateFormat]
           [java.security MessageDigest]))

(declare valmap strftime hej)

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
  (let [bts (.getBytes s "utf-8")
        algo (MessageDigest/getInstance "SHA-256")
        dig (.digest algo bts)]
    (apply str (map #(format "%02x" %) dig))))

