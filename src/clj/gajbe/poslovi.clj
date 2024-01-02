(ns gajbe.poslovi
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gajbe.email :as imejl]
            [gajbe.prikazi :as prikazi]
            [gajbe.rasclanjivaci :as rasclanjivaci]
            [gajbe.util :as util]
            [taoensso.timbre :as log])
  (:gen-class))

(def put-do-seta-svih-oglasa-kuce "set-heseva-svih-oglasa-kuce.txt")
(def put-do-seta-svih-oglasa-stanovi "set-heseva-svih-oglasa-stan.txt")
(def put-do-seta-svih-oglasa-vikendice "set-heseva-svih-oglasa-vikendice.txt")
(def put-do-seta-svih-oglasa-stanova-za-izdavanje "set-heseva-svih-oglasa-stanova-za-izdavanje.txt")

(defn proveri-oglase-sa-skladistem-kuce []
  (let [trenutni-oglasi    (rasclanjivaci/dohvati-oglase)
        ;;trenutni-oglasi (concat (take 4 (rest trenutni-oglasi)) [prvi-oglas] (drop 5 trenutni-oglasi))
        fajl-postoji?      (.exists (io/file put-do-seta-svih-oglasa-kuce))
        oglasi-sa-hesevima (map (fn [{:keys [naslov opis mesto] :as oglas}]
                                  (assoc oglas :hes (util/md5 (str naslov opis mesto))))
                             trenutni-oglasi)
        set-svih-oglasa    (if fajl-postoji?
                             (edn/read-string (slurp put-do-seta-svih-oglasa-kuce))
                             #{})
        novi-oglasi        (remove (comp set-svih-oglasa :hes) oglasi-sa-hesevima)]
    (log/info "Proveravam oglase za kuće")
    (when (seq novi-oglasi)
      (log/info "Broj novih oglasa: " (count novi-oglasi))
      (imejl/posalji-imejl "toromanj+gajbe@gmail.com" "Novi oglasi za kuće od 50k do 150k" (prikazi/prikaz-oglasa novi-oglasi))
      (log/info "Poslat je imejl sa novim oglasima"))
    (with-open [wrtr (io/writer put-do-seta-svih-oglasa-kuce)]
      (.write wrtr ^String (str (into set-svih-oglasa (map :hes novi-oglasi)))))))

(defn proveri-oglase-sa-skladistem-stanovi []
  (let [trenutni-oglasi    (rasclanjivaci/dohvati-oglase-stanova)
        fajl-postoji?      (.exists (io/file put-do-seta-svih-oglasa-stanovi))
        oglasi-sa-hesevima (map (fn [{:keys [naslov opis mesto] :as oglas}]
                                  (assoc oglas :hes (util/md5 (str naslov opis mesto))))
                             trenutni-oglasi)
        set-svih-oglasa    (if fajl-postoji?
                             (edn/read-string (slurp put-do-seta-svih-oglasa-stanovi))
                             #{})
        novi-oglasi        (remove (comp set-svih-oglasa :hes) oglasi-sa-hesevima)]
    (log/info "Proveravam oglase za stanove")
    (when (seq novi-oglasi)
      (log/info "Broj novih oglasa: " (count novi-oglasi))
      (imejl/posalji-imejl "toromanj+gajbe@gmail.com" "Novi oglasi za stanove od 50k do 150k" (prikazi/prikaz-oglasa novi-oglasi))
      (log/info "Poslat je imejl sa novim oglasima"))
    (with-open [wrtr (io/writer put-do-seta-svih-oglasa-stanovi)]
      (.write wrtr ^String (str (into set-svih-oglasa (map :hes novi-oglasi)))))))

(defn proveri-oglase-sa-skladistem-vikendice []
  (let [trenutni-oglasi    (rasclanjivaci/dohvati-oglase-vikendica)
        fajl-postoji?      (.exists (io/file put-do-seta-svih-oglasa-vikendice))
        oglasi-sa-hesevima (map (fn [{:keys [naslov opis mesto] :as oglas}]
                                  (assoc oglas :hes (util/md5 (str naslov opis mesto))))
                             trenutni-oglasi)
        set-svih-oglasa    (if fajl-postoji?
                             (edn/read-string (slurp put-do-seta-svih-oglasa-vikendice))
                             #{})
        novi-oglasi        (remove (comp set-svih-oglasa :hes) oglasi-sa-hesevima)]
    (log/info "Proveravam oglase za vikendice")
    (when (seq novi-oglasi)
      (log/info "Broj novih oglasa: " (count novi-oglasi))
      (imejl/posalji-imejl "toromanj+gajbe@gmail.com" "Novi oglasi za vikendice od 10k do 50k" (prikazi/prikaz-oglasa novi-oglasi))
      (log/info "Poslat je imejl sa novim oglasima"))
    (with-open [wrtr (io/writer put-do-seta-svih-oglasa-vikendice)]
      (.write wrtr ^String (str (into set-svih-oglasa (map :hes novi-oglasi)))))))

(defn proveri-oglase-sa-skladistem-stanovi-izdavanje []
  (let [trenutni-oglasi    (rasclanjivaci/dohvati-oglase-stanova-za-izdavanje)
        fajl-postoji?      (.exists (io/file put-do-seta-svih-oglasa-stanova-za-izdavanje))
        oglasi-sa-hesevima (map (fn [{:keys [naslov opis mesto] :as oglas}]
                                  (assoc oglas :hes (util/md5 (str naslov opis mesto))))
                             trenutni-oglasi)
        set-svih-oglasa    (if fajl-postoji?
                             (edn/read-string (slurp put-do-seta-svih-oglasa-stanova-za-izdavanje))
                             #{})
        novi-oglasi        (remove (comp set-svih-oglasa :hes) oglasi-sa-hesevima)]
    (log/info "Proveravam oglase za stanove izdavanje")
    (when (seq novi-oglasi)
      (log/info "Broj novih oglasa: " (count novi-oglasi))
      (imejl/posalji-imejl "toromanj+gajbe@gmail.com" "Novi oglasi za stanove za izdavanje od 100 do 500 evra"
        (prikazi/prikaz-oglasa novi-oglasi))
      (log/info "Poslat je imejl sa novim oglasima"))
    (with-open [wrtr (io/writer put-do-seta-svih-oglasa-stanova-za-izdavanje)]
      (.write wrtr ^String (str (into set-svih-oglasa (map :hes novi-oglasi)))))))

(defn -main [& _args]
  (proveri-oglase-sa-skladistem-kuce)
  (proveri-oglase-sa-skladistem-vikendice)
  ;;(proveri-oglase-sa-skladistem-stanovi-izdavanje)
  (proveri-oglase-sa-skladistem-stanovi)
  (log/info "Uspešno proverio sve oglase"))
