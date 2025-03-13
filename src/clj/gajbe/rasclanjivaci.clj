(ns gajbe.rasclanjivaci
  (:require [clojure.string :as str]
            [gajbe.urlovi :as url]
            [gajbe.util :refer [-m]]
            [hickory.core :as h]
            [hickory.select :as hs]
            [java-time.api :as jt]
            [jsonista.core :as json])
  (:import (java.time ZoneId))
  (:gen-class))

(def broj-strana 1)

(defprotocol IzvuciOglase
  (dohvati-stranicu [this url] "prihvata url i vraća html stranicu")
  (izvuci-oglase [this] "uzima stranicu i vraca kolekciju mapa, od kojih je svaka jedan oglas")
  (obradi-oglas [this oglas] "uzima hickory podatke jednog oglasa i vraca njegove elemente")
  (ustranici-urlove [this broj-strana] "od jednog url-a pravi `broj-strana` urlova tako što uzima sledeću stranu"))

(defn datum-string->instant [^String datum ^String format]
  (let [formater        (jt/formatter format)
        local-date-time (.atStartOfDay (jt/local-date formater datum))]
    (jt/instant (jt/zoned-date-time local-date-time (ZoneId/systemDefault)))))

(defn- relativni-u-apsolutni
  [^String datum]
  (condp re-matches datum
    #"[Dd]anas" (jt/instant)
    #"[Jj]u[a-zA-Z\u00C0-\u024F]e" (jt/minus (jt/instant) (jt/days 1))
    #"pre nedelju dana" (jt/minus (jt/instant) (jt/weeks 1))
    #"pre mesec dana" (jt/minus (jt/instant) (jt/days 30))
    #"pre ([0-9]+) nedelj[a-z]" :>> (fn [[_ broj-nedelja]]
                                      (jt/minus (jt/instant) (jt/weeks (read-string broj-nedelja))))
    #"pre ([0-9]+) dana" :>> (fn [[_ broj-dana]]
                               (jt/minus (jt/instant) (jt/days (read-string broj-dana))))
    #"([0-9]+) dan[a-z]?[\s]+pre" :>> (fn [[_ broj-dana]]
                                        (jt/minus (jt/instant) (jt/days (read-string broj-dana))))
    #"([0-9]+) sat[a-z]?[\s]+pre" :>> (fn [[_ broj-sati]]
                                        (jt/minus (jt/instant) (jt/hours (read-string broj-sati))))
    #"([0-9]+) minut[a-z]?[\s]+pre" :>> (fn [[_ broj-minuta]]
                                          (jt/minus (jt/instant) (jt/minutes (read-string broj-minuta))))
    #"([a-zA-Z]+) ([0-9]+), ([0-9]+)" :>> (fn [[_ mesec dan godina]] ;; e.g. Mar 21, 2023
                                            (datum-string->instant
                                              (str/join "/" [godina mesec (min (inc (parse-long dan)) 31)])
                                              "yyyy/MMM/d"))))

(deftype ProcesorKP [urlovi imena-domena]
  IzvuciOglase
  (ustranici-urlove [_this broj-strana]
    (mapcat (fn [url]
              (conj
                (map (fn [strana]
                       (str url "&page=" strana))
                  (range 2 (inc broj-strana)))
                url))
      (:KP urlovi)))
  (dohvati-stranicu [_this url]
    (slurp url))
  (izvuci-oglase [this]
    (let [oglasi (remove (fn [oglas]
                           (str/includes? (get-in oglas [:attrs :class]) "CategoryPartner"))
                   (mapcat (fn [url]
                             (hs/select (hs/tag :article) (h/as-hickory (h/parse (dohvati-stranicu this url)))))
                     (ustranici-urlove this broj-strana)))]
      (sequence (keep (partial obradi-oglas this)) oglasi)))
  (obradi-oglas [_this oglas]
    (let [[{{link-oglasa :href} :attrs}] (hs/select
                                           (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :a))
                                           oglas)
          link-oglasa   (str (:KP imena-domena) link-oglasa)
          [{[naslov] :content}] (hs/select
                                  (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :a) (hs/tag :div))
                                  oglas)
          [{[opis] :content}] (hs/select
                                (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :p))
                                oglas)
          [{[cena] :content}] (hs/select (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :div)) oglas)
          [{{link-fotografije :src} :attrs}] (hs/select (hs/child (hs/tag :a) (hs/tag :div) (hs/tag :img)) oglas)
          {[mesto] :content} (last (hs/select
                                     (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :p))
                                     oglas))
          datum         (when-some [datum-sirov (first (:content (last
                                                                   (hs/select
                                                                     (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div)
                                                                       (hs/tag :p))
                                                                     oglas))))]
                          (relativni-u-apsolutni datum-sirov))
          kp-obnovljen? (some? (first (:content (first (hs/select
                                                         (hs/and
                                                           (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :a))
                                                           (hs/attr :aria-label #{"obnovljen"}))
                                                         oglas)))))
          izvor         :KP]
      ;; KP je ponekad nedostupan pa je bacao NPE pa radim nil proveru za datum i za druga polja
      (when (and (some? datum) (some? link-oglasa) (some? naslov))
        (-m link-oglasa naslov opis cena link-fotografije mesto datum kp-obnovljen? izvor)))))

