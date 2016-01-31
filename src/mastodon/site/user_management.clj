(ns mastodon.site.user-management
  (:use
   [ring.util.response :only [redirect]]
   [clojure.tools.logging :only (infof errorf)]
   [clojure.tools.logging.impl :only (log4j-factory)])
  (:require
   [hiccup.core :as hc]
   [hiccup.form :as form]
   [hiccup.page :as page]
   [hiccup.element :as element]
   [clj-time.core :as d]
   [clj-http.client :as http]
   [mastodon.site.views :as views]
   [mastodon.storage :as storage]
   [cemerick.friend :as friend]
   [cemerick.friend.credentials :as creds]
   [clojure.string :as string]))

(defn conj-when [coll test x]
  (if test
    (conj coll x)
    coll))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login Page and Form
(defn login-form [request]
  (let [login-failed (= "Y" (get-in request [:params :login_failed]))
        username (get-in request [:params :username])]
    [:div
     [:h3 "Log In"]
     (when login-failed
       [:div.alert.alert-error
        [:a.close {:data-dismiss "alert"} "x"]
        [:strong "Whoops! "] "Please enter a valid email address and password."])
     [:form.well {:action "/login" :method "POST"}
      (form/label "Email Address" "Email Address")
      [:input {:type "email" :name "username" :placeholder "Your Email Address" :value (when username username)}]
      (form/label "Password" "Password")
      [:input {:type "password" :name "password" :placeholder "Your Password"}]
      [:br]
      (form/submit-button "Log In")
      [:p (element/link-to "/forgot-password" "Forgotten your password?") " | " (element/link-to "/register" "Don't have an account?")]]]))

