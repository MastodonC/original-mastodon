(ns mastodon.email
  (:use [clojure.tools.logging :only (infof errorf)])
  (:require [clj-http.client :as http]))

(defn send-email [from to subject text]
  (http/get
   "https://sendgrid.com/api/mail.send.json"
   {:accept :json
    :query-params
    {:api_user (System/getenv "SENDGRID_USERNAME")
     :api_key (System/getenv "SENDGRID_PASSWORD")
     :to to
     :subject subject
     :text text
     :from from}}))
