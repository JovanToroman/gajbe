(ns gajbe.email
  (:require [postal.core :as email]))

(defn posalji-imejl
  [to tema sadrzaj]
  (email/send-message {:host "smtp.gmail.com"
                       :user "toromanj@gmail.com"
                       :pass "zorititfkbhrlmpv"
                       :port 587
                       :tls  true}
    {:from    "toromanj+gajbe@gmail.com"
     :to      [to]
     :subject tema
     :body    [{:type    "text/html; charset=utf-8"
                :content sadrzaj}]}))
