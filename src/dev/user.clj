(ns user
  (:require [gajbe.server :as server]))

(defn start []
  (server/start-server))

(defn stop []
  (server/stop-server))

(defn restart []
  (stop)
  (start))