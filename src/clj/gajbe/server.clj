(ns gajbe.server
  (:require [org.httpkit.server :as kit]
            [ring.middleware.reload :as reload]
            [taoensso.timbre :as log]
            [gajbe.web :as web])
  (:gen-class))


(defonce server (atom nil))

(defn stop-server []
  (when @server
    @(kit/server-stop! @server)                             ; we deref the returned promise to do the stopping in sync
    (reset! server nil)))

(defn start-server []
  (stop-server)
  (reset! server
    (kit/run-server (reload/wrap-reload #'web/app) {:port 3003 :legacy-return-value? false})))

(defn -main [& _args]
  (log/info "Starting local server")
  (start-server))
