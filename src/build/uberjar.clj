(ns uberjar
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def srcs (:paths basis))
(def uber-file "target/nekretninice.jar")

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs   srcs
               :target-dir class-dir})
  (b/compile-clj {:basis     basis
                  :src-dirs  srcs
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'nekretninice.poslovi}))
