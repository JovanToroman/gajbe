(ns nekretninice.aplikacija.putanje
  (:require [reitit.coercion.spec]))

(def putanje
  ["/" {:coercion reitit.coercion.spec/coercion}
   ["" {:name :putanja/pocetna
        :doc  "Početna strana"}]])
