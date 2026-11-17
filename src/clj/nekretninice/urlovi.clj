(ns nekretninice.urlovi)

(def imena-domena
  {:KP          "https://www.kupujemprodajem.com"
   :halo-oglasi "https://www.halooglasi.com"
   :4zida       "https://www.4zida.rs"})

;; Kanonski feed-ovi sortirani od najnovijih oglasa ka starijim.
;; Svo filtriranje po ceni, kvadraturi i mikrolokaciji vrši se nad našom PostgreSQL bazom.
(def izvori-za-skrejpovanje
  [{:izvor  :KP :vrsta_ponude "prodaja" :tip_nekretnine "stan"
    :urlovi ["https://www.kupujemprodajem.com/nekretnine-kupoprodaja/garsonjere/pretraga?categoryId=26&groupId=237&order=published-desc"
             "https://www.kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-jednosobni/pretraga?categoryId=26&groupId=229&order=published-desc"
             "https://www.kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-jednoiposobni/pretraga?categoryId=26&groupId=763&order=published-desc"
             "https://www.kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-dvosobni/pretraga?categoryId=26&groupId=235&order=published-desc"
             "https://www.kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-dvoiposobni/pretraga?categoryId=26&groupId=764&order=published-desc"
             "https://www.kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-trosobni/pretraga?categoryId=26&groupId=236&order=published-desc"
             "https://www.kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-cetvorosobni/pretraga?categoryId=26&groupId=2759&order=published-desc"]}
   {:izvor  :KP :vrsta_ponude "prodaja" :tip_nekretnine "kuca"
    :urlovi ["https://www.kupujemprodajem.com/nekretnine-kupoprodaja/kuce/pretraga?categoryId=26&groupId=230&order=published-desc"
             "https://www.kupujemprodajem.com/nekretnine-kupoprodaja/vikendice/pretraga?categoryId=26&groupId=231&order=published-desc"]}

   {:izvor  :halo-oglasi :vrsta_ponude "prodaja" :tip_nekretnine "stan"
    :urlovi ["https://www.halooglasi.com/nekretnine/prodaja-stanova?sort=publish_date_dt%20desc"]}
   {:izvor  :halo-oglasi :vrsta_ponude "prodaja" :tip_nekretnine "kuca"
    :urlovi ["https://www.halooglasi.com/nekretnine/prodaja-kuca?sort=publish_date_dt%20desc"]}

   {:izvor  :4zida :vrsta_ponude "prodaja" :tip_nekretnine "stan"
    :urlovi ["https://www.4zida.rs/prodaja-stanova?sortiranje=najnoviji"]}
   {:izvor  :4zida :vrsta_ponude "prodaja" :tip_nekretnine "kuca"
    :urlovi ["https://www.4zida.rs/prodaja-kuca?sortiranje=najnoviji"]}])