(ns nekretninice.poslovi
  (:require [java-time.api :as jt]
            [nekretninice.baza :as baza]
            [nekretninice.imejl :as imejl]
            [nekretninice.rasclanjivaci :as rasclanjivaci]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [taoensso.timbre :as log])
  (:import (java.sql Timestamp)
           (java.time Instant)
           (java.util.concurrent Executors TimeUnit)))

(def intervali-ucestalosti-u-satima
  {"odmah"             0
   "na_6_sati"         6
   "dnevno"            24
   "tri_puta_nedeljno" 56
   "nedeljno"          168})

(defn vreme-je-za-alarm? [{:keys [ucestalost poslednje_slanje]}]
  (let [sati      (get intervali-ucestalosti-u-satima ucestalost 24)
        poslednje (.toInstant ^Timestamp poslednje_slanje)
        sledece   (jt/plus poslednje (jt/hours sati))]
    (jt/after? (Instant/now) sledece)))

(defn obradi-alarme-korisnika! []
  (let [alarmi (jdbc/execute! (baza/ds)
                 ["SELECT a.*, k.imejl, k.imejl_potvrdjen, k.pretplata_plan
                   FROM alarmi_pretrage a
                   JOIN korisnici k ON k.id = a.korisnik_id
                   WHERE a.aktivan = TRUE AND k.imejl_potvrdjen = TRUE"]
                 {:builder-fn rs/as-unqualified-lower-maps})]
    (doseq [alarm alarmi]
      (when (vreme-je-za-alarm? alarm)
        (let [kriterijumi (baza/<-pg-jsonb (:kriterijumi alarm))
              {:keys [oglasi]} (baza/pretrazi-oglase (assoc kriterijumi :po_strani 50))
              novi-oglasi (filter (fn [o]
                                    (jt/after? (.toInstant ^Timestamp (:prvi_put_vidjen o))
                                      (.toInstant ^Timestamp (:poslednje_slanje alarm))))
                            oglasi)]
          (when (seq novi-oglasi)
            (log/info "Šaljem alarm" (:naziv alarm) "korisniku" (:imejl alarm) "broj oglasa:" (count novi-oglasi))
            (imejl/posalji-alarm-izvestaj! (:imejl alarm) (:naziv alarm) novi-oglasi))
          (jdbc/execute-one! (baza/ds)
            ["UPDATE alarmi_pretrage SET poslednje_slanje = NOW() WHERE id = ?" (:id alarm)]))))))

(defn izvrsi-ciklus-skrejpovanja! []
  (log/info "Pokrećem ciklus skrejpovanja oglasa sa KP, HaloOglasi i 4zida...")
  (try
    (let [oglasi     (rasclanjivaci/skrejpuj-sve-portale)
          statistika (frequencies (map baza/sacuvaj-oglas! oglasi))]
      (log/info "Završeno skrejpovanje:" statistika)
      (obradi-alarme-korisnika!))
    (catch Exception e
      (log/error e "Greška u ciklusu skrejpovanja"))))

(defonce ^:private zakazivac (atom nil))

(defn pokreni-radnika! [interval-minuta]
  (when-not @zakazivac
    (let [executor (Executors/newSingleThreadScheduledExecutor)]
      (reset! zakazivac executor)
      (.scheduleWithFixedDelay executor
        ^Runnable izvrsi-ciklus-skrejpovanja!
        0
        interval-minuta
        TimeUnit/MINUTES)
      (log/info "Pozadinski radnik pokrenut (interval:" interval-minuta "minuta)."))))