(defn login-page [request]
  (infof "Login Page request: [%s]" request)
  (views/page
   {:title "Login"
    :request request
    :body-content
    [:div.container
     [:div.span6.offset3
      (login-form request)]]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Register a new user
(defn register-form [request & [errors email]]
  ;; [errors email]
  [:div
   [:h3 "Register"]
   (views/error-list errors)
   [:form.well {:action "/register" :method "POST"}
    (form/label "Email Address" "Email Address")
    [:input {:type "email" :name "email" :placeholder "Your Email Address" :value (when email email)}]
    (form/label "Password" "Password")
    (form/password-field "password")
    (form/label "Confirm Password" "Confirm Password")
    (form/password-field "confirm_password")
    [:br]
    (form/submit-button "Register")]])

(defn register-page [request & [errors email]]
  ;;[& [errors email]]
  (views/page
   {:title "Register"
    :request request
    :body-content
    [:div.container
     [:div.span6.offset3
      (register-form request errors email)]]}))

(defn existing-account? [email]
  (not (zero? (storage/get-user-count email))))

(defn validate-profile [email password confirm]
  (-> nil
      (conj-when (string/blank? email) "Email can't be blank.")
      (conj-when (< (count password) 5) "Password must be at least 5 characters long.")
      (conj-when (string/blank? password) "Password can't be blank.")
      (conj-when (not= password confirm)
                 "Password and password confimation must match")
      (conj-when (existing-account? email)
                 "A user with that email address already exists.")))

(defn register-user [email password confirm-password request]
  (let [referer (get-in request [:headers "referer"])]
    (infof "Registering user: [%s] with P: [%s] and CP: [%s]" email password confirm-password)
    (infof "Request: [%s]" request)
    (if-let [errors (validate-profile email password confirm-password)]
      (register-page request errors email)
      (do
        (storage/add-user! {:identity email
                            :email email
                            :api-key (str (java.util.UUID/randomUUID))
                            :password (creds/hash-bcrypt password)
                            :roles #{"user"}})
        (let [response (redirect "/dashboard")]
          ;; Create the authentications map as if we'd logged in
          (assoc-in response [:session :cemerick.friend/identity]
                    {:current email
                     :authentications
                     {email
                      {:identity email
                       :email email
                       :roles ["user"]}}}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Change existing user's password
(defn change-password-page
  ([request errors]
     (views/page
      {:title "Change Password"
       :request request
       :body-content
       [:div.container
        [:div.span6.offset3
         [:h3 "Change Password"]
         (views/error-list errors)
         [:form.well {:action "/user/change-password" :method "POST"}
          (form/label "Old Password" "Old Password")
          (form/password-field "old-password")
          (form/label "New Password" "New Password")
          (form/password-field "new-password")
          (form/label "Confirm New Password" "Confirm New Password")
          (form/password-field "confirm-new-password")
          [:br]
          (form/submit-button "Change Password")]]]}))
  ([request]
     (change-password-page request nil)))


(defn validate-password-change [old-password new-password confirm-new-password user]
  (-> nil
      (conj-when (nil? user) "Old password is incorrect.")
      (conj-when (string/blank? old-password)
                 "Old password can't be blank.")
      (conj-when (= old-password new-password)
                 "New password must be different from the old password.")
      (conj-when (string/blank? new-password)
                 "New password can't be blank.")
      (conj-when (not= new-password confirm-new-password)
                 "New password and new password confimation must match.")))

(defn change-user-password! [username password]
  (let [old-user (storage/get-user username)
        new-user (assoc old-user :password (creds/hash-bcrypt password))]
    (infof "Changing password. Old User: [%s] New User [%s]" old-user new-user)
    (storage/update-user! old-user new-user)))

(defn change-password! [old-password new-password confirm-new-password request]
  (let [username (get-in request [:session :cemerick.friend/identity :current])
        user (creds/bcrypt-credential-fn storage/get-user {:username username :password old-password})
        response (redirect "/dashboard")]
    (infof "bcrypt-credential-fn returned [%s]" user)
    (if-let [errors (validate-password-change old-password new-password confirm-new-password user)]
      (change-password-page request errors)
      (do
        (change-user-password! username new-password)
        (assoc-in response [:flash] {:success "Password successfully changed."})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Forgotten Password
(defn forgot-password-page
  ([request errors]
     (views/page
      {:title "Forgot Password"
       :request request
       :body-content
       [:div.container
        [:div.span6.offset3
         [:h3 "Forgot Password"]
         (views/error-list errors)
         [:form.well {:action "/forgot-password" :method "POST"}
          (form/label "Email Address" "Email Address")
          (form/email-field "email")
          [:br]
          (form/submit-button "Reset Password")]]]}))
  ([request]
     (forgot-password-page request nil)))

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
     :from "theteam@mastodonc.com"}}))

(defn forgot-password [email r]
  ;; if email exists
  ;; generate temporary pass key with expiry
  ;; send reset password link email
  (if-let [account (storage/get-user email)]
    (let [temp-key (str (java.util.UUID/randomUUID))
          response (redirect "/forgot-password")
          server-name (:server-name r)
          server-port (if (= 80 (:server-port r)) "" (format ":%s" (:server-port r)))]
      (storage/update-user! account (assoc account :temp-key temp-key))
      (send-email
       "theteam@mastodonc.com"
       email
       "Mastodon C - Password Reset"
       (format "Hi,

Someone has requested a password reset attempt using your email address at Mastodon C.
If this was you then great! Here is the link you need to click on in order to create a
new password:

https://%s%s/reset-password/%s

If not, then please contact us at theteam@mastodonc.com and we'll investigate this
incident.

Keeping it green,

The Mastodon C Team
" server-name server-port temp-key))
      (assoc-in response [:flash] {:success "Please check your email for the instructions on how to change your password."}))
    (assoc-in (redirect "/forgot-password") [:flash] {:error "That user does not exist."})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reset Password
(defn reset-password-page
  ([id request errors]
     (infof "ID: %s Request: %s" id request)
     (if-let [user (storage/get-user-from-temp-id id)]
       (do
         (infof "User: %s" user)
         (views/page
          {:title "Reset Password"
           :request request
           :body-content
           [:div.container
            [:div.span6.offset3
             [:h3 "Change Password"]
             (views/error-list errors)
             [:form.well {:action "/reset-password" :method "POST"}
              (form/label "User Name" "User Name")
              [:p (:email user)]
              (form/hidden-field "temp-id" id)
              (form/label "New Password" "New Password")
              (form/password-field "new-password")
              (form/label "Confirm New Password" "Confirm New Password")
              (form/password-field "confirm-new-password")
              [:br]
              (form/submit-button "Reset Password")]]]}))
       (forgot-password-page request ["Temporary password reset key has expired. Please enter your email address again."])))
  ([id request]
     (reset-password-page id request nil)))

(defn validate-password-reset [new-password confirm-new-password]
  (-> nil
      (conj-when (string/blank? new-password)
                 "New password can't be blank.")
      (conj-when (not= new-password confirm-new-password)
                 "New password and new password confimation must match.")))

(defn reset-password! [temp-id new-password confirm-new-password r]
  (if-let [user (storage/get-user-from-temp-id temp-id)]
    (do
      (infof "Resetting password for %s" user)
      (if-let [errors (validate-password-reset new-password confirm-new-password)]
        (reset-password-page temp-id r errors)
        (do
          (storage/update-user! user (dissoc user :temp-key))
          (change-user-password! (:email user) new-password)
          (-> (assoc-in (redirect "/dashboard") [:flash] {:success "Password successfully changed."})
              ;; And log the user in
              ;; REFACTOR
              (assoc-in [:session :cemerick.friend/identity]
                    {:current (:email user)
                     :authentications
                     {(:email user)
                      {:identity (:email user)
                       :email (:email user)
                       :roles ["user"]}}})))))
    (do
      (errorf "Attempted to reset password with expired temp-key")
      (assoc-in (redirect "/forgot-password") [:flash] {:error "Temporary password reset key no longer valid. Please request a new one."}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User Profile
(defn profile-page
  ([request errors]
     (let [email (get-in request [:session :cemerick.friend/identity :current])
           api-key (get-in request [:session :cemerick.friend/identity :authentications email :api-key])]
       (views/page
        {:title "Your Profile"
         :request request
         :body-content
         [:div.container
          [:div.span6.offset3
           [:h3 "Your Profile"]
           (views/error-list errors)
           [:form.well {:action "/user/profile" :method "POST"}
            (form/label "Email Address" "Email Address")
            [:p email]
            (form/label "API Key" "API Key")
            [:p api-key]
            ]]]})))
  ([request]
     (profile-page request nil)))

;; User Profiles and Application P
(defn change-user-profile! [request]
  nil)

(defn change-application-preferences! [request]
  nil)
