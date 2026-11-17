(ns nekretninice.aplikacija.prikazi
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn formatiraj-broj [n]
  (when n
    (.toLocaleString (js/Number. n) "sr-RS")))

(defn bedz-portala [izvor]
  (let [[tekst klase] (case izvor
                        "KP" ["KUPUJEMPRODAJEM" "bg-ochre-bg text-ochre-text border-ochre-text/30"]
                        "halo-oglasi" ["HALO OGLASI" "bg-slate-pastel text-slate-ink border-slate-ink/30"]
                        "4zida" ["4ZIDA.RS" "bg-terracotta-bg text-terracotta-text border-terracotta-text/30"]
                        [(str/upper-case (str izvor)) "bg-surface text-ink border-border-subtle"])]
    [:span {:class (str "px-2 py-0.5 text-[10px] font-mono font-medium tracking-wider border " klase)}
     tekst]))

(defn kartica-oglasa [{:keys [id izvor naslov opis cena_eur prethodna_cena_eur kvadratura_m2
                              cena_po_m2 prosek_kraja_m2 broj_soba sprat uknjizeno oglasivac
                              mesto_sirovo link_oglasa link_fotografije broj_portala sacuvan]}]
  (let [odstupanje-m2 (when (and cena_po_m2 prosek_kraja_m2 (pos? prosek_kraja_m2))
                        (Math/round (* 100 (/ (- cena_po_m2 prosek_kraja_m2) prosek_kraja_m2))))
        snizeno?      (and prethodna_cena_eur cena_eur (< cena_eur prethodna_cena_eur))]
    [:article {:class "bg-card border border-border-strong flex flex-col justify-between transition-colors hover:bg-surface/60"}
     [:div
      ;; Slika + gornja traka sa izvorom i dugmetom za čuvanje
      [:div {:class "relative h-52 w-full bg-surface border-b border-border-strong overflow-hidden"}
       (if (and link_fotografije (not (str/blank? link_fotografije)))
         [:img {:src     link_fotografije
                :alt     naslov
                :loading "lazy"
                :class   "w-full h-full object-cover"}]
         [:div {:class "w-full h-full flex items-center justify-center font-mono text-xs text-muted uppercase tracking-widest"}
          "Bez fotografije"])
       [:div {:class "absolute top-2 left-2 flex flex-wrap gap-1"}
        [bedz-portala izvor]
        (when (and broj_portala (> broj_portala 1))
          [:span {:class "px-2 py-0.5 text-[10px] font-mono bg-ink text-paper"}
           (str "NA " broj_portala " PORTALA")])
        (when snizeno?
          [:span {:class "px-2 py-0.5 text-[10px] font-mono font-semibold bg-sage-bg text-sage-text border border-sage-text/40"}
           (str "SNIŽENO -" (formatiraj-broj (- prethodna_cena_eur cena_eur)) " €")])]
       [:button {:on-click #(rf/dispatch [:sacuvaj-oglas id])
                 :class    (str "absolute top-2 right-2 px-2.5 py-1 text-xs font-mono border border-border-strong cursor-pointer "
                             (if sacuvan "bg-ink text-paper" "bg-paper/95 text-ink hover:bg-ink hover:text-paper"))}
        (if sacuvan "★ SAČUVANO" "☆ SAČUVAJ")]]

      ;; Glavni podaci
      [:div {:class "p-4"}
       [:div {:class "flex items-center justify-between gap-2 text-[11px] font-mono text-muted uppercase tracking-wider mb-1"}
        [:span {:class "truncate"} (or mesto_sirovo "Srbija")]
        (when (= oglasivac "vlasnik")
          [:span {:class "px-1.5 py-0.5 bg-sage-bg text-sage-text font-semibold shrink-0"} "VLASNIK"])]

       [:a {:href   link_oglasa
            :target "_blank"
            :rel    "noopener noreferrer"
            :class  "block font-serif text-lg font-medium leading-snug text-ink hover:underline line-clamp-2 mb-2"}
        naslov]

       (when opis
         [:p {:class "text-xs text-muted line-clamp-2 mb-3 leading-relaxed"} opis])]]

     ;; Donja tehnička tabela sa metrikama
     [:div {:class "border-t border-border-subtle bg-paper/60 px-4 py-3"}
      [:div {:class "grid grid-cols-3 gap-2 font-mono text-xs border-b border-border-subtle pb-2 mb-2"}
       [:div
        [:span {:class "block text-[10px] text-muted uppercase"} "Kvadratura"]
        [:span {:class "font-medium"} (if kvadratura_m2 (str kvadratura_m2 " m²") "—")]]
       [:div {:class "border-l border-border-subtle pl-2"}
        [:span {:class "block text-[10px] text-muted uppercase"} "Struktura"]
        [:span {:class "font-medium"} (if broj_soba (str broj_soba " sobe") (or sprat "—"))]]
       [:div {:class "border-l border-border-subtle pl-2"}
        [:span {:class "block text-[10px] text-muted uppercase"} "Cena / m²"]
        [:span {:class "font-medium"} (if cena_po_m2 (str (formatiraj-broj cena_po_m2) " €") "—")]]]

      [:div {:class "flex items-baseline justify-between pt-1"}
       [:div
        [:span {:class "font-mono text-lg font-semibold tracking-tight text-ink"}
         (if cena_eur (str (formatiraj-broj cena_eur) " €") "Na upit")]
        (when prethodna_cena_eur
          [:span {:class "ml-2 font-mono text-xs line-through text-muted"}
           (str (formatiraj-broj prethodna_cena_eur) " €")])]

       (if (and odstupanje-m2 (not (zero? odstupanje-m2)))
         [:span {:class (str "text-[11px] font-mono px-1.5 py-0.5 border "
                          (if (neg? odstupanje-m2)
                            "bg-sage-bg text-sage-text border-sage-text/30"
                            "bg-terracotta-bg text-terracotta-text border-terracotta-text/30"))}
          (str (if (pos? odstupanje-m2) "+" "") odstupanje-m2 "% od kraja")]
         (when uknjizeno
           [:span {:class "text-[10px] font-mono uppercase px-1.5 py-0.5 bg-surface border border-border-subtle"}
            "Uknjiženo"]))]]]))

