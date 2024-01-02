(ns gajbe.urlovi)

(def urlovi-oglasa-kuce
  {:KP                ["https://kupujemprodajem.com/nekretnine-kupoprodaja/kuce/pretraga?categoryId=26&groupId=230&locationId=1&priceFrom=50000&priceTo=150000&currency=eur"
                       "https://kupujemprodajem.com/nekretnine-kupoprodaja/kuce/pretraga?categoryId=26&groupId=230&locationId=1&priceFrom=50000&priceTo=150000&currency=eur&page=2"]
   :halo-oglasi       ["https://www.halooglasi.com/nekretnine/prodaja-kuca/beograd?cena_d_from=50000&cena_d_to=150000&cena_d_unit=4&kvadratura_d_from=90&kvadratura_d_unit=1"
                       "https://www.halooglasi.com/nekretnine/prodaja-kuca/beograd?cena_d_from=50000&cena_d_to=150000&cena_d_unit=4&kvadratura_d_from=90&kvadratura_d_unit=1&page=2"]
   :beogradski-oglasi ["https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?price_low=50000&price_high=150000&country=beograd&area=80&search_id=0"
                       "https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?page=2&price_low=50000&price_high=150000&country=beograd&area=80&search_id=0"]
   :4zida             ["https://www.4zida.rs/prodaja-kuca/beograd?skuplje_od=50000eur&jeftinije_od=151000eur&vece_od=80m2&mesto=stara-pazova"
                       "https://www.4zida.rs/prodaja-kuca/beograd?skuplje_od=50000eur&jeftinije_od=151000eur&vece_od=80m2&mesto=stara-pazova&strana=2"]})

(def urlovi-oglasa-stanovi
  {:KP                ["https://kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-dvoiposobni/pretraga?categoryId=26&groupId=764&locationId=112&priceFrom=50000&priceTo=120000&currency=eur&hasPrice=yes"
                       "https://kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-dvosobni/pretraga?categoryId=26&groupId=235&locationId=112&priceFrom=50000&priceTo=120000&currency=eur&hasPrice=yes"
                       "https://kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-jednoiposobni/pretraga?categoryId=26&groupId=763&locationId=112&priceFrom=50000&priceTo=120000&currency=eur&hasPrice=yes"
                       "https://kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-jednoiposobni/pretraga?categoryId=26&groupId=763&locationId=110&priceFrom=50000&priceTo=160000&currency=eur&hasPrice=yes"
                       "https://kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-dvosobni/pretraga?categoryId=26&groupId=235&locationId=110&priceFrom=50000&priceTo=160000&currency=eur&hasPrice=yes"
                       "https://kupujemprodajem.com/nekretnine-kupoprodaja/stanovi-dvoiposobni/pretraga?categoryId=26&groupId=764&locationId=110&priceFrom=50000&priceTo=160000&currency=eur&hasPrice=yes"]
   :halo-oglasi       ["https://www.halooglasi.com/nekretnine/prodaja-zemljista/beograd-rakovica-miljakovac"
                       "https://www.halooglasi.com/nekretnine/prodaja-zemljista/beograd-vozdovac-jajinci?cena_d_to=60000&cena_d_unit=4"
                       "https://www.halooglasi.com/nekretnine/prodaja-zemljista/beograd-surcin-becmen?cena_d_to=60000&cena_d_unit=4"
                       "https://www.halooglasi.com/nekretnine/prodaja-stanova/beograd-novi-beograd-blok-28?cena_d_to=160000&cena_d_unit=4"
                       "https://www.halooglasi.com/nekretnine/prodaja-stanova/beograd-rakovica-miljakovac?cena_d_from=50000&cena_d_to=120000&cena_d_unit=4&kvadratura_d_from=40&kvadratura_d_unit=1&sprat_order_i_from=11&sprat_order_i_to=40&dodatno_id_ls=12000025"
                       "https://www.halooglasi.com/nekretnine/prodaja-stanova/beograd-novi-beograd?cena_d_from=50000&cena_d_to=160000&cena_d_unit=4&kvadratura_d_from=40&kvadratura_d_unit=1&sprat_order_i_from=11&sprat_order_i_to=40&dodatno_id_ls=12000025"]
   :beogradski-oglasi ["https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?price_low=50000&price_high=135000&property_type_Stan=on&country=beograd&area=59&property_type=stan&region=rakovica&lokacija1=miljakovac"
                       "https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?price_low=50000&price_high=160000&property_type_Stan=on&country=beograd&area=59&property_type=stan&region=novi%20beograd"]
   :4zida             ["https://www.4zida.rs/prodaja-stanova/blok-28-novi-beograd-beograd?jeftinije_od=160000eur"
                       "https://www.4zida.rs/prodaja-stanova/miljakovac-rakovica-opstina-beograd?skuplje_od=50000eur&jeftinije_od=120000eur&vece_od=40m2"
                       "https://www.4zida.rs/prodaja-stanova/novi-beograd-beograd?skuplje_od=50000eur&jeftinije_od=160000eur&vece_od=40m2"]})

