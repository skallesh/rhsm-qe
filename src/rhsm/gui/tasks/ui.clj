(ns rhsm.gui.tasks.ui
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
                :contract-selection-dialog "Contract Selection"
                :date-selection-dialog "Date Selection"
                :error-dialog "Error"
                :facts-dialog "facts_dialog"
                :file-chooser "Select A File"
                :filter-dialog "Filter Options"
                :firstboot-proxy-dialog "Proxy Configuration"
                :firstboot-window "frm0"
                :help-dialog "Subscription Manager Manual"
                :import-dialog "Import Certificates"
                :information-dialog "Information"
                :progress-dialog "Progress Dialog"
                :proxy-config-dialog "Proxy Configuration"
                :question-dialog "Question"
                ;; does not exist anymore? part of the register-dialog
                ;:subscribe-system-dialog "Subscribe System"
                :register-dialog "register_dialog"
                ;;also does not exist anymore > tests have been moved to oldtests folder
                ;:subscription-assistant-dialog "Subscription Assistant"
                :subscription-redemption-dialog "Subscription Redemption"
                :system-preferences-dialog "System Preferences"
                :warning-dialog "Warning"}))


(def elements
  (merge
    (define-elements (windows :main-window)
      (merge (same-name capitalize [:registration-settings
                                    :glossary
                                    :update-certificates
                                    :all-available-subscriptions
                                    :my-subscriptions
                                    :my-installed-products
                                    :search
                                    ;; now called attach
                                        ;:subscribe
                                    :attach
                                    :all-subscriptions-view
                                    :my-subscriptions-view
                                    :installed-view
                                    ;; now called remove
                                        ;:unsubscribe
                                    :remove
                                    :system-preferences
                                    :configure-proxy
                                    :view-system-facts
                                    :help
                                    :getting-started
                                    :online-documentation
                                    :filters
                                    :calendar])
		    {:date-entry "date-entry"
                     :register-system "Register System"
                     :redeem "Redeem a Subscription"
                     :import-certificate "Import Cert"
                     :unregister-system "Unregister"
                     :update-certificates "Update"
                     ;; now called auto-attach
                     ;:autosubscribe "Auto-subscribe"
                     :auto-attach "Auto-attach"
                     }
                    ;dynamic text fields for details sections:
                    (text-field [:arch
                                 :certificate-status
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
                     ;;stacking id removed in BZ 821544
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
         :owner-view "owner_treeview"
         :registering "Registering"
         :register-proxy-conf "proxy_button"
         :register-server "server_entry"
         :default-server "default_button"
         ;;activation-key section
         :organization "organization_entry"
         :activation-key "activation_key_entry"
         :activation-system-name "consumer_entry"
         ;; sla/auto-attach stuff below
         :sla-attach "Attach"
         :sla-forward "Forward"
         :sla-next "Next"
         :sla-cancel "Cancel"
         :sla-back "Back"
         })
    (comment ;;this stuff is now part of the register-dialog
      (define-elements (windows :subscribe-system-dialog)
        {:sla-subscribe "Subscribe"
         :sla-forward "Forward"
         :sla-cancel "Cancel"
         :sla-back "Back"}))
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
       ;; now attach
       ;:subscribe-contract-selection "Subscribe"
       :attach-contract-selection "Attach"
       })
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
    (define-elements (windows :filter-dialog)
      (merge (same-name capitalize [:match-system
                                    :match-installed
                                    :do-not-overlap])
             {:contain-the-text "Text in Subscription"
              :clear-filters "Clear"
              :close-filters "Close"}))
    (define-elements (windows :firstboot-window)
      {:firstboot-back "Back"
       :firstboot-forward "Forward"
       :license-yes "Yes, I agree to the License Agreement"
       :license-no "No, I do not agree"
       :register-now "Yes, I'd like to register now."
       :register-later "No, I prefer to register at a later time."
       :register-rhsm "Red Hat Subscription Management"
       :register-rhn "Red Hat Network (RHN) Classic"
       :register-satellite "An RHN Satellite or RHN Proxy"
       :satelite-location "Location:"
       :firstboot-proxy-config "Proxy Setup Button"
       :firstboot-server-entry "server_entry"
       :firstboot-server-default "default_button"
       :firstboot-activation-checkbox "I will use an Activation Key"
       :firstboot-user "account_login"
       :firstboot-pass "account_password"
       :firstboot-autosubscribe "auto_bind"
       :firstboot-system-name "consumer_name"
       :firstboot-owner-table "tbl0"})
    (define-elements (windows :firstboot-proxy-dialog)
      {:firstboot-proxy-checkbox "I would like to connect*"
       :firstboot-proxy-location "Proxy Location:"
       :firstboot-auth-checkbox "Use Authentication with HTTP Proxy"
       :firstboot-proxy-user "proxy user field"
       :firstboot-proxy-pass "proxy password field"
       :firstboot-proxy-close "Close"})
    (define-elements (windows :import-dialog)
      {:text-entry-toggle "Type a file name"
       :import-cert "Import"
       :import-cancel "Cancel"})
    (comment ;this is now embedded into the import window
      (define-elements (windows :file-chooser)
        {:text-entry-toggle "Type a file name"
         :file-cancel "Cancel"
         :file-open "Open"}))
    (define-elements (windows :system-preferences-dialog)
      {:close-system-prefs "Close"})
    (define-elements (windows :date-selection-dialog)
      {:today "Today"})
    (define-elements (windows :subscription-redemption-dialog)
      {:email-address "Email Address Text"
       :redeem-cancel "Cancel"
       :redeem "Redeem"})))


(def tabs (define-tabs (elements :main-tabgroup)
	    (same-name capitalize [:all-available-subscriptions
				   :my-subscriptions
				   :my-installed-products])))


(def all-elements (merge windows elements tabs))


;; let clojure keywords represent locators.  When you call locator on
;; a keyword, it looks up that keyword in the all-elements map.

(extend-protocol ldtp/LDTPLocatable
  clojure.lang.Keyword
  (locator [this] (let [locatable (all-elements this)]
		    (if-not locatable
		      (throw (IllegalArgumentException. (str "Key not found in UI mapping: " this))))
		    (ldtp/locator locatable))))




