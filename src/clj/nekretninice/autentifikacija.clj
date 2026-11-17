(ns nekretninice.autentifikacija
  (:require [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [clojure.string :as str]
            [nekretninice.baza :as baza]
            [nekretninice.imejl :as imejl]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import (java.util UUID)))

(def jwt-tajna
  (or (System/getenv "JWT_SECRET") "nekretninice-lokalna-razvojna-tajna-2026"))

(defn kreiraj-jwt [korisnik]
  (jwt/sign {:id              (str (:id korisnik))
             :imejl           (:imejl korisnik)
             :imejl_potvrdjen (:imejl_potvrdjen korisnik)
             :pretplata_plan  (:pretplata_plan korisnik)}
    jwt-tajna))

(defn verifikuj-jwt [token]
  (try
    (jwt/unsign token jwt-tajna)
    (catch Exception _ nil)))

(defn wrap-autentifikacija [handler]
  (fn [request]
    (let [auth-header (get-in request [:headers "authorization"])
          token       (when (and auth-header (str/starts-with? auth-header "Bearer "))
                        (subs auth-header 7))
          korisnik    (when token (verifikuj-jwt token))]
      (handler (assoc request :korisnik korisnik)))))

(defn registruj-korisnika! [{:keys [imejl lozinka]}]
  (let [cist-imejl (str/lower-case (str/trim (str imejl)))
        postojeci  (jdbc/execute-one! (baza/ds)
                     ["SELECT id FROM korisnici WHERE imejl = ?" cist-imejl])]
    (if postojeci
      {:greska "Korisnik sa ovom imejl adresom već postoji."}
      (let [v-token  (str (UUID/randomUUID))
            korisnik (jdbc/execute-one! (baza/ds)
                       ["INSERT INTO korisnici (imejl, lozinka_hes, verifikacioni_token)
                         VALUES (?, ?, ?)
                         RETURNING id, imejl, imejl_potvrdjen, pretplata_plan"
                        cist-imejl (hashers/derive lozinka {:alg :argon2id}) v-token]
                       {:builder-fn rs/as-unqualified-lower-maps})]
        (future (imejl/posalji-verifikacioni-imejl! cist-imejl v-token))
        {:korisnik korisnik
         :token    (kreiraj-jwt korisnik)}))))

(defn prijavi-korisnika! [{:keys [imejl lozinka]}]
  (let [cist-imejl (str/lower-case (str/trim (str imejl)))
        korisnik   (jdbc/execute-one! (baza/ds)
                     ["SELECT * FROM korisnici WHERE imejl = ?" cist-imejl]
                     {:builder-fn rs/as-unqualified-lower-maps})]
    (if (and korisnik (:valid (hashers/verify lozinka (:lozinka_hes korisnik))))
      (let [bezbedan (select-keys korisnik [:id :imejl :imejl_potvrdjen :pretplata_plan])]
        {:korisnik bezbedan
         :token    (kreiraj-jwt bezbedan)})
      {:greska "Pogrešna imejl adresa ili lozinka."})))

(defn potvrdi-imejl-tokenom! [token]
  (jdbc/execute-one! (baza/ds)
    ["UPDATE korisnici
      SET imejl_potvrdjen = TRUE, verifikacioni_token = NULL
      WHERE verifikacioni_token = ?
      RETURNING id, imejl, imejl_potvrdjen, pretplata_plan"
     token]
    {:builder-fn rs/as-unqualified-lower-maps}))