(defn traka-filtera []
  (let [filteri  @(rf/subscribe [:filteri])
        korisnik @(rf/subscribe [:korisnik])
        promeni! (fn [k v]
                   (rf/dispatch [:postavi-filter k v])
                   (rf/dispatch [:dohvati-oglase]))]
    [:section {:class "border-b border-border-strong bg-surface/70"}
     [:div {:class "max-w-7xl mx-auto px-4 py-5"}
      [:div {:class "grid grid-cols-1 md:grid-cols-6 gap-3"}
       ;; Tip nekretnine
       [:div
        [:label {:class "block font-mono text-[10px] uppercase tracking-wider text-muted mb-1"} "Tip nekretnine"]
        [:select {:value     (:tip_nekretnine filteri)
                  :on-change #(promeni! :tip_nekretnine (.. % -target -value))
                  :class     "w-full bg-paper border border-border-strong px-3 py-2 text-xs font-mono focus:outline-none"}
         [:option {:value "sve"} "Stanovi i kuće"]
         [:option {:value "stan"} "Samo stanovi"]
         [:option {:value "kuca"} "Samo kuće i vikendice"]]]

       ;; Lokacija
       [:div {:class "md:col-span-2"}
        [:label {:class "block font-mono text-[10px] uppercase tracking-wider text-muted mb-1"} "Grad / Opština / Mikrolokacija"]
        [:input {:type        "text"
                 :placeholder "npr. Novi Beograd, Blok 28, Miljakovac, Jajinci..."
                 :value       (:lokacija filteri)
                 :on-change   #(rf/dispatch [:postavi-filter :lokacija (.. % -target -value)])
                 :on-key-down #(when (= (.-key %) "Enter") (rf/dispatch [:dohvati-oglase]))
                 :class       "w-full bg-paper border border-border-strong px-3 py-2 text-xs font-mono focus:outline-none"}]]

       ;; Cena OD - DO
       [:div
        [:label {:class "block font-mono text-[10px] uppercase tracking-wider text-muted mb-1"} "Cena (€)"]
        [:div {:class "grid grid-cols-2 gap-1"}
         [:input {:type        "number"
                  :placeholder "Od"
                  :value       (:cena_od filteri)
                  :on-change   #(rf/dispatch [:postavi-filter :cena_od (.. % -target -value)])
                  :class       "w-full bg-paper border border-border-strong px-2 py-2 text-xs font-mono focus:outline-none"}]
         [:input {:type        "number"
                  :placeholder "Do"
                  :value       (:cena_do filteri)
                  :on-change   #(rf/dispatch [:postavi-filter :cena_do (.. % -target -value)])
                  :class       "w-full bg-paper border border-border-strong px-2 py-2 text-xs font-mono focus:outline-none"}]]]

       ;; Kvadratura OD - DO
       [:div
        [:label {:class "block font-mono text-[10px] uppercase tracking-wider text-muted mb-1"} "Kvadratura (m²)"]
        [:div {:class "grid grid-cols-2 gap-1"}
         [:input {:type        "number"
                  :placeholder "Od m²"
                  :value       (:kvadratura_od filteri)
                  :on-change   #(rf/dispatch [:postavi-filter :kvadratura_od (.. % -target -value)])
                  :class       "w-full bg-paper border border-border-strong px-2 py-2 text-xs font-mono focus:outline-none"}]
         [:input {:type        "number"
                  :placeholder "Do m²"
                  :value       (:kvadratura_do filteri)
                  :on-change   #(rf/dispatch [:postavi-filter :kvadratura_do (.. % -target -value)])
                  :class       "w-full bg-paper border border-border-strong px-2 py-2 text-xs font-mono focus:outline-none"}]]]

       ;; Izvor / Sortiranje
       [:div
        [:label {:class "block font-mono text-[10px] uppercase tracking-wider text-muted mb-1"} "Sortiranje"]
        [:select {:value     (:sortiranje filteri)
                  :on-change #(promeni! :sortiranje (.. % -target -value))
                  :class     "w-full bg-paper border border-border-strong px-3 py-2 text-xs font-mono focus:outline-none"}
         [:option {:value "najnovije"} "Najnoviji oglasi"]
         [:option {:value "cena_rastuce"} "Cena: niža ka višoj"]
         [:option {:value "cena_opadajuce"} "Cena: viša ka nižoj"]
         [:option {:value "m2_najpovoljnije"} "Najpovoljniji €/m²"]]]]

      ;; Drugi red filtera: Brzi prekidači i dugme za Alarm
      [:div {:class "flex flex-wrap items-center justify-between gap-3 mt-4 pt-3 border-t border-border-subtle"}
       [:div {:class "flex flex-wrap items-center gap-4 text-xs font-mono"}
        [:label {:class "flex items-center gap-2 cursor-pointer"}
         [:input {:type      "checkbox"
                  :checked   (:samo_vlasnik filteri)
                  :on-change #(promeni! :samo_vlasnik (.. % -target -checked))
                  :class     "accent-ink"}]
         "DIREKTNO OD VLASNIKA"]

        [:label {:class "flex items-center gap-2 cursor-pointer"}
         [:input {:type      "checkbox"
                  :checked   (:samo_uknjizeno filteri)
                  :on-change #(promeni! :samo_uknjizeno (.. % -target -checked))
                  :class     "accent-ink"}]
         "SAMO UKNJIŽENO"]

        [:label {:class "flex items-center gap-2 cursor-pointer"}
         [:input {:type      "checkbox"
                  :checked   (:samo_snizeno filteri)
                  :on-change #(promeni! :samo_snizeno (.. % -target -checked))
                  :class     "accent-ink"}]
         "SNIŽENA CENA"]]

       [:div {:class "flex items-center gap-2"}
        [:button {:on-click #(rf/dispatch [:dohvati-oglase])
                  :class    "px-4 py-2 bg-ink text-paper text-xs font-mono uppercase tracking-wider cursor-pointer hover:bg-ink/85"}
         "Primeni filtere"]
        [:button {:on-click #(if korisnik
                               (rf/dispatch [:otvori-modal :novi-alarm])
                               (rf/dispatch [:otvori-modal :prijava]))
                  :class    "px-4 py-2 bg-sage-bg text-sage-text border border-border-strong text-xs font-mono font-medium uppercase tracking-wider cursor-pointer hover:bg-sage-bg/80"}
         "🔔 Postavi imejl alarm za ovu pretragu"]]]]]))

