(ns nekretninice.konfiguracija.glavno
  (:require [applied-science.js-interop :as j]
            [nekretninice.aplikacija.prikazi :as prikazi]
            [nekretninice.konfiguracija.dogadjaji :as dogadjaji]
            [nekretninice.konfiguracija.podesavanja :as podesavanja]
            [nekretninice.konfiguracija.stilovi :as stilovi]
            [re-frame.core :as re-frame :refer [dispatch-sync]]
            [reagent.dom :as reagent-dom]
            [taoensso.timbre :as timbre]))

(defn montiraj-koren []
  (re-frame/clear-subscription-cache!)
  (let [aplikacija (j/call js/document :getElementById "aplikacija")]
    (reagent-dom/render (stilovi/provider {:main-view prikazi/glavno-podrucje}) aplikacija))
  (timbre/info "Verzija aplikacije:" podesavanja/verzija))

(defn ^:after-load ponovo-iznedri
  "Ponovo iznedri aplikaciju kada se figwheel učita"
  []
  (montiraj-koren))

(defn dev-mod []
  (when podesavanja/debug?
    (enable-console-print!)
    (println "dev mod")))

(defn ^:export pokreni []
  (dispatch-sync [::dogadjaji/pokreni])
  (dev-mod)
  (montiraj-koren))

