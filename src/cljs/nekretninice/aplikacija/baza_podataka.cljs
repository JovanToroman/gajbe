(ns nekretninice.aplikacija.baza-podataka)

(def podrazumevana-baza
  {:aktivni-prikaz  :pretraga                               ;; :pretraga | :sacuvani | :alarmi
   :ucitavanje?     false
   :oglasi          []
   :ukupno-oglasa   0
   :sacuvani-oglasi []
   :alarmi          []
   :korisnik        (when-let [k (.getItem js/localStorage "nekretninice_korisnik")]
                      (js->clj (.parse js/JSON k) :keywordize-keys true))
   :token           (.getItem js/localStorage "nekretninice_token")
   :auth-modal      nil                                     ;; :prijava | :registracija | :novi-alarm
   :obavestenje     nil
   :filteri         {:vrsta_ponude   "prodaja"
                     :tip_nekretnine "sve"
                     :izvor          "svi"
                     :lokacija       ""
                     :q              ""
                     :cena_od        ""
                     :cena_do        ""
                     :kvadratura_od  ""
                     :kvadratura_do  ""
                     :broj_soba_od   ""
                     :samo_vlasnik   false
                     :samo_uknjizeno false
                     :samo_snizeno   false
                     :sortiranje     "najnovije"
                     :strana         1}})