(defn modal-autentifikacije [rezim]
  (let [forma (r/atom {:imejl "" :lozinka ""})]
    (fn [rezim]
      (let [obavestenje @(rf/subscribe [:obavestenje])]
        [:div {:class "fixed inset-0 bg-ink/50 flex items-center justify-center z-50 p-4"}
         [:div {:class "bg-paper border-2 border-border-strong max-w-md w-full p-6"}
          [:div {:class "flex justify-between items-center border-b border-border-subtle pb-3 mb-4"}
           [:h3 {:class "font-serif text-xl font-medium"}
            (if (= rezim :registracija) "Registracija korisnika" "Prijava na nalog")]
           [:button {:on-click #(rf/dispatch [:otvori-modal nil])
                     :class    "font-mono text-sm cursor-pointer"} "✕"]]

          (when obavestenje
            [:div {:class "p-3 mb-4 text-xs font-mono bg-terracotta-bg text-terracotta-text border border-terracotta-text"}
             (:tekst obavestenje)])

          [:div {:class "space-y-4"}
           [:div
            [:label {:class "block font-mono text-xs uppercase mb-1"} "Imejl adresa"]
            [:input {:type      "email"
                     :value     (:imejl @forma)
                     :on-change #(swap! forma assoc :imejl (.. % -target -value))
                     :class     "w-full bg-card border border-border-strong px-3 py-2 text-sm font-mono"}]]
           [:div
            [:label {:class "block font-mono text-xs uppercase mb-1"} "Lozinka"]
            [:input {:type      "password"
                     :value     (:lozinka @forma)
                     :on-change #(swap! forma assoc :lozinka (.. % -target -value))
                     :class     "w-full bg-card border border-border-strong px-3 py-2 text-sm font-mono"}]]

           [:button {:on-click #(rf/dispatch [:posalji-autentifikaciju rezim @forma])
                     :class    "w-full py-2.5 bg-ink text-paper font-mono text-xs uppercase tracking-wider cursor-pointer"}
            (if (= rezim :registracija) "Kreiraj nalog i pošalji verifikaciju" "Prijavi se")]

           [:div {:class "text-center pt-2 border-t border-border-subtle font-mono text-xs"}
            (if (= rezim :registracija)
              [:button {:on-click #(rf/dispatch [:otvori-modal :prijava]) :class "underline cursor-pointer"}
               "Već imate nalog? Prijavite se"]
              [:button {:on-click #(rf/dispatch [:otvori-modal :registracija]) :class "underline cursor-pointer"}
               "Nemate nalog? Registrujte se besplatno"])]]]]))))

(defn modal-novog-alarma []
  (let [forma (r/atom {:naziv "" :ucestalost "dnevno"})]
    (fn []
      (let [korisnik    @(rf/subscribe [:korisnik])
            obavestenje @(rf/subscribe [:obavestenje])
            pro?        (= "pro" (:pretplata_plan korisnik))]
        [:div {:class "fixed inset-0 bg-ink/50 flex items-center justify-center z-50 p-4"}
         [:div {:class "bg-paper border-2 border-border-strong max-w-lg w-full p-6"}
          [:div {:class "flex justify-between items-center border-b border-border-subtle pb-3 mb-4"}
           [:h3 {:class "font-serif text-xl font-medium"} "Novi imejl alarm za pretragu"]
           [:button {:on-click #(rf/dispatch [:otvori-modal nil]) :class "font-mono text-sm cursor-pointer"} "✕"]]

          (when-not (:imejl_potvrdjen korisnik)
            [:div {:class "p-3 mb-4 text-xs font-mono bg-ochre-bg text-ochre-text border border-ochre-text"}
             "Napomena: Proverite vaše prijemno sanduče i potvrdite imejl adresu kako bi alarmi mogli da se šalju."])

          (when obavestenje
            [:div {:class "p-3 mb-4 text-xs font-mono bg-terracotta-bg text-terracotta-text border border-terracotta-text"}
             (:tekst obavestenje)])

          [:div {:class "space-y-4"}
           [:div
            [:label {:class "block font-mono text-xs uppercase mb-1"} "Naziv alarma"]
            [:input {:type        "text"
                     :placeholder "npr. Dvosoban stan Novi Beograd do 150.000€"
                     :value       (:naziv @forma)
                     :on-change   #(swap! forma assoc :naziv (.. % -target -value))
                     :class       "w-full bg-card border border-border-strong px-3 py-2 text-sm font-mono"}]]

           [:div
            [:label {:class "block font-mono text-xs uppercase mb-1"} "Učestalost obaveštavanja"]
            [:select {:value     (:ucestalost @forma)
                      :on-change #(swap! forma assoc :ucestalost (.. % -target -value))
                      :class     "w-full bg-card border border-border-strong px-3 py-2 text-xs font-mono"}
             [:option {:value "dnevno"} "Jednom dnevno (Besplatno)"]
             [:option {:value "tri_puta_nedeljno"} "Svaka 2-3 dana (Besplatno)"]
             [:option {:value "nedeljno"} "Jednom nedeljno — pregled (Besplatno)"]
             [:option {:value "na_6_sati" :disabled (not pro?)} "Na svakih 6 sati (PRO pretplata)"]
             [:option {:value "odmah" :disabled (not pro?)} "Odmah po objavljivanju — Real-time (PRO pretplata)"]]]

           [:button {:on-click #(rf/dispatch [:kreiraj-alarm @forma])
                     :class    "w-full py-2.5 bg-ink text-paper font-mono text-xs uppercase tracking-wider cursor-pointer"}
            "Sačuvaj alarm za trenutne filtere"]]]]))))

(defn zaglavlje []
  (let [korisnik       @(rf/subscribe [:korisnik])
        aktivni-prikaz @(rf/subscribe [:aktivni-prikaz])
        ukupno         @(rf/subscribe [:ukupno-oglasa])]
    [:header {:class "border-b-2 border-border-strong bg-paper"}
     [:div {:class "max-w-7xl mx-auto px-4 py-4 flex flex-wrap items-center justify-between gap-4"}
      [:div {:class "flex items-baseline gap-4"}
       [:h1 {:on-click #(rf/dispatch [:postavi-prikaz :pretraga])
             :class    "font-serif text-2xl md:text-3xl font-semibold tracking-tight cursor-pointer"}
        "NEKRETNINICE"]
       [:span {:class "font-mono text-xs text-muted uppercase tracking-widest hidden sm:inline"}
        (str "KP • HALO OGLASI • 4ZIDA // " (formatiraj-broj ukupno) " AKTIVNIH OGLASA")]]

      [:nav {:class "flex items-center gap-2 font-mono text-xs"}
       [:button {:on-click #(rf/dispatch [:postavi-prikaz :pretraga])
                 :class    (str "px-3 py-1.5 border cursor-pointer "
                             (if (= aktivni-prikaz :pretraga)
                               "bg-ink text-paper border-ink"
                               "bg-card text-ink border-border-strong"))}
        "PRETRAGA"]

       (when korisnik
         [:<>
          [:button {:on-click #(do (rf/dispatch [:dohvati-sacuvane])
                                 (rf/dispatch [:postavi-prikaz :sacuvani]))
                    :class    (str "px-3 py-1.5 border cursor-pointer "
                                (if (= aktivni-prikaz :sacuvani)
                                  "bg-ink text-paper border-ink"
                                  "bg-card text-ink border-border-strong"))}
           "SAČUVANI OGLASI"]
          [:button {:on-click #(do (rf/dispatch [:dohvati-alarme])
                                 (rf/dispatch [:postavi-prikaz :alarmi]))
                    :class    (str "px-3 py-1.5 border cursor-pointer "
                                (if (= aktivni-prikaz :alarmi)
                                  "bg-ink text-paper border-ink"
                                  "bg-card text-ink border-border-strong"))}
           "MOJI ALARMI"]])

       (if korisnik
         [:div {:class "flex items-center gap-2 ml-2 pl-2 border-l border-border-subtle"}
          [:span {:class "text-muted hidden md:inline"} (:imejl korisnik)]
          [:button {:on-click #(rf/dispatch [:odjava])
                    :class    "px-2.5 py-1.5 border border-border-strong bg-surface hover:bg-terracotta-bg cursor-pointer"}
           "ODJAVA"]]
         [:button {:on-click #(rf/dispatch [:otvori-modal :prijava])
                   :class    "px-3.5 py-1.5 bg-ink text-paper border border-ink cursor-pointer uppercase"}
          "PRIJAVA / REGISTRACIJA"])]]]))

(defn glavni-prikaz []
  (let [oglasi         @(rf/subscribe [:oglasi])
        ucitavanje?    @(rf/subscribe [:ucitavanje?])
        aktivni-prikaz @(rf/subscribe [:aktivni-prikaz])
        sacuvani       @(rf/subscribe [:sacuvani-oglasi])
        alarmi         @(rf/subscribe [:alarmi])
        modal          @(rf/subscribe [:auth-modal])
        filteri        @(rf/subscribe [:filteri])]
    [:div {:class "min-h-screen flex flex-col"}
     [zaglavlje]

     (case aktivni-prikaz
       :sacuvani
       [:main {:class "max-w-7xl mx-auto w-full px-4 py-8 flex-1"}
        [:h2 {:class "font-serif text-2xl mb-6 pb-2 border-b border-border-strong"} "Sačuvani oglasi"]
        [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5"}
         (for [oglas sacuvani]
           ^{:key (:id oglas)} [kartica-oglasa (assoc oglas :sacuvan true)])]]

       :alarmi
       [:main {:class "max-w-5xl mx-auto w-full px-4 py-8 flex-1"}
        [:h2 {:class "font-serif text-2xl mb-6 pb-2 border-b border-border-strong"} "Aktivni imejl alarmi"]
        (if (empty? alarmi)
          [:p {:class "font-mono text-xs text-muted"} "Nemate nijedan sačuvan alarm. Podesite filtere u pretrazi i kliknite na 'Postavi imejl alarm'."]
          [:div {:class "space-y-3"}
           (for [{:keys [id naziv ucestalost kriterijumi poslednje_slanje]} alarmi]
             ^{:key id}
             [:div {:class "p-4 bg-card border border-border-strong flex items-center justify-between"}
              [:div
               [:h4 {:class "font-serif text-lg font-medium"} naziv]
               [:p {:class "font-mono text-xs text-muted mt-1"}
                (str "Učestalost: " (str/upper-case ucestalost)
                  " • Lokacija: " (or (not-empty (:lokacija kriterijumi)) "Sve")
                  " • Cena do: " (or (not-empty (str (:cena_do kriterijumi))) "Bez limita") " €")]]
              [:button {:on-click #(rf/dispatch [:obrisi-alarm id])
                        :class    "px-3 py-1.5 text-xs font-mono bg-terracotta-bg text-terracotta-text border border-terracotta-text/40 cursor-pointer"}
               "OBRIŠI"]])])]

       ;; Podrazumevani prikaz: Pretraga
       [:<>
        [traka-filtera]
        [:main {:class "max-w-7xl mx-auto w-full px-4 py-8 flex-1"}
         (if ucitavanje?
           [:div {:class "py-20 text-center font-mono text-xs uppercase tracking-widest text-muted"}
            "Učitavanje indeksa nekretnina..."]
           [:<>
            [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5"}
             (for [oglas oglasi]
               ^{:key (:id oglas)} [kartica-oglasa oglas])]
            ;; Straničenje
            [:div {:class "flex items-center justify-between border-t border-border-strong mt-10 pt-4 font-mono text-xs"}
             [:button {:disabled (<= (:strana filteri) 1)
                       :on-click #(do (rf/dispatch [:promeni-stranu (dec (:strana filteri))])
                                    (rf/dispatch [:dohvati-oglase]))
                       :class    "px-4 py-2 border border-border-strong bg-card disabled:opacity-40 cursor-pointer"}
              "← PRETHODNA STRANA"]
             [:span (str "STRANA " (:strana filteri))]
             [:button {:on-click #(do (rf/dispatch [:promeni-stranu (inc (:strana filteri))])
                                    (rf/dispatch [:dohvati-oglase]))
                       :class    "px-4 py-2 border border-border-strong bg-card cursor-pointer"}
              "SLEDEĆA STRANA →"]]])]])

     (when (#{:prijava :registracija} modal)
       [modal-autentifikacije modal])
     (when (= modal :novi-alarm)
       [modal-novog-alarma])]))