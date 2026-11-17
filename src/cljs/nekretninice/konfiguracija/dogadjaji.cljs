(ns nekretninice.konfiguracija.dogadjaji
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [nekretninice.aplikacija.baza-podataka :as db]
            [re-frame.core :as rf]))

(defn auth-zaglavlja [token]
  (if token {"Authorization" (str "Bearer " token)} {}))

(rf/reg-event-db
  :inicijalizuj-bazu
  (fn [_ _]
    db/podrazumevana-baza))

(rf/reg-event-db
  :postavi-filter
  (fn [db [_ kljuc vrednost]]
    (-> db
      (assoc-in [:filteri kljuc] vrednost)
      (assoc-in [:filteri :strana] 1))))

(rf/reg-event-db
  :promeni-stranu
  (fn [db [_ nova-strana]]
    (assoc-in db [:filteri :strana] nova-strana)))

(rf/reg-event-db
  :postavi-prikaz
  (fn [db [_ prikaz]]
    (assoc db :aktivni-prikaz prikaz)))

(rf/reg-event-db
  :otvori-modal
  (fn [db [_ modal]]
    (assoc db :auth-modal modal :obavestenje nil)))

(rf/reg-event-db
  :postavi-obavestenje
  (fn [db [_ tekst tip]]
    (assoc db :obavestenje {:tekst tekst :tip tip})))

(rf/reg-event-db
  :uspesno-dohvaceni-oglasi
  (fn [db [_ {:keys [oglasi ukupno]}]]
    (assoc db :oglasi oglasi :ukupno-oglasa ukupno :ucitavanje? false)))

(rf/reg-event-fx
  :dohvati-oglase
  (fn [{:keys [db]} _]
    (let [filteri (:filteri db)
          token   (:token db)]
      (go
        (let [odgovor (<! (http/get "/api/oglasi"
                            {:query-params filteri
                             :headers      (auth-zaglavlja token)}))]
          (when (:success odgovor)
            (rf/dispatch [:uspesno-dohvaceni-oglasi (:body odgovor)]))))
      {:db (assoc db :ucitavanje? true)})))

(rf/reg-event-db
  :uspesna-autentifikacija
  (fn [db [_ {:keys [korisnik token]}]]
    (.setItem js/localStorage "nekretninice_token" token)
    (.setItem js/localStorage "nekretninice_korisnik" (.stringify js/JSON (clj->js korisnik)))
    (assoc db :korisnik korisnik :token token :auth-modal nil)))

(rf/reg-event-db
  :odjava
  (fn [db _]
    (.removeItem js/localStorage "nekretninice_token")
    (.removeItem js/localStorage "nekretninice_korisnik")
    (assoc db :korisnik nil :token nil :aktivni-prikaz :pretraga)))

(rf/reg-event-fx
  :posalji-autentifikaciju
  (fn [_ [_ rezim podaci]]
    (let [putanja (if (= rezim :registracija) "/api/auth/registracija" "/api/auth/prijava")]
      (go
        (let [odgovor (<! (http/post putanja {:json-params podaci}))]
          (if (:success odgovor)
            (do
              (rf/dispatch [:uspesna-autentifikacija (:body odgovor)])
              (rf/dispatch [:dohvati-oglase]))
            (rf/dispatch [:postavi-obavestenje (get-in odgovor [:body :greska] "Greška pri prijavi") :greska]))))
      {})))

(rf/reg-event-fx
  :sacuvaj-oglas
  (fn [{:keys [db]} [_ oglas-id]]
    (if-not (:korisnik db)
      {:dispatch [:otvori-modal :prijava]}
      (do
        (go
          (let [odgovor (<! (http/post "/api/sacuvani"
                              {:json-params {:oglas_id oglas-id}
                               :headers     (auth-zaglavlja (:token db))}))]
            (when (:success odgovor)
              (rf/dispatch [:dohvati-oglase])
              (rf/dispatch [:dohvati-sacuvane]))))
        {}))))

(rf/reg-event-db
  :postavi-sacuvane
  (fn [db [_ oglasi]]
    (assoc db :sacuvani-oglasi oglasi)))

(rf/reg-event-fx
  :dohvati-sacuvane
  (fn [{:keys [db]} _]
    (when (:token db)
      (go
        (let [odgovor (<! (http/get "/api/sacuvani" {:headers (auth-zaglavlja (:token db))}))]
          (when (:success odgovor)
            (rf/dispatch [:postavi-sacuvane (get-in odgovor [:body :oglasi])])))))
    {}))

(rf/reg-event-db
  :postavi-alarme
  (fn [db [_ alarmi]]
    (assoc db :alarmi alarmi)))

(rf/reg-event-fx
  :dohvati-alarme
  (fn [{:keys [db]} _]
    (when (:token db)
      (go
        (let [odgovor (<! (http/get "/api/alarmi" {:headers (auth-zaglavlja (:token db))}))]
          (when (:success odgovor)
            (rf/dispatch [:postavi-alarme (get-in odgovor [:body :alarmi])])))))
    {}))

(rf/reg-event-fx
  :kreiraj-alarm
  (fn [{:keys [db]} [_ {:keys [naziv ucestalost]}]]
    (go
      (let [odgovor (<! (http/post "/api/alarmi"
                          {:json-params {:naziv       naziv
                                         :ucestalost  ucestalost
                                         :kriterijumi (:filteri db)}
                           :headers     (auth-zaglavlja (:token db))}))]
        (if (:success odgovor)
          (do
            (rf/dispatch [:otvori-modal nil])
            (rf/dispatch [:dohvati-alarme])
            (rf/dispatch [:postavi-obavestenje "Alarm za pretragu je uspešno sačuvan." :uspeh]))
          (rf/dispatch [:postavi-obavestenje (get-in odgovor [:body :greska] "Greška pri čuvanju alarma.") :greska]))))
    {}))

(rf/reg-event-fx
  :obrisi-alarm
  (fn [{:keys [db]} [_ id]]
    (go
      (let [odgovor (<! (http/delete (str "/api/alarmi/" id) {:headers (auth-zaglavlja (:token db))}))]
        (when (:success odgovor)
          (rf/dispatch [:dohvati-alarme]))))
    {}))