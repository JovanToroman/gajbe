(ns nekretninice.util
  (:import (java.security MessageDigest)))

(defmacro -m
  [& args]
  (reduce (fn [m v]
            (assoc m (keyword v) v))
    {}
    args))

(defn md5 ^String [object]
  (->> object
    str
    .getBytes
    (.digest (MessageDigest/getInstance "MD5"))
    (BigInteger. 1)
    (format "%032x")))