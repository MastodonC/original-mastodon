(ns mastodon.pricing
  (:use [clojure.tools.logging :only (infof errorf)])
  (:require [mastodon.datacentres :as datacentres]))

(def instance-specs
  {:greenqloud {:m1.small {:ram 2.0 :cores 2.0 :hdd 80 :io 2.0}
                :m1.medium {:ram 4.0 :cores 4.0 :hdd 160 :io 2.0}
                :m1.large {:ram 8.0 :cores 8.0 :hdd 320 :io 3.0}
                :m1.xlarge {:ram 15.5 :cores 16.0 :hdd 640 :io 3.0}}
   :aws {:m1.small {:ram 1.7 :cores 1.0 :hdd 160 :io 2.0}
         :m1.medium {:ram 3.75 :cores 2.0 :hdd 410 :io 2.0}
         :m1.large {:ram 7.5 :cores 4.0 :hdd 850 :io 3.0}
         :m1.xlarge {:ram 15 :cores 8.0 :hdd 1690 :io 3.0}
         :m2.xlarge {:ram 17.1 :cores 6.5 :hdd 420 :io 2.0}
         :m2.2xlarge {:ram 34.2 :cores 13.0 :hdd 850 :io 3.0}
         :m2.4xlarge {:ram 68.4 :cores 26.0 :hdd 1690 :io 3.0}
         :c1.medium {:ram 1.7 :cores 5.0 :hdd 350 :io 2.0}
         :c1.xlarge {:ram 7.0 :cores 20.0 :hdd 1690 :io 3.0}
         :cc1.4xlarge {:ram 23.0 :cores 33.5 :hdd 1690 :io 4.0}
         :cc1.8xlarge {:ram 60.5 :cores 88.0 :hdd 3370 :io 4.0}
         :cg1.4xlarge {:ram 22.0 :cores 33.5 :hdd 1690 :io 4.0}}})

(defn hourly-price [site provider instance-type]
  (get-in (datacentres/datacentre site provider) [:pricing instance-type]))


;; (defn instance-performance
;;   "Given a map of {:ram 2.0 :cores 2.0 :hdd 80 :io 2.0} calculate the performance"
;;   [vendor instance-type]
;;   (let [spec (get-in instance-specs [vendor instance-type])
;;         {:keys [ram cores hdd io]} spec]
;;     (+ ram cores io)))

;; (defn performance-price-index [vendor location instance-type]
;;   (let [price (get-hourly-price location instance-type)
;;         performance (instance-performance vendor instance-type)]
;;     (/ performance price)))

;; (defn cost-per-hour [location instance-type]
;;   {:cost (get-in instance-pricing [location instance-type])})

;; Need to do something with the weightings of the different machines
;; (defn weighted-cost-per-hour [location instance-type cost-per-carbon-kg temperature]
;;   (let [weighted-compute-cost-hr (get-in instance-pricing [location instance-type])
;;         kgCO2e (kgCO2e-per-hour location temperature)
;;         carbon-cost-hr (* kgCO2e cost-per-carbon-kg)]
;;     {:cost (+ compute-cost-hr carbon-cost-hr) :kgCO2e kgCO2e :temp temperature}))
