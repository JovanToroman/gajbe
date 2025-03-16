(ns nekretninice.konfiguracija.dogadjaji
  (:require [nekretninice.aplikacija.baza-podataka :as bp]
            [nekretninice.aplikacija.putanje :as putanje]
            [nekretninice.konfiguracija.pretplate :as pretplate]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]
            [re-frame.core :refer [dispatch reg-event-db subscribe]]
            [reitit.coercion :as coercion]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]))

;; region putanje
(defonce router (rf/router putanje/putanje {;; `:compile` čini putajnik bržim
                                            :compile coercion/compile-request-coercers}))

(defn pokreni-putanje! []
  (rfe/start!
    router
    (fn [putanja]
      (let [stara-putanja @(subscribe [::pretplate/aktivna-putanja])]
        (dispatch [::podesi-aktivnu-putanju
                   (assoc putanja :controllers (rfc/apply-controllers (:controllers stara-putanja) putanja))])))
    {:use-fragment false}))

(reg-event-db ::pokreni
  (fn [_ _]
    (pokreni-putanje!)
    bp/podrazumevana-baza-podataka))

(reg-event-db
  ::podesi-aktivnu-putanju
  (fn [bp [_ aktivno-podrucje]]
    (let [stara-putanja (get bp :aktivna-putanja)]
      (assoc bp
        :stara-putanja stara-putanja
        :aktivna-putanja aktivno-podrucje))))
;; endregion