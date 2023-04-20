(ns gajbe.handlers
  (:require [ring.util.http-response :as r]))

(defn display-ads
  [{_params :params}]
  (r/ok "This is where ads will show"))


