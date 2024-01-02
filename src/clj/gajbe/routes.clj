(ns gajbe.routes
  (:require [gajbe.handlers :as handlers]))

(def routes
  ["/" {}
   ["" {:get {:handler    handlers/prikazi-oglase
              :parameters {}}}]])
