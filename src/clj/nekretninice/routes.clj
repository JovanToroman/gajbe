(ns nekretninice.routes
  (:require [nekretninice.handlers :as handlers]))

(def routes
  ["/" {}
   ["" {:get {:handler    handlers/prikazi-oglase
              :parameters {}}}]])
