(ns nekretninice.konfiguracija.pretplate
  (:require [re-frame.core :as rf]))

(rf/reg-sub :oglasi (fn [db _] (:oglasi db)))
(rf/reg-sub :ukupno-oglasa (fn [db _] (:ukupno-oglasa db)))
(rf/reg-sub :ucitavanje? (fn [db _] (:ucitavanje? db)))
(rf/reg-sub :filteri (fn [db _] (:filteri db)))
(rf/reg-sub :korisnik (fn [db _] (:korisnik db)))
(rf/reg-sub :aktivni-prikaz (fn [db _] (:aktivni-prikaz db)))
(rf/reg-sub :auth-modal (fn [db _] (:auth-modal db)))
(rf/reg-sub :obavestenje (fn [db _] (:obavestenje db)))
(rf/reg-sub :sacuvani-oglasi (fn [db _] (:sacuvani-oglasi db)))
(rf/reg-sub :alarmi (fn [db _] (:alarmi db)))