(ns nekretninice.rasclanjivaci
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [hickory.core :as h]
            [hickory.select :as hs]
            [java-time.api :as jt]
            [nekretninice.urlovi :as url]
            [nekretninice.util :as util]
            [taoensso.timbre :as log])
  (:import (java.time Instant ZoneId)))

(def broj-strana 2)

(defn dohvati-html [url]
  (try
    (:body (http/get url
             {:headers            {"User-Agent"      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                                   "Accept-Language" "sr-RS,sr;q=0.9,en-US;q=0.8"}
              :socket-timeout     12000
              :connection-timeout 8000
              :throw-exceptions   false}))
    (catch Exception e
      (log/warn "Greška pri dohvatanju URL-a:" url (.getMessage e))
      nil)))

(defn ocisti-tekst [s]
  (when s
    (-> (str s)
      (str/replace #"\s+" " ")
      str/trim)))

(defn parsiraj-cenu-eur [cena-str]
  (when (and cena-str (not (str/blank? cena-str)))
    (let [samo-cifre (-> cena-str
                       (str/replace #"\." "")
                       (str/replace #"," ".")
                       (as-> s (re-find #"\d+" s)))]
      (when samo-cifre
        (let [v (parse-double samo-cifre)]
          (when (and v (> v 1000) (< v 25000000))
            v))))))

(defn parsiraj-kvadraturu [tekst]
  (when tekst
    (when-let [[_ broj] (re-find #"(\d+(?:[\.,]\d+)?)\s*(?:m2|m²|kv)" (str/lower-case tekst))]
      (parse-double (str/replace broj "," ".")))))

(defn parsiraj-broj-soba [tekst]
  (when tekst
    (let [t (str/lower-case tekst)]
      (cond
        (str/includes? t "garsonjera") 0.5
        (str/includes? t "jednoiposoban") 1.5
        (str/includes? t "jednosoban") 1.0
        (str/includes? t "dvoiposoban") 2.5
        (str/includes? t "dvosoban") 2.0
        (str/includes? t "troiposoban") 3.5
        (str/includes? t "trosoban") 3.0
        (str/includes? t "četvorosoban") 4.0
        (str/includes? t "cetvorosoban") 4.0
        :else (when-let [[_ br] (re-find #"(\d+(?:[\.,]5)?)" t)]
                (parse-double (str/replace br "," ".")))))))

(defn izvori-lokaciju [mesto-sirovo]
  (let [delovi (->> (str/split (or mesto-sirovo "") #"[/,\-]")
                 (map ocisti-tekst)
                 (remove str/blank?)
                 vec)]
    {:grad          (first delovi)
     :opstina       (second delovi)
     :mikrolokacija (nth delovi 2 nil)
     :mesto_sirovo  (ocisti-tekst mesto-sirovo)}))

(defn detektuj-atribute-iz-teksta [naslov opis]
  (let [spojeno (str/lower-case (str naslov " " opis))]
    {:uknjizeno (boolean (re-find #"uknji[žz]en" spojeno))
     :oglasivac (cond
                  (re-find #"vlasnik|direktno od vlasnika|bez posrednika" spojeno) "vlasnik"
                  (re-find #"investitor" spojeno) "investitor"
                  :else "agencija")}))

(defn izvuci-prvi-url-iz-srcset [srcset-ili-src]
  (when (and srcset-ili-src (not (str/blank? srcset-ili-src)))
    (-> srcset-ili-src
      str/trim
      (str/split #",")
      first
      str/trim
      (str/split #"\s+")
      first)))

(defn datum-string->instant [^String datum ^String format]
  (try
    (let [formater        (jt/formatter format)
          local-date-time (.atStartOfDay (jt/local-date formater (str/trim datum)))]
      (jt/instant (jt/zoned-date-time local-date-time (ZoneId/systemDefault))))
    (catch Exception _ (Instant/now))))

(defn relativni-u-apsolutni [^String datum]
  (if (str/blank? datum)
    (Instant/now)
    (let [d (str/trim datum)]
      (or (condp re-find d
            #"[Dd]anas" (jt/instant)
            #"[Jj]u[čc]e" (jt/minus (jt/instant) (jt/days 1))
            #"pre nedelju dana" (jt/minus (jt/instant) (jt/weeks 1))
            #"pre mesec dana" (jt/minus (jt/instant) (jt/days 30))
            #"pre ([0-9]+) nedelj" :>> (fn [[_ br]] (jt/minus (jt/instant) (jt/weeks (parse-long br))))
            #"pre ([0-9]+) dan" :>> (fn [[_ br]] (jt/minus (jt/instant) (jt/days (parse-long br))))
            #"([0-9]+) sat" :>> (fn [[_ br]] (jt/minus (jt/instant) (jt/hours (parse-long br))))
            #"([0-9]+) minut" :>> (fn [[_ br]] (jt/minus (jt/instant) (jt/minutes (parse-long br))))
            nil)
        (Instant/now)))))

;; --- 1. KupujemProdajem ---
(defn obradi-kp-oglas [oglas {:keys [vrsta_ponude tip_nekretnine]}]
  (let [[{{href :href} :attrs}] (hs/select (hs/descendant (hs/tag :a)) oglas)
        puni-link     (when href
                        (if (str/starts-with? href "http")
                          href
                          (str (:KP url/imena-domena) href)))
        spoljni-id    (or (second (re-find #"/oglas/[^/]+/(\d+)" (or href "")))
                        (when puni-link (util/md5 puni-link)))
        [{[naslov] :content}] (hs/select (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :a) (hs/tag :div)) oglas)
        [{[opis] :content}] (hs/select (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :p)) oglas)
        [{[cena-sirova] :content}] (hs/select (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :div)) oglas)
        [{{slika :src} :attrs}] (hs/select (hs/descendant (hs/tag :img)) oglas)
        {[mesto] :content} (last (hs/select (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :p)) oglas))
        datum-sirov   (first (:content (last (hs/select (hs/child (hs/tag :div) (hs/tag :div) (hs/tag :div) (hs/tag :p)) oglas))))
        kp-obnovljen? (some? (seq (hs/select (hs/attr :aria-label #{"obnovljen"}) oglas)))
        naslov-cist   (ocisti-tekst naslov)
        opis-cist     (ocisti-tekst opis)]
    (when (and spoljni-id naslov-cist puni-link)
      (merge
        {:izvor            :KP
         :spoljni_id       spoljni-id
         :vrsta_ponude     vrsta_ponude
         :tip_nekretnine   tip_nekretnine
         :naslov           naslov-cist
         :opis             opis-cist
         :cena_eur         (parsiraj-cenu-eur cena-sirova)
         :kvadratura_m2    (or (parsiraj-kvadraturu naslov-cist) (parsiraj-kvadraturu opis-cist))
         :broj_soba        (or (parsiraj-broj-soba puni-link) (parsiraj-broj-soba naslov-cist))
         :link_oglasa      puni-link
         :link_fotografije slika
         :kp_obnovljen     kp-obnovljen?
         :objavljen_u      (relativni-u-apsolutni (str datum-sirov))}
        (izvori-lokaciju mesto)
        (detektuj-atribute-iz-teksta naslov-cist opis-cist)))))

;; --- 2. HaloOglasi ---
(defn obradi-halo-oglas [oglas {:keys [vrsta_ponude tip_nekretnine]}]
  (let [[{[naslov] :content {href :href} :attrs}] (hs/select (hs/child (hs/class :product-title) (hs/tag :a)) oglas)
        puni-link   (when href (str (:halo-oglasi url/imena-domena) href))
        spoljni-id  (or (second (re-find #"/(\d{8,})" (or href "")))
                      (when puni-link (util/md5 puni-link)))
        [{[{[cena-sirova] :content}] :content}] (hs/select (hs/attr :data-value) oglas)
        [{{slika :src} :attrs}] (hs/select (hs/descendant (hs/tag :figure) (hs/tag :img)) oglas)
        mesto       (str/join " / " (keep (comp ocisti-tekst first :content)
                                      (:content (first (hs/select (hs/class :subtitle-places) oglas)))))
        osobine     (mapv (comp ocisti-tekst first :content)
                      (hs/select (hs/descendant (hs/class :product-features) (hs/class :value-wrapper)) oglas))
        kvadratura  (parsiraj-kvadraturu (str/join " " osobine))
        broj-soba   (parsiraj-broj-soba (second osobine))
        sprat       (nth osobine 2 nil)
        [{[opis] :content}] (hs/select (hs/class :text-description-list) oglas)
        [{[datum-sirov] :content}] (hs/select (hs/class :publish-date) oglas)
        naslov-cist (ocisti-tekst naslov)
        opis-cist   (ocisti-tekst opis)]
    (when (and spoljni-id naslov-cist puni-link)
      (merge
        {:izvor            :halo-oglasi
         :spoljni_id       spoljni-id
         :vrsta_ponude     vrsta_ponude
         :tip_nekretnine   tip_nekretnine
         :naslov           naslov-cist
         :opis             opis-cist
         :cena_eur         (parsiraj-cenu-eur cena-sirova)
         :kvadratura_m2    kvadratura
         :broj_soba        broj-soba
         :sprat            sprat
         :link_oglasa      puni-link
         :link_fotografije slika
         :objavljen_u      (if datum-sirov (datum-string->instant datum-sirov "dd.MM.yyyy.") (Instant/now))}
        (izvori-lokaciju mesto)
        (detektuj-atribute-iz-teksta naslov-cist opis-cist)))))

;; --- 3. 4zida (HTML kartice + JSON-LD fallback) ---
(defn obradi-4zida-oglas [oglas {:keys [vrsta_ponude tip_nekretnine]}]
  (let [[{[naslov] :content}] (hs/select (hs/and (hs/class :desk:text-lg) (hs/tag :p)) oglas)
        [{{href :href} :attrs}] (hs/select (hs/tag :a) oglas)
        puni-link   (when href
                      (if (str/starts-with? href "http")
                        href
                        (str (:4zida url/imena-domena) href)))
        spoljni-id  (or (second (re-find #"/([a-f0-9]{20,24})(?:\?|$)" (or href "")))
                      (last (str/split (or href "") #"/")))
        [{[opis] :content}] (hs/select (hs/child (hs/and (hs/class :desk:block) (hs/class :desk:text-sm) (hs/tag :div))
                                         (hs/tag :span)) oglas)
        [{[cena-sirova] :content}] (hs/select (hs/and (hs/class :rounded-tl) (hs/tag :p)) oglas)
        [{{srcset :srcset src :src} :attrs}] (hs/select (hs/tag :img) oglas)
        slika       (or (izvuci-prvi-url-iz-srcset srcset) src)
        [{[mesto] :content}] (hs/select (hs/and (hs/class :line-clamp-2) (hs/class :!leading-tight)) oglas)
        naslov-cist (ocisti-tekst naslov)
        opis-cist   (ocisti-tekst opis)]
    (when (and spoljni-id naslov-cist puni-link)
      (merge
        {:izvor            :4zida
         :spoljni_id       spoljni-id
         :vrsta_ponude     vrsta_ponude
         :tip_nekretnine   tip_nekretnine
         :naslov           naslov-cist
         :opis             opis-cist
         :cena_eur         (parsiraj-cenu-eur cena-sirova)
         :kvadratura_m2    (or (parsiraj-kvadraturu naslov-cist) (parsiraj-kvadraturu opis-cist))
         :broj_soba        (or (parsiraj-broj-soba naslov-cist) (parsiraj-broj-soba opis-cist))
         :link_oglasa      puni-link
         :link_fotografije slika
         :objavljen_u      (Instant/now)}
        (izvori-lokaciju mesto)
        (detektuj-atribute-iz-teksta naslov-cist opis-cist)))))

(defn ustranici-url [izvor bazni-url strana]
  (if (= strana 1)
    bazni-url
    (let [param (if (= izvor :4zida) "strana" "page")
          sep   (if (str/includes? bazni-url "?") "&" "?")]
      (str bazni-url sep param "=" strana))))

(defn skrejpuj-izvor [{:keys [izvor urlovi] :as meta-izvora}]
  (mapcat
    (fn [bazni-url]
      (mapcat
        (fn [strana]
          (let [ciljni-url (ustranici-url izvor bazni-url strana)]
            (when-let [html (dohvati-html ciljni-url)]
              (let [stablo (h/as-hickory (h/parse html))]
                (case izvor
                  :KP
                  (->> (hs/select (hs/tag :article) stablo)
                    (remove #(str/includes? (str (get-in % [:attrs :class])) "CategoryPartner"))
                    (keep #(obradi-kp-oglas % meta-izvora)))

                  :halo-oglasi
                  (->> (hs/select (hs/and (hs/class :product-item) (hs/el-not (hs/class :banner-list))) stablo)
                    (keep #(obradi-halo-oglas % meta-izvora)))

                  :4zida
                  (->> (hs/select (hs/attr :test-data #{"ad-search-card"}) stablo)
                    (keep #(obradi-4zida-oglas % meta-izvora))))))))
        (range 1 (inc broj-strana))))
    urlovi))

(defn skrejpuj-sve-portale []
  (mapcat skrejpuj-izvor url/izvori-za-skrejpovanje))