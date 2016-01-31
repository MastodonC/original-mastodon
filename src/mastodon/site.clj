(ns mastodon.site
  (:use [compojure.core :only [defroutes HEAD GET PUT POST ANY context]]
        [ring.util.response :only [redirect]]
        [clojure.tools.logging :only (infof errorf)]
        [monger.ring.session-store :only (session-store)])
  (:require
   [monger.core :as mongo]
   [mastodon.storage :as storage]
   [mastodon.site.views :as views]
   [mastodon.site.user-management :as user-management]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.resource :as rms]
   [ring.middleware.params :as rmp]
   [ring.middleware.file-info :as rmfi]
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])))

(defn http-scheme [handler environment-variable]
  "Decide whether to use HTTPS or HTTP to build the handler based on the HTTP_SCHEME environment variable. 
   If the environment variable is not set, it defaults to HTTP and returns the handler."
   (if (= environment-variable :https)
     (friend/requires-scheme-with-proxy handler :https)
     handler))

(defroutes application-routes
  (GET "/preferences" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/preferences-page request)})
  ;; (POST "/preferences" request
  ;;       (change-application-preferences! request))
  ;; Saved/Running Hadoop Jobs
  ;; Replicated S3 Jobs
  )

(defroutes user-routes
  (GET "/profile" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (user-management/profile-page request)})
  (POST "/profile" request
        (user-management/change-user-profile! request))
  (GET "/change-password" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (user-management/change-password-page request)})
  (POST "/change-password" [old-password new-password confirm-new-password :as r]
        (user-management/change-password! old-password new-password confirm-new-password r)))

(defroutes routes
  ;; Site Basics
  (GET "/" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/home-page request)})
  (GET "/insight" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/insight-page request)})
  (GET "/kixi" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/kixi-page request)})
  (GET "/contact" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/contact-page request)})
  (POST "/contact" [email name message :as r]
        (views/contact-us email name message r))
  (GET "/sustainability" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/sustainability-page request)})
  (GET "/team" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/team-page request)})
  (GET "/projects" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/projects-page request)})
  
  (GET "/about" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/about-page request)})
  (GET "/faq" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/faq-page request)})
  (GET "/consultancytandcs" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/conditions-page request)})
  (POST "/quickstart" [email name message :as r]
        (views/quickstart email name message r))
  (GET "/quickstart" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/quickstart-page request)})
  (GET "/thanks" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/thanks-page request)})
  (POST "/register-interest" request
        ;; {params :params remote-addr :remote-addr headers :headers}
        (views/register-interest request))

  ;; Login/Logout/Register/Forgotten Password
  (GET "/login" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (user-management/login-page request)
        })
  (friend/logout (ANY "/logout" request (redirect "/")))
  (GET "/register" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (user-management/register-page request)
        })
  (POST "/register" [email password confirm_password :as r]
        (user-management/register-user email password confirm_password r))
  (GET "/forgot-password" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (user-management/forgot-password-page request)
        })
  (POST "/forgot-password" [email :as r]
        (user-management/forgot-password email r))
  (GET "/reset-password/:id" [id :as request]
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (user-management/reset-password-page id request)
        })
  (POST "/reset-password" [temp-id new-password confirm-new-password :as r]
        (user-management/reset-password! temp-id new-password confirm-new-password r))

  ;; Account Profile/Change Password
  (context "/user" request (friend/wrap-authorize user-routes #{"user"}))

  ;; The Application
  (GET "/dashboard" request
       {:status 200
        :headers {"Content-Type" "text/html"}
        :body (views/dashboard-page request)})
  (context "/application" request (friend/wrap-authorize application-routes #{"user"}))

  ;; API
  (GET "/dc_rankings" request
       {:status 200
        :headers {"Content-Type" "application/json"}
        :body (views/datacentre-ranking-json request)})

  ;; Everything Else
  (route/resources "/")

  (route/files "/" {:root "resources"})
  ;;(route/not-found (views/not-found-404-page request))
  (HEAD "*" [] {:status 404})
  (ANY "*" request {:status 404, :body (views/not-found-404-page request)}))

(def application
  (-> routes
      (rms/wrap-resource "public")
      rmfi/wrap-file-info
      (friend/authenticate
       {:credential-fn (partial creds/bcrypt-credential-fn storage/get-user)
        :unauthorized-redirect-uri "/login"
        :login-uri "/login"
        :default-landing-uri "/dashboard"
        :workflows [(workflows/interactive-form :login-uri "/login")]})
      
      (http-scheme (keyword (System/getenv "HTTP_SCHEME")))
      
      (handler/site {:session {:store (session-store "sessions")}})))

(defn init []
  (storage/init-mongo!)
  (infof "Using mongodb: [%s]" mongo/*mongodb-connection*))

(defn start [port]
  ;; fire up jetty
  (jetty/run-jetty (var application) {:port (or port 8080) :join? false}))

(defn -main []
  (let [port (Integer/parseInt (System/getenv "PORT"))]
    ;; connect to mongolab
    (storage/init-mongo!)
    (infof "Using mongodb: [%s]" mongo/*mongodb-connection*)
    (infof "Starting on port: [%d]." port)
    (start port)))
