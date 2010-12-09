(ns sm.gui.ui
  (:use  [clojure.contrib [string :only [join split capitalize]]])
  (:require [sm.gui.ldtp :as ldtp])
  (:import java.util.NoSuchElementException))


(defn same-name "takes a collection of keywords like :registration-settings
and returns a mapping like :registration-settings -> 'Registration Settings'" 
  ([coll]
     (same-name identity coll))
  ([word-fn coll]
     (zipmap coll
	     (for [keyword coll] (->> keyword name (split #"-") (map word-fn) (join " "))))))

;;A map of keywords to the GNOME ui data

(def windows {:mainWindow  {:id "Subscription Manager"
			    :elements (merge
				       (same-name capitalize [:registration-settings
							      :register-system
							      :unregister-system
							      :add-subscription
							      :view-my-system-facts
							      :glossary
							      :become-compliant
							      :all-available-subscriptions
							      :my-subscriptions
							      :my-installed-software
							      :search
							      :subscribe])
				       (same-name [:match-my-hardware
						   :overlap-with-existing-subscriptions
						   :provide-software-not-yet-installed
						   :contain-the-text])
				       {:available-subscription-table "tbl3"})}
             :registerDialog {:id "register_dialog"
                               :elements {:redhat-login "account_login"
                                          :password "account_password"
                                          :system-name "consumer-name"
                                          :automatically-subscribe "auto_bind"
                                          :register "register_button"
					  :register-cancel "cancel_button"}}
              :registrationSettingsDialog {:id "register_token_dialog"
                                           :elements {:registration-token "regtoken-entry"}}
              :error-dialog {:id "Error"
			     :elements {:ok-error "OK"}}
              :question-dialog {:id "Question"
			       :elements (same-name [:yes :no])} 
              :facts-dialog {:id "facts_dialog"
			     :elements {:facts-view "facts_view"
					:close-facts "close_button"}}
              :subscribeDialog {:id "dialog_add"}
	      :progress-dialog {:id "Progress Dialog"}
	      :contract-selection-dialog {:id "Contract Selection"
					  :elements {:contract-selection-table "tbl0"
						     :cancel-contract-selection "Cancel"
						     :subscribe-contract-selection "Subscribe"}}
	      })

(defn element-getter "Returns a function that, given a keyword, retrieves 
the window id and element id from the given element map, and return those 2 items in a vector" 
  [elem-map]
  (fn [name-kw] 
    (let [containing-window (first (filter #(get-in % [:elements name-kw])
					   (vals elem-map)))
	  elem (get-in containing-window [:elements name-kw])
	  window (get-in elem-map [name-kw :id])]
      (cond elem [(:id containing-window) elem]
	    window [window]
	    :else (throw (NoSuchElementException.
			  (format "%s was not found in ui mapping." name-kw)))))))


(def element (element-getter windows))

(def action (ldtp/action-with-uimap element))


