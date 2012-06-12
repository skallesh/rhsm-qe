(ns com.redhat.qe.sm.gui.tasks.ui
  (:use  [clojure.string :only [join split capitalize]])
  (:require [gnome.ldtp :as ldtp])
  (:import java.util.NoSuchElementException
	   [gnome.ldtp Element Tab Window TabGroup]))

(defn same-name "takes a collection of keywords like :registration-settings
and returns a mapping like :registration-settings -> 'Registration Settings'" 
  ([coll]
     (same-name identity coll))
  ([word-fn coll]
     (zipmap coll
	     (for [keyword coll] (->> (split (name keyword) #"-") (map word-fn) (join " "))))))

(defn text-field [coll]
  (zipmap coll
          (for [keyword coll]
            (->> (conj (split (name keyword) #"-") "text") (map capitalize) (join  " ")))))

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
                :subscription-assistant-dialog "Subscription Assistant"
                :information-dialog "Information"
                :warning-dialog "Warning"
                :firstboot-window "frm0"
                :firstboot-proxy-dialog "Advanced Network Configuration"
                :import-window "Provide a Subscription Certificate"
                :file-chooser "Select A File"
                :subscribe-system-dialog "Subscribe System"
                :system-preferences-dialog "System Preferences"
                :help-dialog "Subscription Manager Manual"}))


(def elements
  (merge
    (define-elements (windows :main-window)
      (merge (same-name capitalize [:registration-settings
                                    :glossary
                                    :update-certificates
                                    :all-available-subscriptions
                                    :my-subscriptions
                                    :my-installed-software
                                    :search
                                    :subscribe
                                    :all-subscriptions-view
                                    :my-subscriptions-view
                                    :installed-view
                                    :match-system
                                    :match-installed
                                    :do-not-overlap
                                    :contain-text
                                    :text-in-subscription
                                    :unsubscribe
                                    :system-preferences
                                    :view-system-facts
                                    :proxy-configuration
                                    :import-certificate
                                    :help
                                    :calendar])
		    {:more-search-options "More search options"
                     :contains-the-text "Text in Subscription"
                     :date-entry "date-entry"
                     :register-system "Register"
                     :redeem "Redeem a Subscription"
                     :unregister-system "Unregister"
                     :update-certificates "Update"
                     :autosubscribe "Auto-subscribe"}
                    ;dynamic text fields for details sections:
                    (text-field [:certificate-status
                                 :installed-subscription
                                 :product
                                 :support-type
                                 :support-type
                                 :support-level
                                 :provides-management
                                 :account
                                 :start-date
                                 :end-date
                                 :subscription
                                 :all-available-support-type
                                 :all-available-support-level
                                 :all-available-subscription])
                    {:product-id "Product ID Text"
                     :stacking-id "Stacking ID Text"
                     :contract-number "Contract Number"
                     :bundled-products "Bundeled Products Table"
                     :all-available-bundled-products "All Available Bundled Products Table"}))
		    {:main-tabgroup (TabGroup. (windows :main-window) "ptl0")}
    (define-elements (windows :register-dialog)
        {:redhat-login "account_login"
         :password "account_password"
         :system-name "consumer_name"
         :skip-autobind "auto_bind"
         :register "register_button"
         :register-cancel "cancel_button"
         :owners "ownerSelectVbox"
         :owner-view "tbl0"
         :registering "Registering"
         :register-proxy-conf "Proxy Configuration"
         :register-import-cert "Import Certificates"})
    (define-elements (windows :subscribe-system-dialog)
      {:sla-subscribe "Subscribe"
       :sla-forward "Forward"
       :sla-cancel "Cancel"
       :sla-back "Back"})
    (define-elements (windows :question-dialog)
      (same-name capitalize [:yes
                             :no ]))
    (define-elements (windows :facts-dialog)
      {:facts-view "facts_view"
       :close-facts "close_button"
       :update-facts "Update Facts"})
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
    (define-elements (windows :warning-dialog)
      {:warn-ok "OK"
       :warn-cancel "Cancel"})
    ; subscription-assistant dialog no longer exists
    (define-elements (windows :subscription-assistant-dialog)
      {:first-date "*first date of invalid entitlements*"
       :different-date "A different date:"
       :assistant-date-entry "date-entry"
       :update "Update"
       :assistant-subscribe "subscribe button"
       :subscription-product-view "Invalid Product List"
       :assistant-subscription-view "Subscription List"
       :check-all "Check/uncheck all products"})
    (define-elements (windows :firstboot-window)
      {:firstboot-back "Back"
       :firstboot-forward "Forward"
       :license-yes "Yes, I agree to the License Agreement"
       :license-no "No, I do not agree"
       :register-now "Yes, I'd like to register now."
       :register-later "No, I prefer to register at a later time."
       :register-rhsm "I'd like to receive updates from Red Hat Network or Subscription Asset Manager*"
       :register-rhn "I'd like to receive updates from Red Hat Network Classic*"
       :register-satellite "I have access to a Red Hat Network Satellite*"
       :satelite-location "satellite server location"
       ;; no longer in use
       ;:rhn-classic-mode "RHN Classic Mode"
       :firstboot-proxy-config "Advanced Network Configuration button"
       :firstboot-user "account_login"
       :firstboot-pass "account_password"
       :firstboot-autosubscribe "auto_bind"
       :firstboot-system-name "consumer_name"
       :firstboot-owner-table "tbl0"})
    (define-elements (windows :firstboot-proxy-dialog)
      {:firstboot-proxy-checkbox "I would like to connect*"
       :firstboot-proxy-location "Proxy Location:"
       :firstboot-auth-checkbox "Use Authentication*"
       :firstboot-proxy-user "proxy user field"
       :firstboot-proxy-pass "proxy password field"
       :firstboot-proxy-close "Close"})
    (define-elements (windows :import-window)
      {:choose-cert "(None)"
       :import-cert "Import Certificate"
       :import-cancel "import_cancel_button"})
    (define-elements (windows :file-chooser)
      {:text-entry-toggle "Type a file name"
       :file-cancel "Cancel"
       :file-open "Open"})
    (define-elements (windows :system-preferences-dialog)
      {:close-system-prefs "Close"}) ))


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




