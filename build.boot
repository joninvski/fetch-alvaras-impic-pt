 (set-env!  
  :resource-paths #{"src"}
  :dependencies '[[clj-http "3.6.1"]
                  [hickory "0.7.1"]
                  [xenopath "0.1.2"]
                  [net.sf.jtidy/jtidy "r938"]
                  [org.jsoup/jsoup "1.7.3"]
                  [org.clojure/data.csv "0.1.4"]
                  ])