(deftype ProcesorHaloOglasi [urlovi imena-domena]
  IzvuciOglase
  (ustranici-urlove [_this broj-strana]
    (mapcat (fn [url]
              (conj
                (map (fn [strana]
                       (str url "&page=" strana))
                  (range 2 (inc broj-strana)))
                url))
      (:halo-oglasi urlovi)))
  (dohvati-stranicu [_this url]
    (slurp url))
  (izvuci-oglase [this]
    (let [oglasi (mapcat (fn [url]
                           (hs/select (hs/and (hs/class :product-item) (hs/el-not (hs/class :banner-list)))
                             (h/as-hickory (h/parse (dohvati-stranicu this url)))))
                   (ustranici-urlove this broj-strana))]
      (map (partial obradi-oglas this) oglasi)))
  (obradi-oglas [_this oglas]
    (let [[{[naslov] :content {link-oglasa :href} :attrs}] (hs/select (hs/child (hs/class :product-title) (hs/tag :a))
                                                             oglas)
          link-oglasa (str (:halo-oglasi imena-domena) link-oglasa)
          [{[{[cena] :content}] :content}] (hs/select (hs/attr :data-value) oglas)
          [{{link-fotografije :src} :attrs}] (hs/select (hs/descendant (hs/tag :figure) (hs/tag :img)) oglas)
          mesto       (str/join "/" (map (comp first :content)
                                      (:content (first (hs/select (hs/class :subtitle-places) oglas)))))
          [tip kvadratura broj-soba] (map (comp first :content)
                                       (hs/select (hs/descendant (hs/class :product-features) (hs/class :value-wrapper))
                                         oglas))
          [{[datum] :content}] (hs/select (hs/class :publish-date) oglas)
          datum       (datum-string->instant datum "dd.MM.yyyy.")
          izvor       :halo-oglasi]
      (-m link-oglasa naslov cena link-fotografije mesto tip kvadratura broj-soba datum izvor))))

