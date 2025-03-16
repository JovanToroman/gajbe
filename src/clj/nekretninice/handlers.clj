(ns nekretninice.handlers
  (:require [nekretninice.prikazi :as prikazi]
            [nekretninice.rasclanjivaci :as parsers]
            [ring.util.http-response :as r]))

(defn prikazi-oglase
  [{_params :params}]
  (let [oglasi (parsers/dohvati-oglase)]
    (r/ok (prikazi/prikaz-oglasa oglasi))))
