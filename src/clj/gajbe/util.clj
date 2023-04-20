(ns gajbe.util)

(defmacro -m
  [& args]
  (reduce (fn [m v]
            (assoc m (keyword v) v))
    {}
    args))
