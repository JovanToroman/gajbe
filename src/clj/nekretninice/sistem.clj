(ns nekretninice.sistem
  (:require [nekretninice.baza :as baza]
            [nekretninice.poslovi :as poslovi]
            [nekretninice.web :as web]
            [ring.adapter.jetty :as jetty]
            [taoensso.timbre :as log])
  (:gen-class))

(defn -main [& _args]
  (let [port            (parse-long (or (System/getenv "PORT") "3000"))
        interval-minuta (parse-long (or (System/getenv "SCRAPE_INTERVAL_MINUTES") "30"))]
    (baza/inicijalizuj-bazu!)
    (poslovi/pokreni-radnika! interval-minuta)
    (log/info "Pokrećem HTTP server na portu" port)
    (jetty/run-jetty #'web/app {:port port :join? true})))