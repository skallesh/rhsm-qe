(ns sm.gui.ui
  (:use  [clojure.contrib [string :only [join split capitalize]]])
  (:require [sm.gui.ldtp :as ldtp])
  (:import java.util.NoSuchElementException))


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
	      :manageSubscriptionsDialog {:id "manage_subscriptions_dialog"
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

(defn element-getter "Returns a function that, given a keyword, retrieves 
the window id and element id from the given element map, and return those 2 items in a vector" 
  [elem-map]
  (fn [name-kw] 
    (let [window (first (filter #(get-in % [:elements name-kw])
				(vals elem-map)))
	  elem (get-in window [:elements name-kw])]
      (if elem [(:id window) elem]
	  (throw (NoSuchElementException.
		  (format "%s not found in ui mapping." name-kw)))))))


(def elements (element-getter windows))

(comment (defprotocol Locatable "A protocol for locatable UI elements"
   (loc [this] "Returns locator information for various UI types (html, gnome, etc)"))

	 (defrecord Element [window-name elem-name]
	   Locatable
	   (loc [this] [window-name elem-name])))

