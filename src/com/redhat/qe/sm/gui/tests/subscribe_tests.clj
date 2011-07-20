(ns com.redhat.qe.sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.string :only (split)]
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


(defn ^{Test {:groups ["subscribe"]
              :dataProvider "subscribed"}}
  unsubscribe_each [_ subscription]
  (tasks/ui selecttab :my-subscriptions)
  (with-handlers [(ignore :not-subscribed)]
    (tasks/unsubscribe subscription)
    (verify (= false (tasks/ui rowexist? :my-subscriptions-view subscription)))))


(defn ^{Test {:groups ["subscribe" "blockedByBug-703920"]
              :dataProvider "subscriptions"}}
  check_contract_selection_dates
  "https://bugzilla.redhat.com/show_bug.cgi?id=703920"
  [_ subscription]
  (with-handlers [(ignore :subscription-not-available)
                  (handle :wrong-consumer-type [e]
                          (recover e :log-warning))]
    (tasks/open-contract-selection subscription)
    (loop [row (- (tasks/ui getrowcount :contract-selection-table) 1)]
      (if (>= row 0)
        (let [startdate (tasks/ui getcellvalue :contract-selection-table row 2)
              enddate (tasks/ui getcellvalue :contract-selection-table row 3)]
          (verify (not (nil? (re-matches #"\d+/\d+/\d+" startdate))))
          (verify (not (nil? (re-matches #"\d+/\d+/\d+" enddate))))
          (recur (dec row)))))
    (tasks/ui click :cancel-contract-selection)))


(defn ^{Test {:groups ["subscribe" "blockedByBug-723248"]
              :dataProvider "subscriptions"}}
  check_quantity_scroller
  "https://bugzilla.redhat.com/show_bug.cgi?id=723248#c3"
  [_ subscription]
  (with-handlers [(ignore :subscription-not-available)
                  (handle :wrong-consumer-type [e]
                          (recover e :log-warning))]
    (tasks/open-contract-selection subscription)
    (loop [row (- (tasks/ui getrowcount :contract-selection-table) 1)]
      (if (>= row 0)
        (let [contract (tasks/ui getcellvalue :contract-selection-table row 0)
              pool (tasks/get-pool-id (@config :username)
                                      (@config :password)
                                      (@config :owner-key)
                                      subscription
                                      contract)
              usedmax (tasks/ui getcellvalue :contract-selection-table row 1)
              default (tasks/ui getcellvalue :contract-selection-table row 4)
              max (last (split #" / " usedmax))
              cmd (fn [num]
                    (str  "<right> <right> <right> <right> <space> " num " <enter>"))]
          (if (tasks/multi-entitlement? (@config :username) (@config :password) pool)
            (do
              ;verify that the quantity can be changed
              (tasks/ui selectrowindex :contract-selection-table row)
              (tasks/ui generatekeyevent (cmd max))
              (verify (= max
                         (tasks/ui getcellvalue :contract-selection-table row 4)))
              ;verify that the quantity cannot exceed the max
              (tasks/ui generatekeyevent (cmd (str (+ 1 (Integer. max)))))
              (verify (>= (Integer. max)
                          (Integer. (tasks/ui getcellvalue :contract-selection-table row 4))))
              ;need to add a case for seeing quantitiy subscribed = quantity selected
              ;  possibly a completely separate test?
              )
            (do
              ;verify that the quantity cannot be changed
              (tasks/ui selectrowindex :contract-selection-table row)
              (tasks/ui generatekeyevent (cmd max))
              (verify (= default
                        (tasks/ui getcellvalue :contract-selection-table row 4)))))
          (recur (dec row)))))
    (tasks/ui click :cancel-contract-selection)))



(defn ^{DataProvider {:name "subscriptions"}}
  get_subscriptions [_]
  (tasks/search {})
  (to-array-2d (map vector (tasks/get-table-elements :all-subscriptions-view 0))))

(defn ^{DataProvider {:name "subscribed"}}
  get_subscribed [_]
  (tasks/ui selecttab :my-subscriptions)
  (if (> 0 (tasks/ui getrowcount :my-subscriptions-view)) 
    (to-array-2d (map vector (tasks/get-table-elements :my-subscriptions-view 0)))
    (do (subscribe_all)
        (tasks/ui selecttab :my-subscriptions)
        (to-array-2d (map vector (tasks/get-table-elements :my-subscriptions-view 0))))))

  ;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=683550
  ;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=691784
  ;; TODO https://bugzilla.redhat.com/show_bug.cgi?id=691788

(gen-class-testng)