(deftype ProcesorBeogradskiOglasi [urlovi imena-domena]
  IzvuciOglase
  (ustranici-urlove [_this broj-strana]
    (mapcat (fn [url]
              (conj
                (map (fn [strana]
                       (str url "&page=" strana))
                  (range 2 (inc broj-strana)))
                url))
      (:beogradski-oglasi urlovi)))
  (dohvati-stranicu [_this url]
    (slurp url))
  (izvuci-oglase [this]
    (let [oglasi (mapcat (fn [url]
                           (hs/select (hs/class :classified) (h/as-hickory (h/parse (dohvati-stranicu this url)))))
                   (ustranici-urlove this broj-strana))]
      (map (partial obradi-oglas this) oglasi)))
  (obradi-oglas [_this oglas]
    (let [[{[naslov] :content}] (hs/select (hs/child (hs/class :title) (hs/tag :h3)) oglas)
          [{{link-oglasa :href} :attrs}] (hs/select (hs/child (hs/class :classified) (hs/tag :a)) oglas)
          link-oglasa (str (:beogradski-oglasi imena-domena) link-oglasa)
          [{[opis] :content}] (hs/select (hs/child (hs/class :fbac) (hs/tag :p)) oglas)
          [{[cena] :content}] (hs/select (hs/class :sl-price) oglas)
          cena        (str/trim cena)
          [{{link-fotografije :src} :attrs}] (hs/select (hs/class :cpic) oglas)
          [{[mesto] :content}] (hs/select (hs/class :sl-loc) oglas)
          [{[datum] :content}] (hs/select (hs/child (hs/class :fbac) (hs/class :small-light)) oglas)
          datum       (relativni-u-apsolutni (str/trim (second (str/split datum #" "))))
          izvor       :beogradski-oglasi]
      (-m link-oglasa naslov opis cena link-fotografije mesto datum izvor))))

(deftype Procesor4zida [urlovi imena-domena]
  IzvuciOglase
  (ustranici-urlove [_this broj-strana]
    (mapcat (fn [url]
              (conj
                (map (fn [strana]
                       (str url "&strana=" strana))
                  (range 2 (inc broj-strana)))
                url))
      (:4zida urlovi)))
  (dohvati-stranicu [_this url]
    (slurp url))
  (izvuci-oglase [this]
    (let [oglasi (mapcat (fn [url]
                           (hs/select (hs/attr :test-data #{"ad-search-card"})
                             (h/as-hickory (h/parse (dohvati-stranicu this url)))))
                   (ustranici-urlove this broj-strana))]
      (map (partial obradi-oglas this) oglasi)))
  (obradi-oglas [_this oglas]
    (let [[{[naslov] :content}] (hs/select (hs/and (hs/class :desk:text-lg) (hs/tag :p)) oglas)
          [{{link-oglasa :href} :attrs}] (hs/select (hs/and (hs/class :justify-between) (hs/class :gap-1)
                                                      (hs/class :flex) (hs/tag :a)) oglas)
          link-oglasa (str (:4zida imena-domena) link-oglasa)
          [{[opis] :content}] (hs/select (hs/child (hs/and (hs/class :desk:block) (hs/class :desk:text-sm) (hs/tag :div))
                                           (hs/tag :span)) oglas)
          [{[cena] :content}] (hs/select (hs/and (hs/class :rounded-tl) (hs/tag :p)) oglas)
          cena        (str/trim cena)
          [{{link-fotografije :srcset} :attrs}] (hs/select (hs/child (hs/and (hs/class :relative) (hs/class :size-full)
                                                                       (hs/class "bg-black/50")) (hs/tag :img)) oglas)
          [{[mesto] :content}] (hs/select (hs/and (hs/class :line-clamp-2) (hs/class :desk:line-clamp-3)
                                            (hs/class :!leading-tight)) oglas)
          datum       (jt/instant)
          izvor       :4zida]
      (-m link-oglasa naslov opis cena link-fotografije mesto datum izvor))))

(defn dohvati-oglase []
  (sort-by :datum jt/after?
    (mapcat izvuci-oglase
      [(->ProcesorBeogradskiOglasi url/urlovi-oglasa-kuce url/imena-domena)
       (->ProcesorHaloOglasi url/urlovi-oglasa-kuce url/imena-domena)
       (->ProcesorKP url/urlovi-oglasa-kuce url/imena-domena)
       (->Procesor4zida url/urlovi-oglasa-kuce url/imena-domena)])))

(defn dohvati-oglase-stanova []
  (sort-by :datum jt/after?
    (mapcat izvuci-oglase
      [(->ProcesorBeogradskiOglasi url/urlovi-oglasa-stanovi url/imena-domena)
       (->ProcesorHaloOglasi url/urlovi-oglasa-stanovi url/imena-domena)
       (->ProcesorKP url/urlovi-oglasa-stanovi url/imena-domena)
       (->Procesor4zida url/urlovi-oglasa-stanovi url/imena-domena)])))

(defn dohvati-oglase-vikendica []
  (sort-by :datum jt/after?
    (mapcat izvuci-oglase
      [(->ProcesorBeogradskiOglasi url/urlovi-oglasa-vikendice url/imena-domena)
       (->ProcesorHaloOglasi url/urlovi-oglasa-vikendice url/imena-domena)
       (->ProcesorKP url/urlovi-oglasa-vikendice url/imena-domena)
       (->Procesor4zida url/urlovi-oglasa-vikendice url/imena-domena)])))

(defn dohvati-oglase-stanova-za-izdavanje []
  (sort-by :datum jt/after?
    (mapcat izvuci-oglase
      [(->ProcesorBeogradskiOglasi url/urlovi-oglasa-stanova-za-izdavanje url/imena-domena)
       (->ProcesorHaloOglasi url/urlovi-oglasa-stanova-za-izdavanje url/imena-domena)
       (->ProcesorKP url/urlovi-oglasa-stanova-za-izdavanje url/imena-domena)
       (->Procesor4zida url/urlovi-oglasa-stanova-za-izdavanje url/imena-domena)])))

(comment
  (count (dohvati-oglase))
  (slurp "https://www.4zida.rs/prodaja-kuca/beograd?skuplje_od=50000eur&jeftinije_od=151000eur&vece_od=80m2&mesto=stara-pazova")
  (ustranici-urlove (->ProcesorKP url/urlovi-oglasa-kuce url/imena-domena) 10)
  (let [xform  (comp
                 (map (fn [element]
                        (update element :content (fn [[content]]
                                                   (json/read-value content json/keyword-keys-object-mapper)))))
                 (filter (comp #{"RealEstateListing"} :additionalType :content))
                 (map (fn [{:keys [content]}]
                        ((juxt :name :url :image) content))))
        oglasi (sequence xform (hs/select (hs/and (hs/tag :script) (hs/attr :type #{"application/ld+json"}))
                                 (h/as-hickory (h/parse (slurp "https://www.4zida.rs/prodaja-kuca/beograd?skuplje_od=50000eur&jeftinije_od=151000eur&vece_od=80m2&mesto=stara-pazova&strana=2")))))]
    oglasi))