(def urlovi-oglasa-vikendice
  {:KP                ["https://kupujemprodajem.com/nekretnine-kupoprodaja/kuce/pretraga?categoryId=26&groupId=230&locationId=1&priceFrom=5000&priceTo=49999&currency=eur"
                       "https://kupujemprodajem.com/nekretnine-kupoprodaja/vikendice/pretraga?categoryId=26&groupId=231&locationId=1&priceFrom=5000&priceTo=49999&currency=eur"]
   :halo-oglasi       ["https://www.halooglasi.com/nekretnine/prodaja-kuca/beograd?cena_d_from=10000&cena_d_to=50000&cena_d_unit=4&kvadratura_d_from=70&kvadratura_d_unit=1"
                       "https://www.halooglasi.com/nekretnine/prodaja-kuca/beograd?cena_d_from=10000&cena_d_to=50000&cena_d_unit=4&kvadratura_d_from=70&kvadratura_d_unit=1&page=2"]
   :beogradski-oglasi ["https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?price_low=10000&price_high=50000&country=beograd&area=80&search_id=0"
                       "https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?page=2&price_low=10000&price_high=50000&country=beograd&area=80&search_id=0"]
   :4zida             ["https://www.4zida.rs/prodaja-kuca/beograd?skuplje_od=10000eur&jeftinije_od=50000eur&vece_od=70m2&mesto=stara-pazova"
                       "https://www.4zida.rs/prodaja-kuca/beograd?skuplje_od=10000eur&jeftinije_od=50000eur&vece_od=70m2&mesto=stara-pazova&strana=2"]})

(def urlovi-oglasa-stanova-za-izdavanje
  {:KP                []
   :halo-oglasi       ["https://www.halooglasi.com/nekretnine/izdavanje-stanova/beograd?cena_d_from=100&cena_d_to=500&cena_d_unit=4&oglasivac_nekretnine_id_l=387237&dodatno_id_ls=12000009"
                       "https://www.halooglasi.com/nekretnine/izdavanje-kuca/beograd?cena_d_from=100&cena_d_to=500&cena_d_unit=4&dodatno_id_ls=12000009"]
   :beogradski-oglasi []
   :4zida             ["https://www.4zida.rs/izdavanje-stanova/beograd?skuplje_od=100eur&jeftinije_od=500eur&pet_friendly="
                       "https://www.4zida.rs/izdavanje-kuca/beograd?skuplje_od=100eur&jeftinije_od=500eur&pet_friendly="]})

#_(def urlovi-oglasa-kuce
  {:KP ["https://novi.kupujemprodajem.com/pretraga?priceTo=60000&locationRadius=50&categoryId=26&currency=eur&keywordsScope=description&priceFrom=10000&groupId=230&locationId=1&saved=true"
        "https://novi.kupujemprodajem.com/pretraga?priceTo=60000&locationRadius=50&categoryId=26&currency=eur&keywordsScope=description&priceFrom=10000&groupId=230&locationId=1&saved=true&page=2"
        "https://novi.kupujemprodajem.com/nekretnine-kupoprodaja/vikendice/pretraga?categoryId=26&groupId=231&locationId=1&locationRadius=50&priceFrom=10000&priceTo=60000&currency=eur&keywordsScope=description"
        "https://novi.kupujemprodajem.com/nekretnine-kupoprodaja/vikendice/pretraga?categoryId=26&groupId=231&locationId=1&locationRadius=50&priceFrom=10000&priceTo=60000&currency=eur&keywordsScope=description&page=2"]
   :halo-oglasi [;; prva strana
                 "https://www.halooglasi.com/nekretnine/prodaja-kuca?cena_d_to=60000&cena_d_unit=4&grad_id_l-lokacija_id_l-mikrolokacija_id_l=35112%2C35237%2C35139"
                 ;; druga strana
                 "https://www.halooglasi.com/nekretnine/prodaja-kuca?grad_id_l-lokacija_id_l-mikrolokacija_id_l=35112%2C35139%2C35237&cena_d_to=60000&cena_d_unit=4&page=2"
                 ;; alarm
                 "https://www.halooglasi.com/nekretnine/prodaja-kuca?search_ids=5425643073517%2C5425643070870%2C5425643062004&grad_id_l-lokacija_id_l-mikrolokacija_id_l=35112%2C35139%2C35237&cena_d_from=5000&cena_d_to=63000&cena_d_unit=4&sort=ValidFromForDisplay%2Cdesc"]
   :beogradski-oglasi ["https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?price_high=60000&property_type_Ku%C4%87a=on&property_type_Vikendica=on&country=beograd&search_id=0"
                       "https://www.beogradskioglasi.com/oglasi/prodaja-nekretnina/?page=2&price_high=60000&property_type_Ku%C4%87a=on&property_type_Vikendica=on&country=beograd&search_id=0"]})

(def imena-domena
  {:KP          "https://novi.kupujemprodajem.com"
   :halo-oglasi "https://www.halooglasi.com"
   :4zida       "https://www.4zida.rs"})
