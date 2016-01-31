(ns mastodon.site.views
  (:use [clojure.tools.logging :only (infof errorf)])
  (:require [mastodon.core :as mcore]
            [mastodon.storage :as storage]
            [mastodon.email :as email]
            [mastodon.datacentres :as datacentres]
            [clojure.math.numeric-tower :as math]
            [clojure.data.json :as json]
            [ring.util.response :as response]
            [hiccup.core :as hc]
            [hiccup.form :as form]
            [hiccup.page :as page]
            [hiccup.element :as element]
            [clj-time.core :as d]
            [clj-time.format :as tf]))

(defn error-list [errors]
  (if errors
    [:div.alert.alert-error
     [:a.close {:data-dismiss "alert"} "x"]
     [:strong "Whoops! "] "Please fix the following problems."
     (element/unordered-list errors)]
    nil))

(defn get-current-user
  "Get the current user from the context-map."
  [context-map]
  (get-in context-map [:request :session :cemerick.friend/identity :current]))

(defn get-user-menu [context-map]
  (if-let [user (get-current-user context-map)]
    [:ul.nav.pull-right
     [:li.dropdown
      [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"}
       user [:b.caret]]
      [:ul.dropdown-menu
       [:li [:a {:href "/user/change-password"} "Change Password"]]
       [:li [:a {:href "/user/profile"} "Profile"]]
       [:li.divider]
       [:li [:a {:href "/logout"} "Logout"]]]
      ]
     ]
    [:ul.nav.pull-right
     [:li [:a {:href "/register"} "Register"]]
     [:li [:a {:href "/login"} "Login"]]]))

(defn display-flash-message [context-map]
  (if-let [msg (get-in context-map [:request :flash])]
    (let [msg-key (first (keys msg))
          msg-txt (msg-key msg)]
      (cond (= msg-key :success)
            [:div.container
             [:div.alert.alert-success
              [:a.close {:data-dismiss "alert"} "x"]
              msg-txt]]
            (= msg-key :error)
            [:div.container
             [:div.alert.alert-error
              [:a.close {:data-dismiss "alert"} "x"]
              msg-txt]]
            (= msg-key :info)
            [:div.container
             [:div.alert.alert-info
              [:a.close {:data-dismiss "alert"} "x"]
              msg-txt]]
            :else nil))
    nil))

(defn get-navigation-menu [context-map]
  ;; Navigation
  [:div.navbar.navbar-fixed-top
   [:div.navbar-inner
    [:div.container
     [:a.btn.btn-navbar {:data-toggle "collapse" :data-target ".nav-collapse"}
      [:span.icon-bar]
      [:span.icon-bar]
      [:span.icon-bar]]
     [:h1 [:a.brand {:href "/"} "Mastodon&nbsp;C " [:i.icon-cloud " "]]]
     [:ul.nav.pull-right
      (map (fn [[k v]] (if (= v (:title context-map))
                         [:li.active [:a {:href k} v]]
                         [:li [:a {:href k} v]])) [["/" "Home"]
                                                   ["/kixi" "Kixi"]
                                                   ["/insight" "Chaos to Insight"]
                                                   ["/team" "Team"]
                                                   ["/sustainability" "Sustainability"]
                                                   ["http://blog.mastodonc.com/" "Blog"]
                                                   ["/contact" "Contact"]])]]]])

