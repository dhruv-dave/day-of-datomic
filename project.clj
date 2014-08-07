(defproject day-of-datomic "1.0.0-SNAPSHOT"
  :description "Sample Code for Day of Datomic Presentation"
  :plugins [[lein-tg "0.0.1"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/test.generative "0.3.0"]
                 [com.datomic/datomic-pro "0.9.4815.12"
                  :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]
                 [incanter/incanter-charts "1.3.0"]
                 [incanter/incanter-pdf "1.3.0"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [midje "1.6.3"]
                                  [rhizome "0.2.0"]]}}
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}})
