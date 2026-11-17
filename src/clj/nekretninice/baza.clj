(ns nekretninice.baza
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [honey.sql :as sql]
            [jsonista.core :as json]
            [nekretninice.util :as util]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [next.jdbc.result-set :as rs]
            [taoensso.timbre :as log])
  (:import (com.zaxxer.hikari HikariDataSource)
           (java.sql Timestamp)
           (java.time Instant)
           (org.postgresql.util PGobject)))

(defonce ^:private ds-atom (atom nil))

(def db-spec
  {:dbtype   "postgresql"
   :host     (or (System/getenv "DB_HOST") "localhost")
   :port     (parse-long (or (System/getenv "DB_PORT") "5432"))
   :dbname   (or (System/getenv "DB_NAME") "nekretninice")
   :username (or (System/getenv "DB_USER") "postgres")
   :password (or (System/getenv "DB_PASS") "postgres")})

(defn ds []
  (or @ds-atom
    (reset! ds-atom (connection/->pool HikariDataSource db-spec))))

(defn inicijalizuj-bazu! []
  (log/info "Inicijalizacija PostgreSQL šeme...")
  (let [sema (slurp (io/resource "sql/sema.sql"))]
    (jdbc/execute! (ds) [sema])))

(defn ->pg-jsonb [v]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (json/write-value-as-string v))))

(defn <-pg-jsonb [^PGobject obj]
  (when obj
    (json/read-value (.getValue obj) json/keyword-keys-object-mapper)))

(defn izracunaj-otisak
  "Pravi otisak nekretnine za grupisanje istog stana/kuće na više portala."
  [{:keys [tip_nekretnine opstina kvadratura_m2 cena_eur broj_soba]}]
  (when (and opstina kvadratura_m2 cena_eur)
    (let [kv-zaokruzeno   (* 2 (Math/round (/ (double kvadratura_m2) 2.0)))
          cena-zaokruzena (* 2000 (Math/round (/ (double cena_eur) 2000.0)))]
      (util/md5 (str/lower-case (str tip_nekretnine "|" opstina "|" kv-zaokruzeno "|" cena-zaokruzena "|" broj_soba))))))

(defn sacuvaj-oglas!
  "Upsertuje oglas. Ako se cena promenila, upisuje u istorija_cena. Vraća :novi, :azuriran ili :nepromenjen."
  [oglas]
  (jdbc/with-transaction [tx (ds)]
    (let [otisak    (izracunaj-otisak oglas)
          cena-m2   (when (and (:cena_eur oglas) (:kvadratura_m2 oglas) (pos? (:kvadratura_m2 oglas)))
                      (Math/round (/ (double (:cena_eur oglas)) (double (:kvadratura_m2 oglas)))))
          postojeci (jdbc/execute-one! tx
                      ["SELECT id, cena_eur FROM oglasi WHERE izvor = ? AND spoljni_id = ?"
                       (name (:izvor oglas)) (str (:spoljni_id oglas))]
                      {:builder-fn rs/as-unqualified-lower-maps})]
      (if postojeci
        (let [stara-cena (some-> (:cena_eur postojeci) double)
              nova-cena  (some-> (:cena_eur oglas) double)
              promena?   (and stara-cena nova-cena (not= stara-cena nova-cena))]
          (jdbc/execute-one! tx
            ["UPDATE oglasi SET
                naslov = ?, opis = ?, cena_eur = COALESCE(?, cena_eur),
                kvadratura_m2 = COALESCE(?, kvadratura_m2), cena_po_m2 = COALESCE(?, cena_po_m2),
                link_fotografije = COALESCE(?, link_fotografije),
                kp_obnovljen = ?, poslednji_put_vidjen = NOW(), aktivan = TRUE
              WHERE id = ?"
             (:naslov oglas) (:opis oglas) nova-cena (:kvadratura_m2 oglas) cena-m2
             (:link_fotografije oglas) (boolean (:kp_obnovljen oglas)) (:id postojeci)])
          (when promena?
            (jdbc/execute-one! tx
              ["INSERT INTO istorija_cena (oglas_id, stara_cena_eur, nova_cena_eur) VALUES (?, ?, ?)"
               (:id postojeci) stara-cena nova-cena]))
          (if promena? :azuriran :nepromenjen))
        (let [novi (jdbc/execute-one! tx
                     ["INSERT INTO oglasi
                        (izvor, spoljni_id, vrsta_ponude, tip_nekretnine, naslov, opis,
                         cena_eur, kvadratura_m2, cena_po_m2, broj_soba, sprat, grejanje,
                         uknjizeno, oglasivac, grad, opstina, mikrolokacija, mesto_sirovo,
                         link_oglasa, link_fotografije, kp_obnovljen, otisak_nekretnine, objavljen_u)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                       RETURNING id"
                      (name (:izvor oglas))
                      (str (:spoljni_id oglas))
                      (or (:vrsta_ponude oglas) "prodaja")
                      (or (:tip_nekretnine oglas) "stan")
                      (:naslov oglas)
                      (:opis oglas)
                      (:cena_eur oglas)
                      (:kvadratura_m2 oglas)
                      cena-m2
                      (:broj_soba oglas)
                      (:sprat oglas)
                      (:grejanje oglas)
                      (:uknjizeno oglas)
                      (:oglasivac oglas)
                      (:grad oglas)
                      (:opstina oglas)
                      (:mikrolokacija oglas)
                      (:mesto_sirovo oglas)
                      (:link_oglasa oglas)
                      (:link_fotografije oglas)
                      (boolean (:kp_obnovljen oglas))
                      otisak
                      (Timestamp/from (or (:objavljen_u oglas) (Instant/now)))]
                     {:builder-fn rs/as-unqualified-lower-maps})]
          (when (:cena_eur oglas)
            (jdbc/execute-one! tx
              ["INSERT INTO istorija_cena (oglas_id, stara_cena_eur, nova_cena_eur) VALUES (?, NULL, ?)"
               (:id novi) (:cena_eur oglas)]))
          :novi)))))

