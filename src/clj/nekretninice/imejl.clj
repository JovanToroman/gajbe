(ns nekretninice.imejl
  (:require [hiccup.core :as h]
            [postal.core :as email]
            [taoensso.timbre :as log]))

(defn smtp-konfiguracija []
  {:host (or (System/getenv "SMTP_HOST") "live.smtp.mailtrap.io")
   :user (or (System/getenv "SMTP_USER") "api")
   :pass (or (System/getenv "SMTP_PASS") "")
   :port (parse-long (or (System/getenv "SMTP_PORT") "587"))
   :tls  true})

(def posaljilac
  (or (System/getenv "SMTP_FROM") "obavestenja@nekretninice.rs"))

(def bazni-url-aplikacije
  (or (System/getenv "APP_BASE_URL") "http://localhost:3000"))

(defn posalji-imejl! [to tema html-sadrzaj]
  (if (empty? (:pass (smtp-konfiguracija)))
    (log/info "[DEV REŽIM - IMEJL NIJE POSLAT PREKO SMTP]" {:za to :tema tema})
    (try
      (email/send-message (smtp-konfiguracija)
        {:from    posaljilac
         :to      [to]
         :subject tema
         :body    [{:type    "text/html; charset=utf-8"
                    :content html-sadrzaj}]})
      (catch Exception e
        (log/error e "Greška pri slanju imejla ka:" to)))))

(defn posalji-verifikacioni-imejl! [imejl token]
  (let [link (str bazni-url-aplikacije "/api/auth/potvrdi-imejl?token=" token)
        html (h/html
               [:div {:style "font-family: Georgia, serif; background: #F6F5F0; padding: 32px; color: #161615; border: 1px solid #2C2C2A;"}
                [:p {:style "font-family: monospace; font-size: 11px; letter-spacing: 2px; text-transform: uppercase;"}
                 "NEKRETNINICE // VERIFIKACIJA NALOGA"]
                [:h2 {:style "margin-top: 8px;"} "Potvrdite vašu imejl adresu"]
                [:p "Da biste aktivirali alarme za nove nekretnine i sačuvali pretrage, potvrdite vašu imejl adresu klikom na dugme ispod:"]
                [:p {:style "margin: 24px 0;"}
                 [:a {:href link :style "background: #161615; color: #F6F5F0; padding: 12px 20px; text-decoration: none; font-family: monospace; font-size: 13px;"}
                  "POTVRDI IMEJL ADRESU →"]]
                [:p {:style "font-size: 12px; color: #6E6D68;"} "Ukoliko niste kreirali nalog, slobodno ignorišite ovu poruku."]])]
    (posalji-imejl! imejl "Potvrdite vašu imejl adresu — Nekretninice" html)))

(defn posalji-alarm-izvestaj! [imejl naziv-alarma oglasi]
  (let [html (h/html
               [:div {:style "font-family: sans-serif; background: #F6F5F0; padding: 28px; color: #161615;"}
                [:div {:style "border-bottom: 2px solid #161615; padding-bottom: 12px; margin-bottom: 20px;"}
                 [:span {:style "font-family: monospace; font-size: 11px; text-transform: uppercase; letter-spacing: 1.5px;"}
                  "NEKRETNINICE // IZVEŠTAJ ALARMA"]
                 [:h2 {:style "font-family: Georgia, serif; margin: 6px 0 0 0;"}
                  (str naziv-alarma " (" (count oglasi) " novih oglasa)")]]
                (for [{:keys [naslov cena_eur kvadratura_m2 cena_po_m2 mesto_sirovo izvor link_oglasa link_fotografije]} oglasi]
                  [:div {:style "display: flex; border: 1px solid #D5D1C8; background: #FAF9F5; margin-bottom: 12px; padding: 12px;"}
                   (when link_fotografije
                     [:img {:src link_fotografije :style "width: 140px; height: 100px; object-fit: cover; margin-right: 16px; border: 1px solid #D5D1C8;"}])
                   [:div
                    [:div {:style "font-family: monospace; font-size: 11px; color: #6E6D68;"}
                     (str (clojure.string/upper-case (name izvor)) " • " mesto_sirovo)]
                    [:a {:href link_oglasa :style "color: #161615; font-weight: 600; font-size: 16px; text-decoration: none;"}
                     naslov]
                    [:div {:style "font-family: monospace; font-size: 14px; margin-top: 6px;"}
                     (if cena_eur (str (long cena_eur) " €") "Cena na upit")
                     (when kvadratura_m2 (str " | " kvadratura_m2 " m²"))
                     (when cena_po_m2 (str " (" (long cena_po_m2) " €/m²)"))]]])])]
    (posalji-imejl! imejl (str "[Nekretninice] " (count oglasi) " novih oglasa za: " naziv-alarma) html)))