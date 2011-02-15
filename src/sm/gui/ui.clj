(ns sm.gui.ui
  (:use  [clojure.contrib [string :only [join split capitalize]]])
  (:require [gnome.ldtp :as ldtp])
  (:import java.util.NoSuchElementException
	   [gnome.ldtp Element Tab Window TabGroup]))



(defn same-name "takes a collection of keywords like :registration-settings
and returns a mapping like :registration-settings -> 'Registration Settings'" 
  ([coll]
     (same-name identity coll))
  ([word-fn coll]
     (zipmap coll
	     (for [keyword coll] (->> keyword name (split #"-") (map word-fn) (join " "))))))

(defn define-elements [window m]
  (zipmap (keys m) (for [v (vals m)] (Element. window v))))

(defn define-tabs [tabgroup m]
  (zipmap (keys m) (for [v (vals m)] (Tab. tabgroup v))))

(defn define-windows [m]
  (zipmap (keys m) (for [v (vals m)] (Window. v))))

(def windows (define-windows
	       {:main-window "Subscription Manager"
		      :register-dialog "register_dialog"
		      :error-dialog "Error" 
		      :question-dialog "Question"  
		      :facts-dialog "facts_dialog" 
		      :progress-dialog "Progress Dialog"
		      :contract-selection-dialog "Contract Selection"
          :proxy-config-dialog "Advanced Network Configuration"
          :compliance-assistant-dialog "Subscription Manager - Compliance Assistant"
          :information-dialog "Information"}))


(def elements (merge
		 (define-elements (windows :main-window)
		   (merge
		    (same-name capitalize [:registration-settings
					                     :register-system
					                     :unregister-system
					                     :add-subscription
					                     :view-system-facts
					                     :glossary
					                     :become-compliant
					                     :all-available-subscriptions
					                     :my-subscriptions
					                     :my-installed-software
					                     :search
					                     :subscribe
                               :all-subscriptions-view
                               :my-subscriptions-view
                               :match-system
                               :match-installed
                               :do-not-overlap
                               :contain-text
                               :text-in-subscription
                               :unsubscribe
                               :proxy-configuration ])
		    {:more-search-options "More search options"
         :contains-the-text "Text in Subscription"
         :date-entry "date-entry"}))
		    {:main-tabgroup (TabGroup. (windows :main-window) "ptl0")}
		 (define-elements (windows :register-dialog)
        {:redhat-login "account_login"
         :password "account_password"
         :system-name "consumer_name"
         :automatically-subscribe "auto_bind"
         :register "register_button"
         :register-cancel "cancel_button"})
		 (define-elements (windows :question-dialog)
		   (same-name capitalize [:yes
		                          :no ]))
		 (define-elements (windows :facts-dialog)
		   {:facts-view "facts_view"
		    :close-facts "close_button"} )
		 (define-elements (windows :error-dialog)
		   {:ok-error "OK"
		    :error-msg "lbl[A-Za-z]*"})
		 (define-elements (windows :contract-selection-dialog)
	     {:contract-selection-table "tbl0"
	      :cancel-contract-selection "Cancel"
	      :subscribe-contract-selection "Subscribe"})
     (define-elements (windows :proxy-config-dialog)
       (merge (same-name capitalize [:proxy-checkbox
                                     :authentication-checkbox
                                     :proxy-text
                                     :password-text
                                     :username-text])
             {:close-proxy "Close Button"
              :proxy-location "Proxy Location:"}))
      (define-elements (windows :information-dialog)
        {:info-ok "OK"})
      (define-elements (windows :compliance-assistant-dialog)
        {:date-entry "date-entry"
         :update "Update"}) ))


(def tabs (define-tabs (elements :main-tabgroup)
	    (same-name capitalize [:all-available-subscriptions 
				   :my-subscriptions
				   :my-installed-software])))
		  

(def all-elements (merge windows elements tabs))


;; let clojure keywords represent locators.  When you call locator on
;; a keyword, it looks up that keyword in the all-elements map. 

(extend-protocol ldtp/LDTPLocatable
  clojure.lang.Keyword
  (locator [this] (let [locatable (all-elements this)]
		    (if-not locatable
		      (throw (IllegalArgumentException. (str "Key not found in UI mapping: " this))))
		    (ldtp/locator locatable))))




