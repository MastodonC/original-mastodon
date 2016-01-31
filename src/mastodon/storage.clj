(ns mastodon.storage
  (:use [clojure.tools.logging :only (infof errorf)])
  (:require
   [monger.core :as mongo]
   [monger.query :as query]
   [monger.collection :as collections]))

(defn init-mongo! []
  (let [mongo-url (get (System/getenv) "MONGOLAB_URI")]
    (mongo/connect-via-uri! mongo-url)
    (infof "Using mongodb: [%s]" mongo/*mongodb-connection*)
    (mongo/set-db! (mongo/get-db))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User manipulation
(defn add-user! [user]
  (collections/insert :users user))

(defn update-user! [old-user new-user]
  (collections/update :users old-user new-user :multi false))

(defn get-user [username]
  (collections/find-one-as-map :users {:email username}))

(defn get-user-from-temp-id [temp-key]
  (collections/find-one-as-map :users {:temp-key temp-key}))

(defn get-user-count [email]
  (collections/count :users {:email email}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lead management
(defn add-lead! [signup-info]
  (collections/insert :leads signup-info))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Weather information management
(defn add-forecasts! [forecasts]
  (infof "Adding %d forecasts" (count forecasts))
  (collections/insert-batch :weather forecasts))

(defn get-last-forecast [location]
  (infof "Getting weather for %s" location)
  (first (query/with-collection "weather"
           (query/find {:location location})
           (query/sort {:time -1})
           (query/limit 1))))

(defn get-latest-forecasts [locations]
  (reduce (fn [acc dc] (assoc acc dc (get-last-forecast dc))) {} locations))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Rankings
(defn add-ranking! [ranking]
  (infof "Ranking %s" ranking)
  (collections/insert :ranking ranking))

(defn get-last-ranking []
  (first (query/with-collection "ranking"
           (query/sort {:tstamp -1})
           (query/limit 1))))
