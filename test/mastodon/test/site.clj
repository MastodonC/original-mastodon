(ns mastodon.test.site
  (:use [mastodon.site]
        [ring.mock.request]
        [clojure.test]))

(deftest test-requires-scheme
  (doseq [ports [nil {:http 8080 :https 8443}]
          p [:http :https]
          :let [other ({:http :https :https :http} p)]]
    (let [h (binding [*default-scheme-ports* (or ports *default-scheme-ports*)]
              (requires-scheme-with-proxy (constantly "response") p))
          {{:strs [Location]} :headers :as resp} (h (assoc (header (request :get "/any?a=5") "x-forwarded-proto" other) :scheme :http))
          location (and Location (java.net.URL. Location))]
      (is (= 302 (:status resp)))
      (is (= (name p) (.getProtocol location)))
      (is (= (get ports p -1) (.getPort location)))
      (is (= "/any" (.getPath location)))
      (is (= "a=5" (.getQuery location))))))