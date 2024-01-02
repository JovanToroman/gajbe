(ns gajbe.handlers
  (:require [gajbe.prikazi :as prikazi]
            [gajbe.rasclanjivaci :as parsers]
            [ring.util.http-response :as r]))

(defn prikazi-oglase
  [{_params :params}]
  (let [oglasi (parsers/dohvati-oglase)]
    (r/ok (prikazi/prikaz-oglasa oglasi))))
