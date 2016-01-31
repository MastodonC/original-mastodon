(ns mastodon.datacentres
  (:use [clojure.tools.logging :only (infof errorf)]))

;; Map for Rackspace pricing (as it is the same for all of the sites)
(def rackspace-pricing {:m1.small 0.12 :m1.medium 0.24 :m1.large 0.48 :m1.xlarge 0.96})

;; Map for Windows Azure pricing (as it is the same for all of the Azure sites)
(def azure-pricing {:m1.small 0.08 :m1.medium 0.16 :m1.large 0.32 :m1.xlarge 0.64})

;; CO2 from http://discover.amee.com/categories/International_electricity_by_DEFRA
;; and
;; http://discover.amee.com/categories/EPA_eGRID_by_state
(def datacentre-details
  ;; kgCO2e is per kWh
  ;; kgCO2 for national grids is "direct CO2 Rolling Basis"
  [
   
   ;; Greenqloud - Kaflavik, Iceland
   {:site :iceland :provider :greenqloud
    :description "Iceland" :region "is-1" :location ["Keflavik" "Iceland"] :kgCO2e 0.00002
    :pricing {:m1.small 0.072 :m1.medium 0.144 :m1.large 0.282 :m1.xlarge 0.576}}
   
   ;; AWS - Portland, Oregon
   {:site :oregon :provider :aws
    :description "Oregon" :region "us-west-2" :location ["Portland" "Oregon"] :kgCO2e 0.197
    :pricing { ;; Standard On-Demand
              :m1.small 0.08 :m1.medium 0.16 :m1.large 0.32 :m1.xlarge 0.64
              ;; Hi-Memory On-Demand
              :m2.xlarge 0.45 :m2.2xlarge 0.90 :m2.4xlarge 1.8
              ;; Hi-CPU On-Demand
              :c1.medium 0.165 :c1.xlarge 0.660}}
   
   ;; AWS - San Francisco, California
   {:site :bay-area :provider :aws
    :description "Bay Area" :region "us-west-1" :location ["San Francisco" "California"] :kgCO2e 0.271
    :pricing { ;; Standard On-Demand
              :m1.small 0.09 :m1.medium 0.18 :m1.large 0.36 :m1.xlarge 0.72
              ;; Hi-Memory On-Demand
              :m2.xlarge 0.506 :m2.2xlarge 1.012 :m2.4xlarge 2.024
              ;; Hi-CPU On-Demand
              :c1.medium 0.186 :c1.xlarge 0.744}}
   
   ;; AWS - Herndon, Virginia
   {:site :virginia :provider :aws
    :description "Virginia" :region "us-east-1" :location ["Herndon" "Virginia"] :kgCO2e 0.555
    :pricing { ;; Standard On-Demand
              :m1.small 0.08 :m1.medium 0.16 :m1.large 0.32 :m1.xlarge 0.64
              ;; Hi-Memory On-Demand
              :m2.xlarge 0.45 :m2.2xlarge 0.90 :m2.4xlarge 1.8
              ;; Hi-CPU On-Demand
              :c1.medium 0.165 :c1.xlarge 0.660
              ;; Cluster Compute
              :cc1.4xlarge 1.3 :cc1.8xlarge 2.4
              ;; Cluster GPU
              :cg1.4xlarge 2.1}}
   
   ;; AWS - Sao Paulo, Brazil
   {:site :sao-paulo :provider :aws
    :description "São Paulo" :region "sa-east-1" :location ["Sao Paulo" "Brazil"] :kgCO2e 0.106
    :pricing { ;; Standard On-Demand
              :m1.small 0.115 :m1.medium 0.230 :m1.large 0.46 :m1.xlarge 0.92
              ;; Hi-Memory On-Demand
              :m2.xlarge 0.68 :m2.2xlarge 1.36 :m2.4xlarge 2.72
              ;; Hi-CPU On-Demand
              :c1.medium 0.230 :c1.xlarge 0.920}}
   
   ;; AWS - Dublin, Ireland
   {:site :dublin :provider :aws
    :description "Dublin" :region "eu-west-1" :location ["Dublin" "Ireland"] :kgCO2e 0.527
    :pricing { ;; Standard On-Demand
              :m1.small 0.09 :m1.medium 0.18 :m1.large 0.36 :m1.xlarge 0.72
              ;; Hi-Memory On-Demand
              :m2.xlarge 0.506 :m2.2xlarge 1.012 :m2.4xlarge 2.024
              ;; Hi-CPU On-Demand
              :c1.medium 0.186 :c1.xlarge 0.744}}
   
   ;; AWS - Tokyo, Japan
   {:site :tokyo :provider :aws
    :description "Tokyo" :region "ap-northeast-1" :location ["Tokyo" "Japan"] :kgCO2e 0.46
    :pricing { ;; Standard On-Demand
              :m1.small 0.092 :m1.medium 0.184 :m1.large 0.368 :m1.xlarge 0.736
              ;; Hi-Memory On-Demand
              :m2.xlarge 0.518 :m2.2xlarge 1.036 :m2.4xlarge 2.072
              ;; Hi-CPU On-Demand
              :c1.medium 0.19 :c1.xlarge 0.76}}
   
   ;; AWS - Singapore, Singapore
   {:site :singapore :provider :aws
    :description "Singapore" :region "ap-southeast-1" :location ["Singapore" "Singapore"] :kgCO2e 0.563
    :pricing { ;; Standard On-Demand
              :m1.small 0.09 :m1.medium 0.18 :m1.large 0.36 :m1.xlarge 0.72
              ;; Hi-Memory On-Demand
              :m2.xlarge 0.506 :m2.2xlarge 1.012 :m2.4xlarge 2.024
              ;; Hi-CPU On-Demand
              :c1.medium 0.186 :c1.xlarge 0.744}}
   
   ;; Rackspace - Chicago, Illinois, US
   {:site :chicago :provider :rackspace 
    :description "Chicago" :location ["Chicago" "Illinois"] :kgCO2e 0.54
    :pricing rackspace-pricing}
   
   ;; Rackspace - Dallas, Texas, US
   {:site :dallas :provider :rackspace
    :description "Dallas" :location ["Dallas" "Texas"] :kgCO2e 0.636
    :pricing rackspace-pricing}
   
   ;; Rackspace - London, United Kingdom
   {:site :london :provider :rackspace
    :description "London" :location ["London" "United Kingdom"] :kgCO2e 0.00012
    :pricing rackspace-pricing}
   
   ;; Azure - Chicago, Illinois
   {:site :chicago :lat-lon [41.90 -89.15] :provider :azure
    :description "Chicago" :location ["Chicago" "Illinois"] :kgCO2e 0.54
    :pricing azure-pricing}
   
   ;; Azure - San Antonio, Texas
   {:site :san-antonio :provider :azure
    :description "San Antonio" :location ["San Antonio" "Texas"] :kgCO2e 0.636
    :pricing azure-pricing}
   
   ;; Azure - Hong Kong, China
   {:site :hong-kong :provider :azure
    :description "Hong Kong" :location ["Hong Kong" "China"] :kgCO2e 0.54
    :pricing azure-pricing}
   
   ;; Azure - Singapore, Singapore
   {:site :singapore :lat-lon [0.90 104.45] :provider :azure
    :description "Singapore" :location ["Singapore" "Singapore"] :kgCO2e 0.563
    :pricing azure-pricing}
   
   ;; Azure - Amsterdam, Netherlands
   {:site :amsterdam :provider :azure
    :description "Amsterdam" :location ["Amsterdam" "Netherlands"] :kgCO2e 0.423
    :pricing azure-pricing}
   
   ;; Azure - Dublin, Ireland
   {:site :dublin :lat-lon [53.4 -7.7] :provider :azure
    :description "Dublin" :location ["Dublin" "Ireland"] :kgCO2e 0.527
    :pricing azure-pricing}
   
   ;; Azure - Bay Area, California, US
   {:site :bay-area :provider :azure :lat-lon [38.8 -121.4192]
    :description "Bay Area" :location ["San Francisco" "California"] :kgCO2e 0.271
    :pricing azure-pricing}
   
   ;; Azure - Northern Virginia, US
   {:site :virginia :lat-lon [39 -78.7] :provider :azure
    :description "Virginia" :location ["Herndon" "Virginia"] :kgCO2e 0.555
    :pricing azure-pricing}

   ;; Google
   ;; Brightbox
   ;; Greenhouse
   ;; cloud.dk
   ])

