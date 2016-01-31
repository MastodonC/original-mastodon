(ns mastodon.weather.core
  (:use [clojure.tools.logging :only (infof errorf)])
  (:require [mastodon.core :as mcore]
            [mastodon.storage :as storage]
            [mastodon.datacentres :as datacentres]
            [mastodon.carbon :as carbon]
            [mastodon.pricing :as pricing]
            [ororo.core :as ororo]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(defn wunderground-key []
  (get (System/getenv) "WUNDERGROUND_KEY"))

(defn fetch-forecast [location]
  "This gets the time and temperature for a datacentre from
   weatherunderground.com for the next 36 hours."
  (infof "Fetching weather for %s" location)
  (let [full-forecast (ororo/hourly (wunderground-key) location)]
    (map
     (fn [w] {:epoch (Long/parseLong (get-in w [:FCTTIME :epoch]))
             :pretty (get-in w [:FCTTIME :pretty])
             :temp (Integer/parseInt (get-in w [:temp :metric]))})
     full-forecast)))

;; Fetch forecasts for the given site names (e.g. iceland london), this function uses the location-from-site-name to get the location of a given site
(defn fetch-all-forecasts [sites]
  (let [now (coerce/to-long (time/now))]
    (vec (filter
          (fn [fc] (< 0 (count (:forecast fc))))
          (map
            (fn [location] {:location location :time now :forecast (fetch-forecast location)}) (map datacentres/location-from-site-name sites))))))

(defn store-latest-forecasts! [sites]
  (->> (fetch-all-forecasts sites)
       storage/add-forecasts!))

(defn store-latest-rankings! []
  (let [weather (storage/get-latest-forecasts (datacentres/locations))
        rankings (mcore/rank-datacentres :m1.xlarge weather :kgCO2e-per-hour)]
    (storage/add-ranking! {:tstamp (coerce/to-long (time/now)) :rankings rankings})))

(defn run [sites]
  (infof "Getting weather for %s" (time/now))
  (storage/init-mongo!)
  (store-latest-forecasts! sites)
  (store-latest-rankings!))

;; Main of the weather.core that takes CLI argument of site names (e.g. iceland chicago) as an argument
(defn -main [& sites]
  (run (set sites))
  (System/exit 0))
