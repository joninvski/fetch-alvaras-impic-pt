(ns crawler
  (:require [clj-http.client :as http]
            [hickory.core :as h]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            )
  (:import (org.w3c.tidy.Tidy)
           (org.jsoup Jsoup)))

(def id-private 18)
(def id-public 17)

(defn fetch-page [page class-value id-object]
  (:body (http/post
           "http://www.impic.pt/impic/ajax/call/impic_api/consultar/ajax/43"
           {:form-params {"id_object" (str id-object)
                          "pesquisar" "true"
                          "pageSearch" page
                          "classe_maxima" class-value}
            :headers {"Accept-Encoding" "deflate, gzip"
                      "Accept" "*/*"
                      "Content-Type" "application/x-www-form-urlencoded"
                      }})))

(defn fetch-single-company [link-id id-object]
  (:body (http/post
           "http://www.impic.pt/impic/ajax/call/impic_api/consultar/ajax/43"
           {:form-params {"id_object" (str id-object)
                          "id_type" "8"
                          "informacao" "true"
                          "info" "NUMERO_LICENCA"
                          "value" (str link-id)
                          "nl" (str link-id)}
            :headers {"Accept-Encoding" "deflate, gzip"
                      "Accept" "*/*"
                      "Content-Type" "application/x-www-form-urlencoded"
                      }})))

(def m-fetch-page
  (memoize fetch-page))

(defn fetch-all-class [class-value id-object]
  (loop [page 1
         accum []]
    (printf "%s-%s\n" page (count accum))
    (let [page-html (m-fetch-page page class-value id-object)]
      (if (clojure.string/blank? page-html)
        accum
        (recur (inc page) (conj accum page-html))))))

(def max-class 0)

(defn parse-line [line id-object]
  (def l line)
  (let [cell (into [] (.select line "td"))
        link-id (.get
                  (.attributes
                    (first (.getElementsByTag (nth cell 0) "a"))) "data-value")]
    {:id (.text (nth cell 0))
     :phone (second (clojure.string/split (.text (-> (Jsoup/parseBodyFragment (fetch-single-company link-id id-object))
                                                     (.select "body > div > div > div:nth-child(10)")))
                                          #" "))
     :email (second (clojure.string/split (.text (-> (Jsoup/parseBodyFragment (fetch-single-company link-id id-object))
                                                     (.select "body > div > div > div:nth-child(12)")))
                                          #" "))
     :nif (.text (nth cell 1))
     :nome (.text (nth cell 2))
     :morada (.text (nth cell 3))
     :classe max-class}))

(defn parse-class
  [class-str id-object]
  (let [html (str "<table>" (clojure.string/join class-str) "</table>")]
    (def c html)
   (let [lines (-> (Jsoup/parseBodyFragment html)
                  (.select  "body > table > tbody > tr"))]
     (map #(parse-line % id-object) lines))))

(defn get-class [class-value id-object]
  (def max-class class-value)
  (parse-class
    (fetch-all-class class-value id-object)
    id-object))

(defn write-csv [path row-data]
  (let [columns [:id :nif :nome :morada :classe :phone :email]
        headers (map name columns)
        rows (mapv #(mapv % columns) row-data)]
    (with-open [file (io/writer path)]
      (csv/write-csv file (cons headers rows)))))

(defn fetch-all-public-works
  []
  (doall
    (map #(write-csv (str "resources/public_works_results_" % ".csv")
                     (get-class % id-public))
         (range 1 9))))

(defn fetch-all-private-works
  []
  (doall
    (map #(write-csv (str "resources/private_works_results_" % ".csv")
                     (get-class % id-private))
         (range 1 9))))
