(ns nekretninice.konfiguracija.pretplate
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-sub trim-v]]))

(reg-sub ::aktivna-putanja
  (fn [bp _]
    (:aktivna-putanja bp)))
