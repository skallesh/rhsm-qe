(ns com.redhat.qe.sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test DataProvider]))

(defn ^{BeforeClass {:groups ["setup"]}}
  register [_]
  (let [ownername (tasks/get-owner-display-name (@config :username)
                                                (@config :password)
                                                (@config :owner-key))]
    (with-handlers [(handle :already-registered [e]
                            (recover e :unregister-first))]
      (tasks/register (@config :username)
                      (@config :password)
                      :owner ownername))))

(defn subscribe_all
  "Subscribes to everything available"
  []
  (tasks/search {})
  (tasks/do-to-all-rows-in :all-subscriptions-view 0
                           (fn [subscription]
                                (with-handlers [(ignore :subscription-not-available)
                                                (handle :wrong-consumer-type [e]
                                                        (recover e :log-warning))]
                                  (tasks/subscribe subscription)))))

(defn unsubscribe_all 
  "Unsubscribes from everything available"
  []
  (tasks/ui selecttab :my-subscriptions)
  (tasks/do-to-all-rows-in :my-subscriptions-view 0
                           (fn [subscription] (with-handlers [(ignore :not-subscribed)]
                                               (tasks/unsubscribe subscription)
                                               (verify (= (tasks/ui rowexist? :my-subscriptions-view subscription) false))))))

(defn ^{Test {:groups ["subscribe"]
              :dataProvider "subscriptions"}}
  subscribe_each [_ subscription]
  (with-handlers [(ignore :subscription-not-available)
                  (handle :wrong-consumer-type [e]
                          (recover e :log-warning))]
    (tasks/subscribe subscription)))

(defn ^{DataProvider {:name "subscriptions"}}
  get_subscriptions [_]
  (tasks/search {})
  (to-array-2d (map vector (tasks/get-table-elements :all-subscriptions-view 0))))


(defn ^{Test {:groups ["subscribe"]
              :dataProvider "subscribed"}}
  unsubscribe_each [_ subscription]
  (tasks/ui selecttab :my-subscriptions)
  (with-handlers [(ignore :not-subscribed)]
    (tasks/unsubscribe subscription)
    (verify (= false (tasks/ui rowexist? :my-subscriptions-view subscription)))))

(defn ^{DataProvider {:name "subscribed"}}
  get_subscribed [_]
  (tasks/ui selecttab :my-subscriptions)
  (if (> 0 (tasks/ui getrowcount :my-subscriptions-view)) 
    (to-array-2d (map vector (tasks/get-table-elements :my-subscriptions-view 0)))
    (do (subscribe_all)
        (tasks/ui selecttab :my-subscriptions)
        (to-array-2d (map vector (tasks/get-table-elements :my-subscriptions-view 0))))))

(comment 

(defn ^{Test {:groups ["subscribe" "blockedByBug-679961" "blockedByBug-714306"]
              :dataProvider "subscribed"}}
  check_unsubscribe_clear
  "https://bugzilla.redhat.com/show_bug.cgi?id=679961
  https://bugzilla.redhat.com/show_bug.cgi?id=714306"
  [_ subscription]
  )

)

(defn ^{Test {:groups ["subscribe" "blockedByBug-703920"]
              :dataProvider "subscriptions"}}
  check_contract_selection_dates
  "https://bugzilla.redhat.com/show_bug.cgi?id=703920"
  [_ subscription]
  (with-handlers [(ignore :subscription-not-available)
                  (handle :wrong-consumer-type [e]
                          (recover e :log-warning))]
    (tasks/open-contract-selection subscription)
    (loop [row (- (tasks/ui getrowcount :contract-selection-dialog "tbl0") 1)]
      (if (>= row 0)
        (let [startdate (tasks/ui getcellvalue :contract-selection-dialog "tbl0" row 2)
              enddate (tasks/ui getcellvalue :contract-selection-dialog "tbl0" row 3)]
          (verify (not (nil? (re-matches #"\d+/\d+/\d+" startdate))))
          (verify (not (nil? (re-matches #"\d+/\d+/\d+" enddate))))
          (recur (dec row)))))
    (tasks/ui click :contract-selection-dialog "Cancel"))) 


  ;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=683550
  ;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=691784
  ;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=691788

(gen-class-testng)