(def provider-descriptions
  {:aws "aws" :greenqloud "Greenqloud"
   :rackspace "Rackspace" :azure "Azure"
   :google "Google" :brightbox "BrightBox"
   :greenhouse "Greenhouse" :clouddk "cloud.dk"})

(defn locations []
  (into #{} (map (fn [d] (:location d)) datacentre-details)))

(defn providers []
  (into #{} (map (fn [d] (:provider d)) datacentre-details)))

(defn sites []
  (into #{} (map (fn [d] (:site d)) datacentre-details)))

(defn datacentres []
  (into #{} (map (fn [d] {:site (:site d) :provider (:provider d)}) datacentre-details)))

(defn datacentre [site provider]
  (first (filter #(and (= (:site %) site)
                       (= (:provider %) provider)) datacentre-details)))

(defn formatted-location [site provider]
           (let [[loc1 loc2] (get (datacentre site provider) :lat-lon (:location (datacentre site provider)))]
             (format "%s, %s" loc1 loc2)))

(defn region [site provider]
  (:region (datacentre site provider)))

(defn instance-types [site provider]
  (into #{} (keys (:pricing (datacentre site provider)))))

(defn site-description [site provider]
  (if (and (= site :london) (= provider :rackspace))
    (format "%s (%s) **" (:description (datacentre site provider)) (provider provider-descriptions))
    (format "%s (%s)" (:description (datacentre site provider)) (provider provider-descriptions))))

;; Method for matching Datacentre locations by their site names, 
;; implemented to fix the bug with weatherunderground limit of 10 requests per 1 min 
(defn location-from-site-name [site]
  (:location
    (first (filter #(= (:site %) (keyword site)) datacentre-details))))