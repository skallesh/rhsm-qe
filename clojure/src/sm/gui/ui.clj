(ns sm.gui.ui
  (:use  [clojure.contrib [string :only [join split capitalize]]])
  (:require [sm.gui.ldtp :as ldtp])
  (:import java.util.NoSuchElementException))

(defrecord SMWindow [id] ldtp/LDTPLocatable
  (ldtp/locator [this] id))

(defrecord SMElement [window id] ldtp/LDTPLocatable
  (ldtp/locator [this] [(ldtp/locator window) id]))

(defrecord SMTabGroup [window id] ldtp/LDTPLocatable
  (ldtp/locator [this] [(ldtp/locator window) id]))

(defrecord SMTab [tabgroup id] ldtp/LDTPLocatable
  (ldtp/locator [this] (flatten [(ldtp/locator tabgroup) id])))

(defn same-name "takes a collection of keywords like :registration-settings
and returns a mapping like :registration-settings -> 'Registration Settings'" 
  ([coll]
     (same-name identity coll))
  ([word-fn coll]
     (zipmap coll
	     (for [keyword coll] (->> keyword name (split #"-") (map word-fn) (join " "))))))

(defn define-elements [window m]
  (zipmap (keys m) (for [v (vals m)] (SMElement. window v))))

(defn define-tabs [tabgroup m]
  (zipmap (keys m) (for [v (vals m)] (SMTab. tabgroup v))))

(defn define-windows [m]
  (zipmap (keys m) (for [v (vals m)] (SMWindow. v))))

(def windows (define-windows
	       {:main-window "Subscription Manager"
		:register-dialog "register_dialog"
		:error-dialog "Error" 
		:question-dialog "Question"  
		:facts-dialog "facts_dialog" 
		:progress-dialog "Progress Dialog"
		:contract-selection-dialog "Contract Selection"}))


(def elements (merge
		 (define-elements (windows :main-window)
		   (merge
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
		    {:available-subscription-table "tbl3"}))
		 {:main-tabgroup (SMTabGroup. (windows :main-window) "ptl0")}
		 (define-elements (windows :register-dialog)
		   {:redhat-login "account_login"
		    :password "account_password"
		    :system-name "consumer_name"
		    :automatically-subscribe "auto_bind"
		    :register "register_button"
		    :register-cancel "cancel_button"})
		 (define-elements (windows :question-dialog)
		   (same-name capitalize [:yes :no]))
		 (define-elements (windows :facts-dialog)
		   {:facts-view "facts_view"
		    :close-facts "close_button"} )
		 (define-elements (windows :error-dialog)
		   {:ok-error "OK"})
		  (define-elements (windows :contract-selection-dialog)
		  {:contract-selection-table "tbl0"
		   :cancel-contract-selection "Cancel"
		   :subscribe-contract-selection "Subscribe"})))

(def tabs (define-tabs (elements :main-tabgroup)
	    (same-name capitalize [:all-available-subscriptions 
				   :my-subscriptions
				   :my-installed-software])))
		  

(def all-elements (merge windows elements tabs))

;;A map of keywords to the GNOME ui data



(defn element-getter "Returns a function that, given a keyword,
retrieves the relevant UI ids from the element map, and return those
items in a vector"
  [elem-map]
  (fn [name-kw]
    (let [elem (all-elements name-kw)]
      (if-not elem (throw (IllegalArgumentException. (str "Key not found in UI mapping: " name-kw))))
      (ldtp/locator elem))))


(def element (element-getter all-elements))

(def action (ldtp/action-with-uimap element))


