(ns nekretninice.aplikacija.prikazi
  (:require [cuerdas.core :as str]
            [nekretninice.konfiguracija.pretplate :as pretplate]
            [nekretninice.konfiguracija.stilovi :as stilovi]
            [re-frame.core :refer [subscribe]]))

(defn prikaz-jednog-oglasa
  [{:keys [cena datum opis kp-obnovljen? broj-soba tip kvadratura naslov link-fotografije mesto link-oglasa]}]
  [:div.row.col-12.p-3.mb-5
   [:img.col-3.img-thumbnail {:src link-fotografije :height 200 :width 300}]
   [:div.col-7
    [:a {:href link-oglasa :target "_blank"} [:h3 naslov]]
    [:span.row.col-12.text-secondary.mt-2 mesto]
    [:p.row.col-12.mt-2 opis]
    [:span.row.col-12.text-secondary.font-italic datum]
    (when (or broj-soba tip kvadratura)
      [:div.row
       [:span.col-sm "Broj soba: " broj-soba]
       [:span.col-sm "Tip: " tip]
       [:span.col-sm "Kvadratura: " kvadratura]])]
   [:span.col-2
    [:span.bg-primary.h-25.h3 (if (not (str/blank? cena)) cena "bez cene")]
    (when kp-obnovljen?
      [:h5 "Obnovljen"])]])

(defn prikaz-oglasa [oglasi]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/bootstrap@4.0.0/dist/css/bootstrap.min.css"}]]
   [:body
    (into [:div.container.mt-5]
      (map prikaz-jednog-oglasa oglasi))
    [:script {:src "https://code.jquery.com/jquery-3.2.1.slim.min.js"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/popper.js@1.12.9/dist/umd/popper.min.js"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/bootstrap@4.0.0/dist/js/bootstrap.min.js"}]]])

(defn pocetna []
  [:div
   [:h1 "Zdravo, ja sam pocetna"]])

(defn- podrucja [panel-name]
  (case panel-name
    :putanja/pocetna [pocetna]
    [:div]))

(defn glavno-podrucje []
  (let [{{naziv-podrucja :name} :data} @(subscribe [::pretplate/aktivna-putanja])
        [css] (stilovi/koristi-styletron)]
    [:div.container
     [:nav.navbar.navbar-expand-lg.navbar-light
      [:a.navbar-brand {:href "/"} "Nekretninice"]]
     [:div.d-flex.flex-column.justify-content-center.align-items-center
      {:className (css {:min-height "80vh"})}
      [podrucja naziv-podrucja]]
     [:p.copyright-text.mt-5.d-flex.justify-content-center
      "Copyright © 2022-2025 Sva prava zadržana od strane Do Brave Plus Software"]]))