(defn pretrazi-oglase
  [{:keys [vrsta_ponude tip_nekretnine izvor q lokacija cena_od cena_do
           kvadratura_od kvadratura_do broj_soba_od samo_vlasnik samo_uknjizeno
           samo_snizeno sortiranje strana po_strani korisnik_id]
    :or   {vrsta_ponude "prodaja" strana 1 po_strani 24 sortiranje "najnovije"}}]
  (let [offset       (* (dec (max 1 (long strana))) (long po_strani))
        uslovi       (cond-> [[:= :o.aktivan true]
                              [:= :o.vrsta_ponude vrsta_ponude]]
                       (and tip_nekretnine (not (str/blank? tip_nekretnine)) (not= tip_nekretnine "sve"))
                       (conj [:= :o.tip_nekretnine tip_nekretnine])

                       (and izvor (not (str/blank? izvor)) (not= izvor "svi"))
                       (conj [:= :o.izvor izvor])

                       (and cena_od (pos? cena_od))
                       (conj [:>= :o.cena_eur cena_od])

                       (and cena_do (pos? cena_do))
                       (conj [:<= :o.cena_eur cena_do])

                       (and kvadratura_od (pos? kvadratura_od))
                       (conj [:>= :o.kvadratura_m2 kvadratura_od])

                       (and kvadratura_do (pos? kvadratura_do))
                       (conj [:<= :o.kvadratura_m2 kvadratura_do])

                       (and broj_soba_od (pos? broj_soba_od))
                       (conj [:>= :o.broj_soba broj_soba_od])

                       samo_vlasnik
                       (conj [:= :o.oglasivac "vlasnik"])

                       samo_uknjizeno
                       (conj [:= :o.uknjizeno true])

                       (and lokacija (not (str/blank? lokacija)))
                       (conj [:or
                              [:ilike :o.mesto_sirovo (str "%" lokacija "%")]
                              [:ilike :o.opstina (str "%" lokacija "%")]
                              [:ilike :o.mikrolokacija (str "%" lokacija "%")]])

                       (and q (not (str/blank? q)))
                       (conj [:or
                              [:ilike :o.naslov (str "%" q "%")]
                              [:ilike :o.opis (str "%" q "%")]]))
        order-clause (case sortiranje
                       "cena_rastuce" [[:o.cena_eur :asc :nulls-last]]
                       "cena_opadajuce" [[:o.cena_eur :desc :nulls-last]]
                       "m2_najpovoljnije" [[:o.cena_po_m2 :asc :nulls-last]]
                       [[:o.objavljen_u :desc]])
        upit-mapa    {:select   [:o.*
                                 [[:raw "(SELECT stara_cena_eur FROM istorija_cena ic WHERE ic.oglas_id = o.id AND ic.stara_cena_eur IS NOT NULL ORDER BY ic.zabelezeno_u DESC LIMIT 1)"] :prethodna_cena_eur]
                                 [[:raw "ROUND(AVG(o.cena_po_m2) OVER (PARTITION BY o.tip_nekretnine, COALESCE(o.opstina, o.grad)))"] :prosek_kraja_m2]
                                 [[:raw "(SELECT COUNT(*) FROM oglasi o2 WHERE o2.otisak_nekretnine = o.otisak_nekretnine AND o.otisak_nekretnine IS NOT NULL)"] :broj_portala]
                                 (if korisnik_id
                                   [[:raw (str "EXISTS(SELECT 1 FROM sacuvani_oglasi so WHERE so.oglas_id = o.id AND so.korisnik_id = '" korisnik_id "')")] :sacuvan]
                                   [[:inline false] :sacuvan])]
                      :from     [[:oglasi :o]]
                      :where    (into [:and] uslovi)
                      :order-by order-clause
                      :limit    po_strani
                      :offset   offset}
        ukupno-mapa  {:select [[[:count :*] :ukupno]]
                      :from   [[:oglasi :o]]
                      :where  (into [:and] uslovi)}
        rezultati    (jdbc/execute! (ds) (sql/format upit-mapa)
                       {:builder-fn rs/as-unqualified-lower-maps})
        ukupno       (:ukupno (jdbc/execute-one! (ds) (sql/format ukupno-mapa)
                                {:builder-fn rs/as-unqualified-lower-maps}))]
    {:oglasi    (if samo_snizeno
                  (filter #(and (:prethodna_cena_eur %) (< (:cena_eur %) (:prethodna_cena_eur %))) rezultati)
                  rezultati)
     :ukupno    (or ukupno 0)
     :strana    strana
     :po_strani po_strani}))