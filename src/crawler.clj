(ns crawler
  (:require [clj-http.client :as http]
            [hickory.core :as h]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            )
  (:import (org.w3c.tidy.Tidy)
           (org.jsoup Jsoup)))

(defn fetch-page [page class-value]
  (:body (http/post
           "http://www.impic.pt/impic/ajax/call/impic_api/consultar/ajax/43"
           {:form-params {"id_object" "17"
                          "pesquisar" "true"
                          "pageSearch" page
                          "classe_maxima" class-value}
            :headers {"Accept-Encoding" "deflate, gzip"
                      "Accept" "*/*"
                      "Content-Type" "application/x-www-form-urlencoded"
                      }
            })))

(def m-fetch-page
  (memoize fetch-page))

(defn fetch-all-class [class-value]
  (loop [page 1
         accum []]
    (printf "%s-%s\n" page (count accum))
    (let [page-html (m-fetch-page page class-value)]
      (if (clojure.string/blank? page-html)
        accum
        (recur (inc page) (conj accum page-html))))))

(defn parse-class 
  [class-str]
  (let [html (str "<table>" (clojure.string/join class-str) "</table>")]
    (def c html)
   (let [lines (-> (Jsoup/parseBodyFragment html)
                  (.select  "body > table > tbody > tr"))]
     (map parse-line lines))))

(defn parse-line [line]
  (def l line)
  (let [cell (.select line "td")]
    {:id (.text (nth cell 0))
     :nif (.text (nth cell 1))
     :nome (.text (nth cell 2))
     :morada (.text (nth cell 3))
     :classe max-class}))

(defn get-class [class-value]
  (def max-class class-value)
  (-> class-value
      (fetch-all-class)
      (parse-class)
  ))

(defn write-csv [path row-data]
  (let [columns [:id :nif :nome :morada :classe]
        headers (map name columns)
        rows (mapv #(mapv % columns) row-data)]
    (with-open [file (io/writer path)]
      (csv/write-csv file (cons headers rows)))))

(doall (map #(write-csv (str "resources/results_" % ".csv") (get-class %)) (range 1 10)))