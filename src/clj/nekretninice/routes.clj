(ns nekretninice.routes
  (:require [nekretninice.hendleri :as handlers]))

(def routes
  ["/" {}
   ["" {:get {:handler    handlers/prikazi-oglase
              :parameters {}}}]])
