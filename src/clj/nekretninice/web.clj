(ns nekretninice.web
  (:require [muuntaja.core :as m]
            [nekretninice.autentifikacija :as auth]
            [nekretninice.routes :as routes]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

(def app
  (ring/ring-handler
    (ring/router
      routes/routes
      {:conflicts identity
       :data      {:muuntaja   m/instance
                   :middleware [parameters/parameters-middleware
                                wrap-keyword-params
                                muuntaja/format-negotiate-middleware
                                muuntaja/format-response-middleware
                                muuntaja/format-request-middleware
                                auth/wrap-autentifikacija]}})
    (ring/routes
      (ring/create-resource-handler {:path "/" :root "javno"})
      (ring/create-default-handler))))