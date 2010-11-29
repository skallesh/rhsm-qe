(ns sm.gui.ui
  (:use  [clojure.contrib [string :only [join split capitalize]]])
  (:require [sm.gui.ldtp :as ldtp]))


(defn same-name "takes a collection of keywords like :registration-settings
and returns a mapping like :registration-settings -> 'Registration Settings'" 
  [coll]
  (zipmap coll
	  (for [keyword coll] (->> keyword name (split #"-") (map capitalize) (join " ")))))

;;A map of keywords to the GNOME ui data
(def windows {:mainWindow  {:id "manage_subscriptions_dialog"
                            :elements {:close-main "button_close"
                                       :add "add_button"
                                       :registration "account_settings"}}
              :registerDialog {:id "register_dialog"
                               :elements {:redhat-login "account_login"
                                          :password "accound_password"
                                          :system-name "consumer-name"
                                          :automatically-subscribe "auto_bind"
                                          :register "register_button"}}
              :registrationSettingsDialog {:id "register_token_dialog"
                                           :elements {:registration-token "regtoken-entry"}}
              :errorDialog "Error"
              :questionDialog "Question"
              :factsDialog "facts_dialog"
              :subscribeDialog "dialog_add"})


(def new-windows {:mainWindow {:id "Subscription Manager"
			       :elements (same-name [:registration-settings
						     :register-system
						     :add-subscription
						     :view-my-system-facts
						     :glossary
						     :become-compliant])}
		  :registerDialog (:registerDialog windows)
		  
		  })

(def elements (ldtp/element-getter new-windows))

(defprotocol Locatable "A protocol for locatable UI elements"
 (loc [this] "Returns locator information for various UI types (html, gnome, etc)"))

(defrecord Element [window-name elem-name]
  Locatable
   (loc [this] [window-name elem-name]))

