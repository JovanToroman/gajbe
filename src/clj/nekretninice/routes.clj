(ns nekretninice.routes
  (:require [nekretninice.hendleri :as h]))

(def routes
  [["/" {:get {:handler h/pocetna-stranica}}]
   ["/api"
    ["/oglasi" {:get {:handler h/api-pretrazi-oglase}}]
    ["/auth"
     ["/registracija" {:post {:handler h/api-registruj}}]
     ["/prijava" {:post {:handler h/api-prijavi}}]
     ["/potvrdi-imejl" {:get {:handler h/api-potvrdi-imejl}}]]
    ["/sacuvani"
     ["" {:get  {:handler h/api-moji-sacuvani-oglasi}
          :post {:handler h/api-sacuvaj-oglas}}]]
    ["/alarmi"
     ["" {:get  {:handler h/api-moji-alarmi}
          :post {:handler h/api-kreiraj-alarm}}]
     ["/:id" {:delete {:handler h/api-obrisi-alarm}}]]]])