(defn page
  ;; :title :tab :request :body-content
  ([context-map]
     ;;(infof "Context Map: %s" context-map)
     (infof "Current User: |%s|" (get-current-user context-map))
     (page/html5 {:lang "en"}
                [:head
                 [:meta {"http-equiv" "Content-Type" "content" "text/html; charset=UTF-8"}]
                 [:meta {"charset" "utf-8"}]
                 [:title (str (:title context-map) " - Mastodon C")]
                 [:meta {"name" "description" "content" "Mastodon C - greenify your Hadoop job."}]
                 [:meta {"name" "author" "content" "Mastodon C"}]

                 ;; stylesheets
                 ;;(page/include-css "https://netdna.bootstrapcdn.com/twitter-bootstrap/2.0.4/css/bootstrap.min.css")
                 (page/include-css "https://fonts.googleapis.com/css?family=Asap:700")
                 (page/include-css "/css/bootstrap.min.css")
                 (page/include-css "/css/responsive.css")
                 (page/include-css "/css/font-awesome.css")
                 (page/include-css "css/fonts.css")
                 (page/include-css "/css/theme.css")
                 (page/include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js" "https://netdna.bootstrapcdn.com/twitter-bootstrap/2.0.4/js/bootstrap-dropdown.js" "https://netdna.bootstrapcdn.com/twitter-bootstrap/2.0.4/js/bootstrap-alert.js")
                 (element/javascript-tag "$(\".dropdown-toggle\").dropdown()")
                 (element/javascript-tag "$(\".alert\").alert()")
                 (:head-content context-map)]
                [:body.theme
                 [:header#masthead
                  ;; Navigation
                  (get-navigation-menu context-map)]
                 [:div.pageContainer

                  ;; Flash Messages
                  (if context-map (display-flash-message context-map))

                  ;; All our lovely lovely content
                  [:div#content {:role "main"} (:body-content context-map)]

                  ;; footer
                  [:footer#footer.section {:role "contentinfo"}
                   [:div.container
                    ;; [:div.pull-right [:a.twitter-follow-button {:data-show-count "false" :data-align "right" :data-lang "en" :href "https://twitter.com/MastodonC"} "Follow @MastodonC"]]
                    ;; [:ul
                    ;;  [:li "&copy; Mastodon C Ltd 2012"]
                    ;;  ;;[:li [:a {:href "/terms"} "Terms and Conditions"]]
                    ;;  [:li [:a {:href "mailto:theteam@mastodonc.com"} "Contact"]]]
                    [:div.row-fluid
                     [:div.span4
                      [:h3 "Contact us"]
                      [:ul.icons
                       [:li [:i.icon-envelope] [:a {:href "mailto:theteam@mastodonc.com"} "theteam@mastodonc.com"]]
                       [:li [:i.icon-twitter] [:a {:href "http://twitter.com/MastodonC"} "@MastodonC"]]
                       [:li [:i.icon-legal] [:a {:href "/consultancytandcs"} "Terms and Conditions"]]]

                      ]
                     [:div.span4
                      [:h3 "3 Cleanest Clouds"]
                      [:ul.icons
                       [:li [:i.icon-leaf] [:a {:href "/dashboard"} "Iceland - GreenQloud"]]
                       [:li [:i.icon-leaf] [:a {:href "/dashboard"} "London - Rackspace"]]
                       [:li [:i.icon-leaf] [:a {:href "/dashboard"} "Oregon - AWS"]]]
                      ]
                     [:div.span4
                      [:h3 "Top Rated Posts"]
                      [:ul.icons
                       [:li [:i.icon-rss] [:a {:href "http://blog.mastodonc.com/2012/04/26/transcript-of-saving-the-world-with-hadoop-talk-at-london-hadoop-user-group/"} "Saving the world with hadoop"]]
                       [:li [:i.icon-rss] [:a {:href "http://blog.mastodonc.com/2012/08/20/hadoop-vs-data-warehouses/"} "Hadoop vs Data Warehouses"]]
                       [:li [:i.icon-rss] [:a {:href "http://blog.mastodonc.com/2012/08/05/hive-pain-reduction-tricks/"} "Hive pain reduction tricks"]]]
                      ]
                     ]

                    ]
                   ] ;; end of footer

                  ] ;; end of PageContainer
                 ])))

;; Basic Site

(def hero-unit
  [:div.hero-unit
   [:div.hero-logo ]])

(defn landing-page [request]
  (page {:title "Home"
         :request request
         :body-content
         [:div
          [:section#promo.section.alt
           [:div.container
            [:div.row-fluid
             [:div.container.span7.hero-unit
              [:h1 "Big Data Done Better"]
              [:p "Your business could be even more successful, if only you could unlock the power of all the data that you already have. But it's hard to even know where to start &mdash; the data is big, it's messy, and the analytical and technology options can be bewildering."]
              [:p "Mastodon C are agile big data specialists. We offer the open source technology platform and the skills to help you realise that potential, and we do it all on zero carbon infrastructure. "]
              ;; [:ul.icons
              ;;  [:li
              ;;   [:i.icon-ok] "Better Platform"]
              ;;  [:li
              ;;   [:i.icon-ok] "Better Data Science"]
              ;;  [:li
              ;;   [:i.icon-ok] "More Sustainable"]]
              [:a.btn.btn-success.btn-large.pull-right {:href "/contact"} [:h2 "Contact Us " [:i.icon-circle-arrow-right]]]]
             [:div.container.span5
              [:img.pull-right {:src "img/imac-screenshot.png"}]]]]]

          [:section.section
           [:div.container
            [:div.page-header [:h1 "What we do"]]
            [:div.row-fluid
             [:ul.thumbnails.bordered.pull-center
              [:li.span4
               [:h2
                [:a {:href "/insight"} [:i.icon-magic.huge-icon] [:span.blocked "Chaos to Insight Service"]]]
               [:p "Want to turn your data into profit? Mastodon C can get you started with data science, machine learning and advanced statistics to improve your business."]
               [:a.btn.btn-success. {:href "/insight"} [:h2 "Make me Money" [:i.icon-circle-arrow-right]]]
               ]
              [:li.span4
               [:h2
                [:a {:href "/kixi"} [:i.icon-cogs.huge-icon] [:span.blocked "Kixi - Big Data Platform"]]]
               [:p "Want to exploit the power of Hadoop? Mastodon C will design and manage your big data factory in the cloud, keep it healthy and happy, and make it green."]
               [:a.btn.btn-success. {:href "/kixi"} [:h2 "Get the Insight" [:i.icon-circle-arrow-right]]]
               ]
              [:li.span4
               [:h2
                [:a {:href "/sustainability"} [:i.icon-leaf.huge-icon] [:span.blocked "Sustainability"]]]
               [:p "Mastodon C has calculated the carbon emissions of public cloud facilities. We give you the confidence that your data crunching won't destroy the planet."]
               [:a.btn.btn-success. {:href "/sustainability"} [:h2 "Save the World " [:i.icon-circle-arrow-right]]]
               ]]
             ;; right
             ]]]] ;; end container.last
         } ;; end context-map
        ))

(defn home-page [request]
  (landing-page request))

(defn about-page [request]
  (page {:title "About"
         :request request
         :body-content
         [:div.container
          hero-unit
          [:div.row
           [:div.span12

            [:p "Mastodon C is a London based startup that makes big
            data and Hadoop in the cloud greener and better, by enabling
            companies to minimise their carbon emissions, while also
            running their big data operations at the highest levels of
            performance, cost effectiveness, and business continuity."]

            [:p "We offer"
             [:ul

              [:li "A managed Hadoop service, to ensure the best cost, carbon footprint, and business continuity possible."]
              [:li "Data science services to help you get deeper insights into your business."]
              [:li "Free information on the carbon emissions and costs of different public cloud options."]]]

            [:p "If you'd like us to run your Hadoop cluster so you can crunch big data without harming the planet, or if you would like to transform your data into business advantage, then " [:a {:href "mailto:theteam@mastodonc.com"} "contact us"] "."]

            [:p "If you like the sound of Mastodon C and want to know more about how to make your cloud computing green, read the " (element/link-to "/faq" "FAQ") ", subscribe to our " (element/link-to "http://mastodonc.wordpress.com" "blog") ", or follow us on " (element/link-to "http://twitter.com/mastodonc" "Twitter") "."]]]]}))

(defn kixi-page [request]
  (page {:title "Kixi"
         :request request
         :body-content
         [:div
          [:section#promo.section.alt
           [:div.container
            [:div.row-fluid
             [:div.span7
              [:div.hero-unit
               [:h1 "Kixi"]
               [:p "Big data is a huge opportunity, and the open source technologies involved are amazingly powerful. But architecting, assembling, and knowing how to use those tools well can be difficult. That's where Kixi comes in."]
               [:p "Kixi is our self-managing, auto-scaling big data cloud platform, which brings together a set of open-source technologies such as Hadoop to build the customised big data factories and pipelines that wrangle our clients' raw data into valuable insights."]
               [:ul.icons
                [:li [:i.icon-ok] "Lives on commodity cloud infrastructure for simple scaling and repair"]
                [:li [:i.icon-ok] "Feeds into existing data flows and visualisation systems (output to CSV, MySQL, ODBC, etc)"]
                [:li [:i.icon-ok] "Personal support and customisation for your business and your unique data flows"]
                [:li [:i.icon-ok] "Managed service includes monitoring, on-call support, data quality checks and alerts, and infrastructure cost management"]
                [:li [:i.icon-ok] "Zero carbon"]
                [:li [:i.icon-ok] "Fully open-source"]]
               [:a.btn.btn-success.btn-large.pull-right {:href "/contact"} [:h2 "Contact Us " [:i.icon-circle-arrow-right]]]
               ]]
             [:div.container.span5
              [:img.pull-right {:src "img/mastodon-sq.png"}]]
             ]]]
          ]}))

(defn insight-page [request]
  (page {:title "Chaos to Insight"
         :request request
         :body-content
         [:div
          [:section#promo.section.alt
           [:div.container
            [:div.row-fluid
             [:div.span7
              [:div.hero-unit
               [:h1 "Chaos to Insight"]
               [:p "Does your data love your business as much as you do?"]
               [:p "Your business already produces massive amounts of data. Our experts can gather, scrub, model and analyse that data for you. When we're done you'll know what insight has been hiding in your business."]
               [:p "Take a look at our quick start package."]
               [:a.btn.btn-success.btn-large.pull-right {:href "/quickstart"} [:h2 "Quick Start " [:i.icon-circle-arrow-right]]]
               ]]
             [:div.container.span5
              [:img.pull-right {:src "img/imac-screenshot.png"}]]
             ]]]
          ]}))

(defn contact-us [from name message r]
  (infof "From: %s Name: %s \nMessage: %s" from name  message)
  (email/send-email "theteam@mastodonc.com" "bruce@mastodonc.com" "CONTACT! Via the Mastodon C website" (format "Contact Us Form: %s wrote from %s with this message %s" name from message))
  (assoc-in (response/redirect "/contact") [:flash]  {:email_sent "Y"}))

(defn contact-page [request]
  (infof "Contact Page - Request: %s" request)
  (page {:title "Contact"
         :request request
         :body-content
         [:div
          [:section.section
           [:div.container
            [:div.page-header
             [:h1 "Talk to us"]]
            [:div.row-fluid
             [:div.span6
              [:p "Come on and talk to us. You know you want to."]
              [:ul.icons
               [:p [:li [:i.icon-home] "c/o Open Data Institute, 3rd Floor, 65 Clifton Street, London, EC2A 4JE"]]
               [:p [:li [:i.icon-envelope] [:a {:href "mailto:theteam@mastodonc.com"} "theteam@mastodonc.com"]]]
               [:p [:li [:i.icon-twitter] [:a {:href "http://twitter.com/MastodonC"} "@MastodonC"]]]
               [:p [:li [:i.icon-globe] [:a {:href "http://www.mastodonc.com"} "www.mastodonc.com"]]]
               ]]
             [:div.span6
              (when (= "Y" (get-in request [:flash :email_sent]))
                [:div.alert.alert-success
                 [:a.close {:data-dismiss "alert"} "x"]
                 [:strong "Thanks! "] "We'll be in touch soon!"])
              [:form.form-horizontal {:action "/contact" :method "POST"}
               [:fieldset
                [:div.control-group
                 [:label.control-label {:for "name"} "Your Name"]
                 [:div.controls [:input.input-xlarge {:type "text" :id "name" :name "name"}]]]
                [:div.control-group
                 [:label.control-label {:for "from"} "Your Email"]
                 [:div.controls [:input.input-xlarge {:type "email" :id "email" :name "email"}]]]
                [:div.control-group
                 [:label.control-label {:for "message"} "Your Message"]
                 [:div.controls [:textarea.input-xlarge {:id "message" :rows "3" :name "message"}]]]
                [:div.control-group
                 [:div.controls
                  [:button.btn.btn-success {:type "submit"} "Thanks!" [:i.icon-right-circle-arrow]]]]]]]]]]
          [:section#promo.alt.section
           [:div.map-top-shadow]
           [:div#map]
           [:div.map-bottom-shadow]]

          ;; javascript for the map
          (page/include-js "https://maps.googleapis.com/maps/api/js?sensor=false")
          (page/include-js "js/script.js")
          (element/javascript-tag "mapInit();")
          ]}))

(defn sustainability-page [request]
  (page {:title "Sustainability"
         :request request
         :body-content
         [:div
          [:section#promo.section.alt
           [:div.container
            [:div.row-fluid
             [:div.span7
              [:div.hero-unit
               [:h1 "Sustainability"]
               [:p "At Mastodon C we think that we can crunch big data and build a bright green future at the same time."]
               [:p "We've done the " [:a {:href "/dashboard"} "research"] " to find the zero carbon data centres and that is where we do all of our data processing. We're more than happy to talk to you about it."]
               [:a.btn.btn-success.btn-large.pull-right {:href "/contact"} [:h2 "Contact Us " [:i.icon-circle-arrow-right]]]
               ]]
             [:div.container.span5
              [:img.pull-right {:src "img/imac-screenshot.png"}]]
             ]]]
          ]}))

(defn projects-page [request]
  (page {:title "Projects"
         :request request
         :body-content
         [:h1 "Hello!"]}))

(defn team-page [request]
  (page {:title "Team"
         :request request
         :body-content
         [:div
          [:section#promo.section.alt
           [:div.container
            [:div.row-fluid
             [:div
              [:div.hero-unit
               [:h1 "Saving the world with Big Data"]]]]]]
          [:section.section
           [:div.container
            [:div.page-header [:h1 "Meet the team"]]
            [:div.container.offset2
             [:ul.thumbnails.bordered.thumbnail-list
              [:li.span4
               [:figure.thumbnail-figure
                [:img {:src "img/fran-bw-4x6.png"}]
                [:figcaption.thumbnail-title
                 [:h3 [:span "Fran Bennett"]]
                 [:p [:span "CEO"]]]]
               [:p "Francine is the CEO and cofounder of Mastodon C. She spent a number of years before that working for search engines, helping them to turn data into money."]
               [:p "She likes coffee, running, sleeping a lot, and large datasets. "]
               ]
              [:li.span4
               [:figure.thumbnail-figure
                [:img {:src "img/bruce-bw-4x6.png"}]
                [:figcaption.thumbnail-title
                 [:h3 [:span "Bruce Durling"]]
                 [:p [:span "CTO"]]]]
               [:p "Bruce is the CTO and cofounder of Mastodon C. He is also one of the co-founders of the London Clojurians and London Python Dojo."]
               [:p "He loves automating drudgery away with a script, learning a new language in GNU emacs and generally talking nonsense."]
               ]
              ]]]]
          ]}))

(defn faq-page [request]
  (page {:title "FAQ"
         :request request
         :body-content
         [:div.container
          [:div.row
           [:h2 "Frequently Asked Questions"]

           [:p [:a {:href "#hadoop"} "Hadoop"]]
           [:p [:a {:href "#carbon-footprints"} "Carboon footprints"]]
           [:p [:a {:href "#about-us"} "About us"]]

           [:h3 [:a {:name "hadoop"} ] "Hadoop"]
           [:dl
            [:dt "What is Hadoop?"]
            [:dd
             [:p "Apache Hadoop is an open source software framework, based on the MapReduce. Hadoop and its ecosystem make it easy and cheap to process large volumes of data using 'clusters' of commodity servers. This means that Hadoop in the cloud is a great way to do Big Data processing and data science type applications. The MapReduce approach that Hadoop is based on was derived from technology invented by Google, was extensively developed by Yahoo!, and is already in use at many "[:a {:href "http://wiki.apache.org/hadoop/PoweredBy/"} " successful organisations"] "."]]

            [:dt "Are you only about Hadoop?"]
            [:dd
             [:p "We don't only do Hadoop - we also use other complementary technologies, and the carbon footprinting that we do applies to any cloud infrastructure. However, all of our business is based around green and big data, which means Hadoop plays a big part in our lives. "]]]

           [:h3 [:a {:name "carbon-footprints"} ] "Carbon footprints"]
           [:dl
            [:dt "Is cloud computing actually a big part of global emissions?"]
            [:dd
             [:p "Yes, it is. By 2020, IT is projected to generate 3% of the world's emissions, and if cloud computing were a country, it would have the 5th largest emissions in the world. Since there is such a wide range of power sources for existing cloud facilities (right through from coal to geothermal), we think that it's possible to save at least 1/3 of these global emissions. That's why our corporate mission is to reduce global greenhouse gas emissions by 1%."]]

            [:dt "Why should I bother thinking about green issues? (I'm busy | my boss won't like it | it's not my decision)"]
            [:dd
             [:p "The global situation in relation to carbon emissions is extremely bad right now - there is a very short time left to us to make changes which give a chance of avoiding catastrophic global warming. For example, the International Energy Agency has calculated that drastic changes in energy use are needed by 2017 in order to avoid serious global consequences. As private individuals, it is hard for us to make an impact on this. However, companies and developers using cloud resources do have a big opportunity to make changes: a UK individual's personal footprint equates to 3 typical servers (about 11 tonnes/year), so a company which makes greener choices in this area can make much more of a difference, and since clouds are now so commoditised, it's easy to move to greener locations without much cost or hassle. That's why we're committed to providing data and tools to enable individuals and companies to make the greener choice."]]

            [:dt "How do you make money from the cloud carbon footprinting data?"]
            [:dd
             [:p "We don't make money directly from publishing this data. We create and publish the data because we believe it's a very important issue which nobody else is tackling. We want you to reuse it for free, and even better we'd love you to contribute data back into the effort or to help open source it. We also use this data for our clients to make sure that their cloud computing is optimised for low carbon as well as for low cost and high performance. "]]

            [:dt "How do you calculate the emissions?"]
            [:dd
             [:p "We use publicly available data about the local electricity grid mix where the facility is located, combined with research data on the power consumption of different types of servers at different temperatures and live weather data, to model expected carbon emissions per server hour. Unfortunately, big providers are currently very secretive about their power consumption and their exact emissions; many of them run mainly on coal, so sharing emissions data would make them look pretty terrible. That's why we have to put together multiple public data sources to figure out the numbers. That's also why we want to keep up the pressure on them to be more transparent to users about the different emissions profiles of different locations. "]]

            [:dt "What about green web hosting?"]
            [:dd
             [:p "Web hosting is trickier, as in that case latency becomes a very high priority so it's difficult to move work to greener locations. However, we're currently putting together data and solutions for people who want to do low-latency green web hosting. " [:a {:href "mailto:theteam@mastodonc.com"} "Contact us"] " if you'd like to be kept updated on it."]]]

           [:h3 [:a {:name "about-us"} ] "About us"]
           [:dl
            [:dt "Why are you called Mastodon C?"]
            [:dd
             [:p "Depends who you ask. Maybe we are Management And Sustainability Through Ordering Distribution Of Networked Computing or maybe because Hadoop's logo is an elephant, mastodons are like elephants and are extinct, and we want to avoid making humans extinct. Or maybe because we like the name Mastodon but a heavy metal band from Georgia already took that one."]]]

            ]]
         }))

(defn conditions-page [request]
  (page {:title "Terms and Conditions"
         :request request
         :body-content
         [:div.container
          [:div.row
           [:h2 "Terms and Conditions"]
           [:p "This Agreement establishes the terms and conditions under which Mastodon C Limited (\"Mastodon C\"), through its employees and/or its sub-contractors, (\"Consultants\") provides to its customer (\"Client\") consulting and professional services as described in the quotations  in Exhibit A to this Agreement."]
           [:p [:strong "1. Services to be performed/personnel: "]  "Mastodon C will provide to Client, Consultants to perform Services at Client's direction as agreed to in writing between Mastodon C and Client. The Consultants will be suitably qualified, experienced and proficient in the relevant products and skills. If at any time Client informs Mastodon C that it is reasonably dissatisfied with the performance of any individual working on the Services,  Mastodon C will take steps to remedy the dissatisfaction, which may at Mastodon C’s option include replacement of the individual by an alternative individual. If reasonably requested by either party, Mastodon C and Client will each appoint a named representative to act as a liaison point between the parties and to meet at agreed intervals to review progress of the Services and discuss any issues or concerns."]

           [:p [:strong "2. Timescales and Change Requests: "] "Mastodon C will use all reasonable endeavours to meet any agreed dates for the performance of Services and shall promptly advise Client of any potential or actual delays. Any changes in the Services to be provided or the agreed performance dates will become effective only upon written agreement of the parties. Client will provide to Mastodon C in a timely manner all assistance and information and materials which Mastodon C may reasonably request for the performance of Services, and Mastodon C will not be liable for delays in performance caused by any delay or failure to provide same to Mastodon C."]

           [:p "If Mastodon C provides Services for a customer of Client, Client shall (i) make no guarantees, warranties or representations in excess of those contained in this Agreement in relation to the Services ; (ii) indemnify Mastodon C against any claims relating to any guarantees, warranties or representations so made by Client; and (iii) procure that Client's customer performs such obligations hereunder are as relevant to enable the Project to be completed."]

           [:p [:strong "3. Acceptance/Charges and Payment: "] "Services will be deemed to be accepted to Client's satisfaction upon delivery of the relevant deliverables by Mastodon C. Charges for the Services will be as described in the relevant quotation and unless stated otherwise in the quotation will be on a time and materials basis.  Unless otherwise agreed between the parties in writing, invoices for time, materials and expenses will be raised monthly in arrears or at the completion of the Services if completed in less than one month. Payments shall be due thirty (30) days from date of invoice. Mastodon C reserves the right to levy interest on overdue payments at the maximum amount due by law from the date payment becomes due until the date it is received by Mastodon C. Mastodon C reserves the right to terminate or suspend Services if Client is overdue with payments at any time."]

           [:p [:strong "4. Confidential Information: "] "During the period of this Agreement and at all times thereafter, each party shall treat as confidential and not reproduce or disclose to any other party all information, including but not limited to, software programs whether in source or object code format, technical data, correspondence, the details of this Agreement or any services or quotation, and other material which is stated to be the confidential and/or trade secret information of the other party, or which may be reasonably presumed to be so. Each party shall safeguard such information to the same extent that it safeguards its own confidential and proprietary information and in any event with not less than a reasonable degree of protection. Notwithstanding the foregoing, Mastodon C shall be entitled to provide to third parties only such information as is necessary for it to perform its obligations in relation to the Services, or as may be required by law. The obligation of the parties not to disclose information shall not apply to information which was already in the public domain, or in the rightful possession of the other party, at the time of its disclosure, or which is disclosed as a matter of right by a third party or which passes into the  public domain  by  acts other than the unauthorised  acts  of the other party. Within ten (10) days of the completion of the relevant Services, each party shall return all originals and any copies thereof of any confidential information of the other party. It is understood and agreed that in the event of a breach of this paragraph money or damages may not be an adequate remedy, and therefore, in addition to any other legal or equitable remedies, either party shall be entitled to seek injunctive relief to prevent an anticipated breach of confidentiality."]

           [:p [:strong "5. Proprietary Rights: "] "Unless otherwise agreed in writing between duly authorised representatives of the parties copyright, patents and any and all industrial and intellectual property rights in any and all computer programs, documentation, reports and all other information developed, written, provided or produced pursuant to the Services do vest or shall vest solely in Mastodon C or its supplier immediately and unconditionally upon being developed, produced or written."]

           [:p [:strong "6. Warranty/Limitation of Liability: "] "Mastodon C shall provide the Services in a professional manner with due care, skill and competence at a level commensurate with industry standards. No  warranty or guarantee is given that Services  will  be  successful  in  whole  or in  part. Mastodon C  shall not  be liable for any indirect, consequential, special or incidental loss or damage  suffered  by Client  or any  third party,  including loss  of  property, of data or of profits  even  if  Mastodon C  has  been  advised  of  the  possibility of such  damage arising directly  or  indirectly  from  the  provision of Services. Mastodon C's  liability to Client or any third  party, for a claim of any kind arising as a  result  of  or related  to any  product  or  Service, whether  in contract, in tort (including negligence or strict liability) or otherwise, under any  warranty, condition or guarantee or otherwise,  shall  be limited to monetary damages  and the aggregate  amount  thereof for all claims  relating  to any particular Project or product provided shall in  any event be limited to a sum equivalent to the aggregate amount paid to Mastodon C under the relevant Project or for the relevant product which gave rise to the claim.  No  action,  regardless of form, may be brought by Client more than one (1) year  after  the  events which gave rise to the  cause  of  the action."]

           [:p [:strong "7. General Provisions: " ] "(i) Mastodon C  shall  not  be  liable  for  failure or delay in performance of its obligations under this Agreement if such failure or delay is due to causes beyond its reasonable control, including but not limited to Acts of God, war, terrorist action, riot, strike, lock-outs, trade disputes, third party delay, accident, fire, flood, storm, natural disaster, shortages, power or environmental failures. (ii) Mastodon C shall be entitled to subcontract any or all of the Services to suitably qualified personnel or organisations. In this event the rights and obligations of Mastodon C hereunder shall not be diminished. (iii) Any notices or other communication required to be given under this Agreement shall be given in writing and sent by recorded delivery mail or facsimile transmission confirmed by hard copy letter to the address of the relevant party as given in the quotation and shall be deemed received forty-eight (48) hours after dispatch. (iv) The  waiver or failure of either party to exercise in any respect any right or remedy pursuant to this Agreement shall not be deemed a waiver of any further rights or remedies. (v) The  relationship  between  Mastodon C  and Client  is  that of independent contractors and nothing in this Agreement shall be construed (a) to give either party the power to direct or  control the activities  of the other party; (b) to  constitute  the parties as  employer  and  employee, principal and agent, partners, joint venturers, co-owners or otherwise participants in any joint undertaking; or (b) to allow either party to create or  assume  any  obligations on behalf of the  other  party  for any purpose. (vi)  During the term of any Services and for a period of two (2) years thereafter  Client shall not, without Mastodon C's prior written consent, offer employment to or enter into any contract for services with any Mastodon C employee or subcontractor who has provided Services. (vii) If  any  provision of this Agreement shall be found by  any court or administrative body of competent jurisdiction to be invalid or unenforceable  the invalidity or unenforceability of such provision shall not affect the other provisions of this Agreement and all provisions not affected by such invalidity or unenforceability shall remain in full force and effect.  (viii) This Agreement and the relevant quotation constitutes the entire agreement between the parties and supersedes  all  previous negotiations and agreements, written or oral, express or implied between the parties with respect to the Services. No amendment to this Agreement shall be effective unless specifically stated to amend this Agreement and executed by authorised representatives of both parties. (ix) In the event of any conflict between the provisions of this Agreement and any purchase order or other document issued by Client the provisions of this Agreement shall prevail. (x) This Agreement shall be governed and construed in accordance with the laws of England and Wales, and  subject to the jurisdiction of the English courts. If any provision of this Agreement is found to be invalid, illegal or unenforceable it shall be considered severable and the remaining provisions shall not be impaired. Any such provision shall be interpreted to the extent possible so as to give effect to its intended purpose. (xi) Upon termination of any Services, (a) Client shall pay Mastodon C for all work performed up to the date of termination by Mastodon C and Mastodon C shall provide to Client any materials for which Client has so paid; (b) each party shall return to the other all materials and property including proprietary data which has been provided to it for the purposes of this Agreement and /or the relevant Services; (c) paragraphs 4, 5 and 6 of this Agreement shall survive."]

            ]]
         }))

(defn quickstart [from name message r]
  (infof "From: %s Name: %s \nMessage: %s" from name message)
  (email/send-email "theteam@mastodonc.com" "bruce@mastodonc.com" "QUICK START! Via the Mastodon C website" (format "Contact Us Form: %s wrote from %s with this message %s" name from message))
  (assoc-in (response/redirect "/contact") [:flash]  {:email_sent "Y"}))

(defn quickstart-page [request]
  (page {:title "Big Data Quick Start"
         :request request
         :body-content
         [:div.container
          [:div.page-header [:h1 "Making sense of Big Data " [:small "our quick-start package"]]]
          [:div.row-fluid
           [:div.span6
            [:p "Organisations everywhere are re-evaluating their use of data. New technologies and smarter analysis techniques enable the fastest to flourish."]
            [:p "We aim to help all our clients to become those fastest movers. In order to do so effectively and quickly, they need a clear understanding of their current situation and needs, and a clear plan identifying what technologies and approaches would benefit them."]
            [:h2 "Why this package?"]
            [:p "Traditional approaches to data create two particular problems which hurt the CTO, CIO, and CEO:"]
            [:ul
             [:li [:strong "Technology Cost: "] "as data gets bigger and more complex, traditional databases need bigger and bigger hardware and start to run more and more slowly. This results in very high costs, or in throwing away valuable data."]
             [:li [:strong "Analytical Limitations: "] "much of the business value of data lies in prediction, forecasting, and classification. These depend on combining different data sets, mining unstructured sources like text, and advanced analytical techniques. Traditional tools generally can't enable these advanced analytics."]]
            [:p "Cost effective and powerful open source technology and analytical tools now exist which solve these issues. However, the landscape is complicated and it can be hard to know where to start."]
            [:p "We are experienced big data and analytical specialists, who understand this landscape in depth. We help you to evaluate how use of \"big data\" technology and tools could help your business, and to create a practical plan to get there."]
            [:h2 "What is it?"]
            [:p "A 5 day quick start consultancy and support package to evaluate how to make best use of data in your own business. We collaborate with your staff to identify your business needs; assess current capability and available data; do a gap analysis to identify what would be needed to realise your priority needs; and deliver a final report documenting everything and recommending next steps for the business."]
            [:p "This package leaves you with a clear and realistic vision for your business, and concrete actions which will realise that vision."]
            [:h2 "Who we are"]
            [:p "We are specialists at dealing with very large datasets and extracting real, usable insight."]
            [:p "Our team has previous experience at companies including Google, Deutsche Bank, and Equifax. We can bring the rare combination of technology know-how with deep analytical abilities."]
            [:p "We supply managed Hadoop and data science solutions which make the most of your business' potential."]]
           [:div.span6
              (when (= "Y" (get-in request [:flash :email_sent]))
                [:div.alert.alert-success
                 [:a.close {:data-dismiss "alert"} "x"]
                 [:strong "Thanks! "] "We'll be in touch soon!"])
              [:form.form-horizontal {:action "/quickstart" :method "POST"}
               [:fieldset
                [:div.control-group
                 [:label.control-label {:for "name"} "Your Name"]
                 [:div.controls [:input.input-xlarge {:type "text" :id "name" :name "name"}]]]
                [:div.control-group
                 [:label.control-label {:for "from"} "Your Email"]
                 [:div.controls [:input.input-xlarge {:type "email" :id "email" :name "email"}]]]
                [:div.control-group
                 [:label.control-label {:for "message"} "Your Message"]
                 [:div.controls [:textarea.input-xlarge {:id "message" :rows "3" :name "message"}]]]
                [:div.control-group
                 [:div.controls
                  [:button.btn.btn-success {:type "submit"} "Let's Get Started!" [:i.icon-right-circle-arrow]]]]]]]
            ]]
         }))

(defn not-found-404-page [request]
  (page {:title "Page Not Found"
         :request request
         :body-content
         [:div
          [:section#promo.section.alt
           [:div.container
            [:div.row-fluid
             [:div.span7
              [:div.hero-unit
               [:h1 "Not Found!"]
               [:p "Kixi couldn't find what you were looking for."]
               [:p "Try starting over from our "  (element/link-to "/" "home page.")]
               ]]
             [:div.container.span5
              [:img.pull-right {:src "img/mastodon-sq.png"}]]
             ]]]
          [:section.section
           [:div.container
            [:div.page-header [:h1 "Are you looking for any of these?"]]
            [:div.row-fluid
             [:ul.thumbnails.bordered.pull-center
              [:li.span4
               [:h2
                [:a {:href "/kixi"} [:i.icon-cogs.huge-icon] [:span.blocked "Kixi - Big Data Platform"]]]
               [:p "Is your Hadoop cluster becoming a hassle? Mastodon C can manage your big data cluster in the cloud, keep healthy and happy, and make it green."]
               ]
              [:li.span4
               [:h2
                [:a {:href "/insight"} [:i.icon-magic.huge-icon] [:span.blocked "Chaos to Insight"]]]
               [:p "Want to turn your data into profit? Mastodon C can get you started with data science, machine learning and advanced statistics to improve your business."]
               ]
              [:li.span4
               [:h2
                [:a {:href "/sustainability"} [:i.icon-leaf.huge-icon] [:span.blocked "Sustainability"]]]
               [:p "Mastodon C has calculated the carbon emissions of public cloud facilities. Mastodon C give you the confidence that your data crunching won't destroy the planet."]
               ]]
             ;; right
             ]]]
          [:section.section.alt
           [:div.container
            [:div.page-header [:h1 "Why are you called Mastodon C?"]]
            [:blockquote.blockquote-fancy
             [:p.pull-center "Depends who you ask. Maybe we are Management And Sustainability Through Ordering Distribution Of Networked Computing or maybe because Hadoop's logo is an elephant, mastodons are like elephants and are extinct, and we want to avoid making humans extinct. Or maybe because we like the name Mastodon but a heavy metal band from Georgia already took that one."]]]]
          ]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Lead Form
(defn register-interest [request]
  (let [timestamp {:timestamp (tf/unparse (tf/formatters :rfc822) (d/now))}
        referer {:referer (get-in request [:headers "referer"])}
        user-ip {:remote-addr (:remote-addr request)}
        signup-info (merge (:params request) timestamp referer user-ip)]
    (infof "Registering a new user [%s]." (:params request))
    (infof "Sign Up Info: %s" signup-info )
    (infof "Request for register interest [%s]" request)
    (storage/add-lead! signup-info))
  (response/redirect "/thanks"))

(defn create-lead-form
  ([request call-to-action]
     [:div
      [:h3 call-to-action]
      [:form.well {:action "/register-interest" :method "POST"}
       (form/label "Your Name" "Your Name")
       (form/text-field "personname" "me")
       (form/label "Email Address" "Email Address")
       (form/email-field "email" "Your email address")
       [:br]
       [:input.btn.btn-success {:type "submit" :value "Tell me more"}]]])
  ([request]
     (create-lead-form request "I'm Interested")))

(defn create-lead-page [request]
  (page {:title "I'm Interested"
         :request request
         :body-content
         [:div.container
          [:div.span6.offset3
           (create-lead-form request)]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Application
(defn console-link [site provider]
  (cond (= provider :greenqloud)
        (element/link-to "https://manage.greenqloud.com/" (datacentres/site-description site provider))
        (= provider :rackspace)
        (element/link-to "https://manage.rackspacecloud.com/Home.do" (datacentres/site-description site provider))
        (= provider :aws)
        (element/link-to (str "https://console.aws.amazon.com/elasticmapreduce/home?region=" (datacentres/region site provider)) (datacentres/site-description site provider))
        (= provider :azure)
        (element/link-to (str "https://windows.azure.com/default.aspx" (datacentres/region site provider)) (datacentres/site-description site provider))))

;; TODO:
;; - Get Carbon Cost from User Prefs
;; - Use :cost rather than :kgCO2e for logged in users
;; - Filter out locations according to user prefs
;; - Get machine pref from User Prefs

;; TODO: Switch between :cost and kgCO2e depending on user login
(defn get-ranking-val [dc]
  (let [kgCO2e (:kgCO2e-per-hour dc)]
    (if (> kgCO2e 1)
      10
      (* 10 (math/expt (- 1 kgCO2e) 2)))))

;; TODO: Switch between :cost and kgCO2e depending on user login
(defn get-ranking-desc [dc]
  (format "%.3fkg" (:kgCO2e-per-hour dc)))

(defn datacentre-ranking-json [request]
  (infof "Request: [%s]" request)
  (json/json-str
   {:cols
    [{:id "Location" :label "Location" :type "string"}
     {:id "MickeyGs" :label "CO2/server hr" :type "number"}]
    :rows (for [dc (:rankings (storage/get-last-ranking))]
            (do (infof "Formatting %s" dc)
                {:c [{:v (datacentres/formatted-location (keyword (:site dc)) (keyword (:provider dc))) :f (datacentres/site-description (keyword (:site dc)) (keyword (:provider dc)))}
                     {:v (get-ranking-val dc) :f (get-ranking-desc dc)}]}))}))

(defn dashboard-page [request]
  (page
    {:title "Live Ratings"
         :request request
         :body-content
         [:div.row-fluid
          (page/include-js "https://www.google.com/jsapi")
          (page/include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
          (element/javascript-tag
           "google.load('visualization', '1', {'packages': ['geochart']});
      google.setOnLoadCallback(drawMarkersMap);

    function drawMarkersMap() {

      var jsonData = $.ajax({
        url: '/dc_rankings',
        dataType:'json',
        async: false
        }).responseText;

      // Create our data table out of JSON data loaded from server.
      var data = new google.visualization.DataTable(jsonData);

      var options = {
        displayMode: 'markers',
        colorAxis: {colors: ['red', 'green'], minValue: 0, maxValue: 10},
        sizeAxis: {minSize: 5,  maxSize: 25},
        backgroundColor: 'transparent',
        datalessRegionColor : 'lightgray',
        markerOpacity: 0.4
      };

    var chart = new google.visualization.GeoChart(document.getElementById('chart_div'));
    chart.draw(data, options);
  };")
          [:div.container-fluid
           [:div.row-fluid
            [:div.span4
             [:h3 "Live Carbon Ranking"]
             [:table.table.table-striped.table-condensed
             [:thead
              [:tr [:th "Location"] [:th "Cost"] [:th "CO" [:sub "2"] "e/hour"]]]
             [:tbody
              (for [dc (:rankings (storage/get-last-ranking))]
                [:tr [:td (console-link (keyword (:site dc)) (keyword (:provider dc)))] [:td {:align "right"} (format "$%.2f" (:cost-per-hour dc))] [:td {:align "right"} (format "%.3f kg" (:kgCO2e-per-hour dc))]])]]
             [:p "Cloud computing uses almost 2x as much power as the whole of the UK. These ratings show you the greenest place to send work right now."]
             [:p "These are the latest modelled CO" [:sub "2"] " emissions per hour for a m1.xlarge or equivalent for popular cloud locations. Our data and analysis are based on assumptions and represent opinions not facts. We aim to update our models as better data becomes available."]
             [:p "Use " (element/link-to "/application/preferences" "Customise") " to receive a personalised recommendation based on your preferred tradeoff between cost and emissions. Click on a location name to go directly to that provider."]
             [:p [:a {:href "mailto:theteam@mastodonc.com"} "Contact"] " us to request ratings for new locations."]
             [:p "** Rackspace purchase renewable energy certificates for the power used by their London data centre. This means that they use normal grid power, but that they have purchased the right to the renewable energy in that grid."]]
            [:div.span8 [:div#chart_div]]]]]}))

(defn preferences-page [request]
  (page {:title "Customise"
         :request request
         :body-content
         [:div.container
          [:ul.nav.nav-tabs
           [:li (element/link-to "/dashboard" "Live Ratings")]
           [:li.active (element/link-to "/application/preferences" "Customise")]]
          [:div.row
           [:div.span6.offset3
            (create-lead-form request "Tell me when this feature is available.")]]]}))

(defn thanks-page [request]
  (page {:title "Thanks"
         :request request
         :body-content
         [:div.container
          hero-unit
          [:div.row
           [:div.span12
            [:p "Thanks for telling us about your interest. In the meantime you can get live carbon ratings for popular cloud datacentres " (element/link-to "/dashboard" "here") "."]
            ]]]}))
