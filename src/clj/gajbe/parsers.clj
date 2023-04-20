(ns gajbe.parsers
  (:require [clojure.string :as str]
            [hickory.core :as h]
            [hickory.select :as hs]
            [gajbe.urlovi :as url]
            [gajbe.util :refer [-m]]
            [java-time.api :as jt])
  (:import (java.time ZoneId)))

(defprotocol IzvuciOglase
  (dohvati-stranicu [this] "prihvata spisak svih urlova i vraća html stranicu")
  (izvuci-oglase [this] "uzima stranicu i vraca kolekciju mapa, od kojih je svaka jedan oglas")
  (obradi-oglas [this oglas] "uzima hickory podatke jednog oglasa i vraca njegove elemente"))

(defn- relativni-u-apsolutni
  [^String datum]
  (condp re-matches datum
    #"danas" (jt/instant)
    #"ju[a-zA-Z\u00C0-\u024F]e" (jt/minus (jt/instant) (jt/days 1))
    #"pre nedelju dana" (jt/minus (jt/instant) (jt/weeks 1))
    #"pre ([0-9]+) nedelj[a-z]" :>> (fn [[_ broj-nedelja]]
                                      (jt/minus (jt/instant) (jt/weeks (read-string broj-nedelja))))
    #"pre ([0-9]+) dana" :>> (fn [[_ broj-dana]]
                               (jt/minus (jt/instant) (jt/days (read-string broj-dana))))
    #"([0-9]+) dan[a-z][\s]+pre" :>> (fn [[_ broj-dana]]
                                       (jt/minus (jt/instant) (jt/days (read-string broj-dana))))
    #"([0-9]+) sat[a-z][\s]+pre" :>> (fn [[_ broj-sati]]
                                       (jt/minus (jt/instant) (jt/hours (read-string broj-sati))))
    #"([0-9]+) minut[a-z][\s]+pre" :>> (fn [[_ broj-minuta]]
                                         (jt/minus (jt/instant) (jt/minutes (read-string broj-minuta))))
    #"([a-zA-Z]+) ([0-9]+), ([0-9]+)" :>> (fn [[_ mesec dan godina]] ;; e.g. Mar 21, 2023
                                            (let [formatter (jt/formatter "yyyy/MMM/d")
                                                  date-string (str/join "/" [godina mesec (inc (read-string dan))])
                                                  local-date-time (.atStartOfDay (jt/local-date formatter date-string))]
                                              (jt/instant (jt/zoned-date-time local-date-time (ZoneId/systemDefault)))))))

(comment
  (relativni-u-apsolutni "8 sati pre"))

(deftype ProcesorKP [urlovi]
  IzvuciOglase
  (dohvati-stranicu [_this]
    (slurp (first (:KP urlovi))))
  (izvuci-oglase [this]
    (let [oglasi (hs/select (hs/tag :article) (h/as-hickory (h/parse (dohvati-stranicu this))))]
      (map (partial obradi-oglas this) oglasi)))
  (obradi-oglas [_this oglas]
    (let [[{[naslov] :content}] (hs/select (hs/class :AdItem_name__RhGAZ) oglas)
          [{[opis] :content}] (hs/select (hs/child (hs/class :AdItem_adTextHolder__Fmra9) (hs/tag :p)) oglas)
          [{[cena] :content}] (hs/select (hs/class :AdItem_price__jUgxi) oglas)
          [{{link-fotografije :src} :attrs}] (hs/select (hs/child (hs/class :AdItem_imageHolder__LZaKa) (hs/tag :img))
                                               oglas)
          [{[mesto] :content}] (hs/select (hs/child (hs/class :AdItem_originAndPromoLocation__HgtYj) (hs/tag :p)) oglas)
          datum (relativni-u-apsolutni
                  (first (:content (last
                                     (hs/select (hs/child (hs/class :AdItem_postedStatus__swUhG)
                                                  (hs/tag :p)) oglas)))))
          kp-obnovljen? (some? (first (:content (first (hs/select (hs/child (hs/class :AdItem_postedStatus__swUhG)
                                                                    (hs/tag :a)) oglas)))))]
      (-m naslov opis cena link-fotografije mesto datum kp-obnovljen?))))

(deftype ProcesorHaloOglasi [urlovi]
  IzvuciOglase
  (dohvati-stranicu [_this]
    (slurp (first (:halo-oglasi urlovi))))
  (izvuci-oglase [this]
    (let [oglasi (hs/select (hs/class :product-item) (h/as-hickory (h/parse (dohvati-stranicu this))))]
      (map (partial obradi-oglas this) oglasi)))
  (obradi-oglas [_this oglas]
    (let [[{[naslov] :content}] (hs/select (hs/child (hs/class :product-title) (hs/tag :a)) oglas)
          [{[{[cena] :content}] :content}] (hs/select (hs/attr :data-value) oglas)
          [{{link-fotografije :src} :attrs}] (hs/select (hs/descendant (hs/tag :figure) (hs/tag :img)) oglas)
          mesto (str/join "/" (map (comp first :content)
                                (:content (first (hs/select (hs/class :subtitle-places) oglas)))))
          [tip kvadratura broj-soba] (map (comp first :content)
                                       (hs/select (hs/descendant (hs/class :product-features) (hs/class :value-wrapper)) oglas))
          [{[datum] :content}] (hs/select (hs/class :publish-date) oglas)]
      (-m naslov cena link-fotografije mesto tip kvadratura broj-soba datum))))

(deftype ProcesorBeogradskiOglasi [urlovi]
  IzvuciOglase
  (dohvati-stranicu [_this]
    (slurp (first (:beogradski-oglasi urlovi))))
  (izvuci-oglase [this]
    (let [oglasi (hs/select (hs/class :classified) (h/as-hickory (h/parse (dohvati-stranicu this))))]
      (map (partial obradi-oglas this) oglasi)))
  (obradi-oglas [_this oglas]
    (let [[{[naslov] :content}] (hs/select (hs/child (hs/class :title) (hs/tag :h3)) oglas)
          [{[opis] :content}] (hs/select (hs/child (hs/class :fbac) (hs/tag :p)) oglas)
          [{[cena] :content}] (hs/select (hs/class :sl-price) oglas)
          cena (str/trim cena)
          [{{link-fotografije :src} :attrs}] (hs/select (hs/class :cpic) oglas)
          [{[mesto] :content}] (hs/select (hs/class :sl-loc) oglas)
          [{[datum] :content}] (hs/select (hs/child (hs/class :fbac) (hs/class :small-light)) oglas)
          datum (relativni-u-apsolutni (str/trim (second (str/split datum #" "))))
          kp-obnovljen? (some? (first (:content (first (hs/select (hs/child (hs/class :AdItem_postedStatus__swUhG)
                                                                    (hs/tag :a)) oglas)))))]
      (-m naslov opis cena link-fotografije mesto datum kp-obnovljen?))))

(comment
  (mapcat izvuci-oglase [(->ProcesorBeogradskiOglasi url/urlovi-oglasa) (->ProcesorHaloOglasi url/urlovi-oglasa)
                      (->ProcesorKP url/urlovi-oglasa)]))
