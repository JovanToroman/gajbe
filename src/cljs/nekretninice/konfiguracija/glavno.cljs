(ns nekretninice.konfiguracija.glavno
  (:require ["react-dom/client" :as rdom]
            [nekretninice.aplikacija.prikazi :as prikazi]
            [nekretninice.konfiguracija.dogadjaji]
            [nekretninice.konfiguracija.pretplate]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defonce koren-atom (atom nil))

(defn ponovo-iznedri []
  (rf/clear-subscription-cache!)
  (when-let [el (.getElementById js/document "aplikacija")]
    (when-not @koren-atom
      (reset! koren-atom (rdom/createRoot el)))
    (.render @koren-atom (r/as-element [prikazi/glavni-prikaz]))))

(defn ^:export iznedri []
  (rf/dispatch-sync [:inicijalizuj-bazu])
  (rf/dispatch [:dohvati-oglase])
  (ponovo-iznedri))

(defonce pokretanje (iznedri))