(defproject mastodon "0.0.1-SNAPSHOT"
  :description "Move your hadoop to the best cloud centre."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/math.numeric-tower "0.0.1"
                  :exclusions [org.clojure/clojure]]
                 [ring "1.1.0"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib
                               clj-stacktrace
                               hiccup]]
                 [compojure "1.0.3"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib
                               hiccup]]
                 [clj-time "0.4.3"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib]]
                 [com.novemberain/monger "1.1.0-rc1"]
                 [org.clojure/data.json "0.1.2"]
                 [hiccup "1.0.0"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib]]
                 [com.cemerick/friend "0.0.9"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib]]
                 [org.clojure/tools.logging "0.2.3"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib]]
                 [log4j/log4j "1.2.16"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib]]
                 [ororo "0.1.0"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib]]
                 [clj-http "0.4.1"
                  :exclusions [org.clojure/clojure
                               org.clojure/data.json
                               org.clojure/clojure-contrib]]]
  :dev-dependencies [[ring-mock "0.1.1"]]
  :plugins [[lein-ring "0.7.5"]]
  :ring {:handler mastodon.site/application
         :init mastodon.site/init})
