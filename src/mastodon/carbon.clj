(ns mastodon.carbon
  (:use [clojure.tools.logging :only (infof errorf)])
  (:require [mastodon.datacentres :as datacentres]))

;;https://docs.google.com/spreadsheet/ccc?key=0An_iartERv2ldDNONmNLLVdDS2QzRjk5a0pORWIzV3c
(def forecast-hours 6)

(def base-server-kWh 0.38)
(def max-cooling-kWh 0.285)
(def default-pue 1.5)

(def no-cooling-temp 21.0)
(def max-cooling-temp 31.0)

(defn cooling-kWh
  [temperature]
  (if (<= temperature no-cooling-temp)
    0.0
    (if (> temperature max-cooling-temp)
      max-cooling-kWh
      (* (/ (- temperature no-cooling-temp) 10) max-cooling-kWh))))

(defn server-kWh
  [temperature]
  (+ (* default-pue base-server-kWh) (cooling-kWh temperature)))

(defn kgCO2e [site provider]
  (infof "Getting CO2 for %s %s" site provider)
  (:kgCO2e (datacentres/datacentre site provider)))

(defn kgCO2e-per-hour [kgCO2e temperature]
  (* kgCO2e (server-kWh temperature)))

(defn CO2e-forecast [kgCO2e temps]
  (map #(kgCO2e-per-hour kgCO2e  %) temps))

(defn mean-CO2e [kgCO2e temps]
  (let [{sum :sum count :count} (reduce (fn [acc new] {:sum (+ new (:sum acc)) :count (inc (:count acc))}) {:sum 0 :count 0} (CO2e-forecast kgCO2e temps))]
    (/ sum count)))

(defn datacentre-emissions-forecast
  ([site provider weather]
     "Return a map of:
  {:provider :aws :location :virginia :time time :kgCO2e-per-hour 0.2581}
  based on a passed in forecast."
     (let [location (:location (datacentres/datacentre site provider))
           temps (map #(:temp %) (take forecast-hours (get-in weather [location :forecast])))
           start (get-in weather [location :time])
           end (+ start (* forecast-hours 60 60 1000))
           co2e (kgCO2e site provider)]
       {:provider provider :location site :valid-from start :valid-to end :kgCO2e-per-hour (mean-CO2e co2e temps)}))
  ([datacentres weather]
     (map #(datacentre-emissions-forecast (:site %) (:provider %) weather) datacentres)))
