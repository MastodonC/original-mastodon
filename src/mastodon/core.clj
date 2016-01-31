(ns mastodon.core
  (:use [clojure.tools.logging :only (infof errorf)])
  (:require [mastodon.datacentres :as datacentres]
            [mastodon.carbon :as carbon]
            [mastodon.pricing :as pricing]))

;; our Mastodon is called Buskeeper

(defn rate-datacentre
  "Return a map of:
  {:site :virginia :provider :aws :instance-type :m1.xlarge :cost-per-hour 0.649 :kgCO2e-per-hour 0.2581}
  based on a passed in forecast."
  [site provider instance-type weather]
  (let [{:keys [valid-from valid-to kgCO2e-per-hour]} (carbon/datacentre-emissions-forecast site provider weather)]
    {:site site
     :provider provider
     :instance-type instance-type
     :valid-from valid-from
     :valid-to valid-to
     :kgCO2e-per-hour kgCO2e-per-hour
     :cost-per-hour (pricing/hourly-price site provider instance-type)}))

(defn rate-all-datacentres [instance-type weather]
  (let [sites (datacentres/datacentres)]
    (map #(rate-datacentre (:site %) (:provider %) instance-type weather) sites)))

(defn rank-datacentres [instance-type weather sort-key]
  (let [ratings (rate-all-datacentres instance-type weather)]
    (sort-by sort-key ratings)))

;; TODO data for API
(defn datacentre-info []
  [{:location :virginia :vendor :aws :kgCO2e-per-hour 0.285 :rank 2
    :server-types [{:type :m1.xlarge :on-demand 0.64 :one-wk-spot-average 0.216}
                   {:type :m1.smallo :on-demand 0.08 :one-wk-spot-average 0.021}]}
   {:location :orgenon :vendor :aws :kgCO2e-per-hour 0.285 :rank 1
    :server-types [{:type :m1.xlarge :on-demand 0.64 :one-wk-spot-average 0.216}
                   {:type :m1.smallo :on-demand 0.08 :one-wk-spot-average 0.021}]}])
