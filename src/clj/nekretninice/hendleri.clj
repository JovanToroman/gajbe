(ns nekretninice.hendleri
  (:require [clojure.java.io :as io]
            [nekretninice.autentifikacija :as auth]
            [nekretninice.baza :as baza]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [ring.util.http-response :as r])
  (:import (java.util UUID)))

(defn pocetna-stranica [_]
  (-> (r/ok (slurp (io/resource "javno/index.html")))
    (r/content-type "text/html; charset=utf-8")))

(defn api-pretrazi-oglase [{:keys [params korisnik]}]
  (let [parsirani {:vrsta_ponude   (get params :vrsta_ponude "prodaja")
                   :tip_nekretnine (get params :tip_nekretnine "sve")
                   :izvor          (get params :izvor "svi")
                   :lokacija       (get params :lokacija)
                   :q              (get params :q)
                   :cena_od        (some-> (get params :cena_od) str parse-double)
                   :cena_do        (some-> (get params :cena_do) str parse-double)
                   :kvadratura_od  (some-> (get params :kvadratura_od) str parse-double)
                   :kvadratura_do  (some-> (get params :kvadratura_do) str parse-double)
                   :broj_soba_od   (some-> (get params :broj_soba_od) str parse-double)
                   :samo_vlasnik   (= "true" (str (get params :samo_vlasnik)))
                   :samo_uknjizeno (= "true" (str (get params :samo_uknjizeno)))
                   :samo_snizeno   (= "true" (str (get params :samo_snizeno)))
                   :sortiranje     (get params :sortiranje "najnovije")
                   :strana         (or (some-> (get params :strana) str parse-long) 1)
                   :korisnik_id    (:id korisnik)}]
    (r/ok (baza/pretrazi-oglase parsirani))))

(defn api-registruj [{:keys [body-params]}]
  (let [rez (auth/registruj-korisnika! body-params)]
    (if (:greska rez)
      (r/bad-request rez)
      (r/ok rez))))

(defn api-prijavi [{:keys [body-params]}]
  (let [rez (auth/prijavi-korisnika! body-params)]
    (if (:greska rez)
      (r/unauthorized rez)
      (r/ok rez))))

(defn api-potvrdi-imejl [{:keys [params]}]
  (if (auth/potvrdi-imejl-tokenom! (:token params))
    (r/found "/?verifikovan=true")
    (r/bad-request {:greska "Nevažeći ili već iskorišćen verifikacioni token."})))

(defn api-sacuvaj-oglas [{:keys [body-params korisnik]}]
  (if-not korisnik
    (r/unauthorized {:greska "Morate biti prijavljeni da biste sačuvali oglas."})
    (let [korisnik-uuid (UUID/fromString (:id korisnik))
          oglas-uuid    (UUID/fromString (:oglas_id body-params))
          postojeci     (jdbc/execute-one! (baza/ds)
                          ["SELECT 1 FROM sacuvani_oglasi WHERE korisnik_id = ? AND oglas_id = ?"
                           korisnik-uuid oglas-uuid])]
      (if postojeci
        (do
          (jdbc/execute-one! (baza/ds)
            ["DELETE FROM sacuvani_oglasi WHERE korisnik_id = ? AND oglas_id = ?"
             korisnik-uuid oglas-uuid])
          (r/ok {:status :uklonjen :oglas_id (:oglas_id body-params)}))
        (do
          (jdbc/execute-one! (baza/ds)
            ["INSERT INTO sacuvani_oglasi (korisnik_id, oglas_id, beleska) VALUES (?, ?, ?)"
             korisnik-uuid oglas-uuid (:beleska body-params)])
          (r/ok {:status :sacuvan :oglas_id (:oglas_id body-params)}))))))

(defn api-moji-sacuvani-oglasi [{:keys [korisnik]}]
  (if-not korisnik
    (r/unauthorized {:greska "Niste prijavljeni."})
    (r/ok {:oglasi (jdbc/execute! (baza/ds)
                     ["SELECT o.*, so.beleska, so.sacuvano_u
                       FROM sacuvani_oglasi so
                       JOIN oglasi o ON o.id = so.oglas_id
                       WHERE so.korisnik_id = ?
                       ORDER BY so.sacuvano_u DESC"
                      (UUID/fromString (:id korisnik))]
                     {:builder-fn rs/as-unqualified-lower-maps})})))

(defn api-moji-alarmi [{:keys [korisnik]}]
  (if-not korisnik
    (r/unauthorized {:greska "Niste prijavljeni."})
    (let [alarmi (jdbc/execute! (baza/ds)
                   ["SELECT * FROM alarmi_pretrage WHERE korisnik_id = ? ORDER BY kreiran_u DESC"
                    (UUID/fromString (:id korisnik))]
                   {:builder-fn rs/as-unqualified-lower-maps})]
      (r/ok {:alarmi (mapv #(update % :kriterijumi baza/<-pg-jsonb) alarmi)}))))

(defn api-kreiraj-alarm [{:keys [body-params korisnik]}]
  (if-not korisnik
    (r/unauthorized {:greska "Morate biti prijavljeni da biste postavili alarm."})
    (let [svi-podaci-korisnika (jdbc/execute-one! (baza/ds)
                                 ["SELECT imejl_potvrdjen, pretplata_plan FROM korisnici WHERE id = ?"
                                  (UUID/fromString (:id korisnik))]
                                 {:builder-fn rs/as-unqualified-lower-maps})
          trazena-ucestalost   (get body-params :ucestalost "dnevno")
          pro-ucestalosti      #{"odmah" "na_6_sati"}]
      (cond
        (not (:imejl_potvrdjen svi-podaci-korisnika))
        (r/forbidden {:greska "Morate prvo potvrditi vašu imejl adresu pre aktiviranja imejl alarma."})

        (and (contains? pro-ucestalosti trazena-ucestalost)
          (not= "pro" (:pretplata_plan svi-podaci-korisnika)))
        (r/forbidden {:greska "Alarmi češći od jednom dnevno su dostupni samo u PRO planu pretplate."})

        :else
        (let [alarm (jdbc/execute-one! (baza/ds)
                      ["INSERT INTO alarmi_pretrage (korisnik_id, naziv, kriterijumi, ucestalost)
                        VALUES (?, ?, ?, ?)
                        RETURNING *"
                       (UUID/fromString (:id korisnik))
                       (or (:naziv body-params) "Moja sačuvana pretraga")
                       (baza/->pg-jsonb (:kriterijumi body-params))
                       trazena-ucestalost]
                      {:builder-fn rs/as-unqualified-lower-maps})]
          (r/ok {:alarm (update alarm :kriterijumi baza/<-pg-jsonb)}))))))

(defn api-obrisi-alarm [{:keys [path-params korisnik]}]
  (if-not korisnik
    (r/unauthorized {:greska "Niste prijavljeni."})
    (do
      (jdbc/execute-one! (baza/ds)
        ["DELETE FROM alarmi_pretrage WHERE id = ? AND korisnik_id = ?"
         (UUID/fromString (:id path-params))
         (UUID/fromString (:id korisnik))])
      (r/ok {:obrisan true}))))