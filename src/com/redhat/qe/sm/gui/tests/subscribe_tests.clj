(ns com.redhat.qe.sm.gui.tests.subscribe-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+ throw+)]
        [clojure.string :only (split)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
            [com.redhat.qe.sm.gui.tasks.candlepin-tasks :as ctasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test DataProvider]))

(def productlist (atom {}))

(defn build-subscription-map
  []
  (reset! productlist (ctasks/build-product-map :all? true))
  @productlist)

(defn allsearch
  ([filter]
     (tasks/search :match-system? false
                   :do-not-overlap? false
                   :contain-text filter))
  ([] (allsearch nil)))

(defn ^{BeforeClass {:groups ["setup"]}}
  register [_]
  (tasks/register-with-creds))

(defn subscribe_all
  "Subscribes to everything available"
  []
  (tasks/search)
  (tasks/do-to-all-rows-in
   :all-subscriptions-view 1
   (fn [subscription]
     (try+ (tasks/subscribe subscription)
           (catch [:type :item-not-available] _)
           (catch [:type :wrong-consumer-type]
               {:keys [log-warning]} (log-warning))))
   :skip-dropdowns? true))

(defn unsubscribe_all
  "Unsubscribes from everything available"
  []
  (tasks/ui selecttab :my-subscriptions)
  (tasks/do-to-all-rows-in
   :my-subscriptions-view 0
   (fn [subscription]
     (try+
      (tasks/unsubscribe subscription)
      (verify (= (tasks/ui rowexist? :my-subscriptions-view subscription) false))
      (catch [:type :not-subscribed] _)))
   :skip-dropdowns? true))

(defn ^{Test {:groups ["subscribe"]
              :dataProvider "subscriptions"}}
  subscribe_each [_ subscription]
  (try+
   (tasks/subscribe subscription)
   (catch [:type :item-not-available] _)
   (catch [:type :wrong-consumer-type]
       {:keys [log-warning]} (log-warning))))

(defn ^{Test {:groups ["subscribe"]
              :dataProvider "subscribed"}}
  unsubscribe_each [_ subscription]
  (tasks/ui selecttab :my-subscriptions)
  (try+ (tasks/unsubscribe subscription)
        (verify (= false (tasks/ui rowexist? :my-subscriptions-view subscription)))
        (catch [:type :not-subscribed] _)))

(defn ^{Test {:groups ["subscribe" "blockedByBug-703920"]
              :dataProvider "subscriptions"}}
  check_contract_selection_dates
  "https://bugzilla.redhat.com/show_bug.cgi?id=703920"
  [_ subscription]
  (try+ (tasks/open-contract-selection subscription)
        (try
          (loop [row (- (tasks/ui getrowcount :contract-selection-table) 1)]
            (if (>= row 0)
              (let [startdate (tasks/ui getcellvalue :contract-selection-table row 3)
                    enddate (tasks/ui getcellvalue :contract-selection-table row 4)
                    datex (tasks/get-locale-regex)]
                (verify (not (nil? (re-matches datex startdate))))
                (verify (not (nil? (re-matches datex enddate))))
                (recur (dec row)))))
          (finally
           (tasks/ui click :cancel-contract-selection)))
        (catch [:type :item-not-available] _)
        (catch [:type :wrong-consumer-type]
            {:keys [log-warning]} (log-warning))))


(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-766778"
                       "blockedByBug-723248"]
              :dataProvider "subscriptions"}}
  check_quantity_scroller
  "https://bugzilla.redhat.com/show_bug.cgi?id=723248#c3"
  [_ subscription]
  (try+
    (tasks/open-contract-selection subscription)
    (loop [row (- (tasks/ui getrowcount :contract-selection-table) 1)]
      (if (>= row 0)
        (let [contract (tasks/ui getcellvalue :contract-selection-table row 0)
              pool (ctasks/get-pool-id (@config :username)
                                      (@config :password)
                                      (@config :owner-key)
                                      subscription
                                      contract)
              usedmax (tasks/ui getcellvalue :contract-selection-table row 2)
              default (tasks/ui getcellvalue :contract-selection-table row 6)
              used (first (split usedmax #" / "))
              max (last (split usedmax #" / "))
              available (str (- (Integer. max) (Integer. used)))
              repeat-cmd (fn [n cmd] (apply str (repeat n cmd)))
              enter-quantity (fn [num]
                               (tasks/ui generatekeyevent
                                         (str (repeat-cmd 6 "<right> ")
                                              "<space>"
                                              (when num (str " " num " <enter>")))))
              get-quantity (fn []
                             (tasks/ui getcellvalue :contract-selection-table row 6))
              get-quantity-int (fn [] (Integer. (get-quantity)))]
          (if (ctasks/multi-entitlement? (@config :username) (@config :password) pool)
            (do
              ;verify that the quantity can be changed
              (tasks/ui selectrowindex :contract-selection-table row)
              (enter-quantity available)
              (verify (= available
                         (get-quantity)))
              ;verify that the quantity cannot exceed the max
              (enter-quantity (str (+ 1 (Integer. available))))
              (verify (>= (Integer. available)
                          (get-quantity-int)))
              ;verify that the quantity cannot exceed the min
              (enter-quantity "-1")
              (verify (<= 0 (get-quantity-int)))
              ;verify max and min values for scroller
              (enter-quantity nil)
              (tasks/ui generatekeyevent (str
                                          (repeat-cmd (+ 2 (Integer. max)) "<up> ")
                                          "<enter>"))
              (verify (>= (Integer. max) (get-quantity-int)))
              (enter-quantity nil)
              (tasks/ui generatekeyevent (str
                                          (repeat-cmd (+ 2 (Integer. max)) "<down> ")
                                          "<enter>"))
              (verify (<= 0 (get-quantity-int))))
            (do
              ;verify that the quantity cannot be changed
              (tasks/ui selectrowindex :contract-selection-table row)
              (enter-quantity max)
              (verify (= default (get-quantity)))))
          (recur (dec row)))))
    (catch [:type :subscription-not-available] _)
    (catch [:type :wrong-consumer-type]
        {:keys [log-warning]} (log-warning))
    (finally (if (tasks/ui showing? :contract-selection-table)
               (tasks/ui click :cancel-contract-selection)))))

(defn ^{Test {:groups ["subscribe" "blockedByBug-723248"]
              :dataProvider "multi-entitle"}}
  check_quantity_subscribe
  "https://bugzilla.redhat.com/show_bug.cgi?id=723248#c3"
  [_ subscription contract]
  (try+
   (.runCommandAndWait @clientcmd "subscription-manager unsubscribe --all")
   (tasks/sleep 3000)
   (tasks/search)
   (tasks/open-contract-selection subscription)
    (tasks/ui selectrow :contract-selection-table contract)
    (let [line (tasks/ui gettablerowindex :contract-selection-table contract)
          usedmax (tasks/ui getcellvalue :contract-selection-table line 2)
          used (first (split usedmax #" / "))
          max (last (split usedmax #" / "))
          available (str (- (Integer. max) (Integer. used)))
          repeat-cmd (fn [n cmd] (apply str (repeat n cmd)))
          enter-quantity (fn [num]
                           (tasks/ui generatekeyevent
                                     (str (repeat-cmd 6 "<right> ")
                                          "<space>"
                                          (when num (str " "
                                                         (repeat-cmd (.length max) "<backspace> ")
                                                         num " <enter>")))))]
      (enter-quantity available)
      (tasks/sleep 500)
      (tasks/ui click :subscribe-contract-selection)
      (tasks/checkforerror)
      (tasks/wait-for-progress-bar)
      (tasks/ui selecttab :my-subscriptions)
      (let [row (tasks/ui gettablerowindex :my-subscriptions-view subscription)
            count (tasks/ui getcellvalue :my-subscriptions-view row 3)]
        (verify (= count available))))
    (tasks/unsubscribe subscription)
    (catch [:type :item-not-available] _)
    (catch [:type :wrong-consumer-type]
        {:keys [log-warning]} (log-warning))))

(defn ^{Test {:groups ["subscribe" "blockedByBug-755861"]
              :dataProvider "multi-entitle"}}
  check_quantity_subscribe_traceback [_ subscription contract]
  (let [ldtpd-log "/var/log/ldtpd/ldtpd.log"
        output (tasks/get-logging @clientcmd
                                  ldtpd-log
                                  "multi-subscribe-tracebacks"
                                  nil
                                  (check_quantity_subscribe nil subscription contract))]
    (verify (not (tasks/substring? "Traceback" output)))))

(comment
  ;; TODO: this is not in rhel6.3 branch, finish when that is released
  ;; https://bugzilla.redhat.com/show_bug.cgi?id=801434
  (defn ^{Test {:groups ["subscribe"
                         "blockedByBug-801434"
                         "blockedByBug-707041"]}}
    check_date_chooser_traceback [_]
    (try
      (let [ldtpd-log "/var/log/ldtpd/ldtpd.log"
            output (tasks/get-logging @clientcmd
                                      ldtp-log
                                      "date-chooser-tracebacks"
                                      nil
                                      (do
                                        (tasks/ui selecttab :all-available-subscriptions)
                                        (tasks/ui click :calendar)
                                        (tasks/checkforerror)))]
        (verify (not (tasks/substring? "Traceback" output))))
      (finally (tasks/restart-app))))

  (defn ^{Test {:groups ["subscribe"
                         "blockedByBug-704408"
                         "blockedByBug-801434"]
                :dependsOnMethods ["check_date_chooser_traceback"]}}
    check_blank_date_click [_]
    (try
      (tasks/ui selecttab :all-available-subscriptions)
      (tasks/ui settextvalue :date-entry "")
      ;;try clicking to another tab/area
      (tasks/ui selecttab :my-subscriptions)
      (tasks/checkforerror)
      (tasks/ui selecttab :all-available-subscriptions)
      ;; try changing the date
      (tasks/ui click :calendar)
      (tasks/checkforerror)
      (tasks/ui click :today)
      ;; verify that today's date was filled in here...
      (finally (tasks/restart-app)))) )

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-688454"
                       "blockedByBug-704408"]}}
  check_blank_date_search [_]
  (try
    (tasks/ui selecttab :all-available-subscriptions)
    (tasks/ui settextvalue :date-entry "")
    (let [error (try+ (tasks/ui click :search)
                      (tasks/checkforerror)
                      (catch Object e (:type e)))]
      (verify (= :date-error error)))
    (verify (= "" (tasks/ui gettextvalue :date-entry)))

    (comment ;this is the old test after it autofilled the date
      (let [date (tasks/ui gettextvalue :date-entry)
            systemtime (.trim (.getStdout (.runCommandAndWait @clientcmd "date +%m/%d/%Y")))]
        (verify (not (nil? (re-matches #"\d{2}/\d{2}/\d{4}" date))))
        (verify (= date systemtime))))

    (finally (tasks/restart-app))))

(defn ^{Test {:groups ["subscribe"]
              :dataProvider "installed-products"}}
  filter_by_product [_ product]
  (allsearch product)
  (let [expected (@productlist product)
        seen (into [] (tasks/get-table-elements
                       :all-subscriptions-view
                       0
                       :skip-dropdowns? true))
        hasprod? (fn [s] (tasks/substring? product s))
        inmap? (fn [e] (some hasprod? (flatten e)))
        matches (flatten (filter inmap? @productlist))
        not-nil? (fn [b] (not (nil? b)))]
    (doseq [s seen]
      (verify (not-nil? (some #{s} matches))))
    (doseq [e expected]
      (verify (not-nil? (some #{e} seen))))))

(comment
  ;; TODO:
(defn ^{Test {:groups ["subscribe" "blockedByBug-740831"]}}
  check_subscribe_greyout [_]
  ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DATA PROVIDERS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; you can test data providers in a REPL the following way:
;; (doseq [s (stest/get_subscriptions nil :debug true)]
;;   (stest/subscribe_each nil (peek s)))

(defn ^{DataProvider {:name "multi-entitle"}}
  get_multi_entitle_subscriptions [_ & {:keys [debug]
                                        :or {debug false}}]
  (tasks/restart-app)
  (register nil)
  (tasks/search)
  (let [subs (atom [])
        subscriptions (tasks/get-table-elements
                       :all-subscriptions-view
                       0
                       :skip-dropdowns? true)]
    (doseq [s subscriptions]
      (try+
        (tasks/open-contract-selection s)
        (loop [row (- (tasks/ui getrowcount :contract-selection-table) 1)]
          (if (>= row 0)
            (let [contract (tasks/ui getcellvalue :contract-selection-table row 0)
                  pool (ctasks/get-pool-id (@config :username)
                                          (@config :password)
                                          (@config :owner-key)
                                          s
                                          contract)]
              (if (ctasks/multi-entitlement? (@config :username) (@config :password) pool)
                (swap! subs conj [s contract]))
              (recur (dec row)))))
        (tasks/ui click :cancel-contract-selection)
        (catch [:type :subscription-not-available] _)
        (catch [:type :wrong-consumer-type]
            {:keys [log-warning]} (log-warning))))
    (if-not debug
      (to-array-2d @subs)
      @subs)))

(defn ^{DataProvider {:name "subscriptions"}}
  get_subscriptions [_ & {:keys [debug]
                          :or {debug false}}]
  (tasks/restart-app)
  (register nil)
  (tasks/search)
  (let [subs (into [] (map vector (tasks/get-table-elements
                                   :all-subscriptions-view
                                   0
                                   :skip-dropdowns? true)))]
    (if-not debug
      (to-array-2d subs)
      subs)))

(defn ^{DataProvider {:name "subscribed"}}
  get_subscribed [_ & {:keys [debug]
                       :or {debug false}}]
  (tasks/restart-app)
  (register nil)
  (tasks/ui selecttab :my-subscriptions)
  (subscribe_all)
  (tasks/ui selecttab :my-subscriptions)
  (let [subs (into [] (map vector (tasks/get-table-elements
                                   :my-subscriptions-view
                                   0
                                   :skip-dropdowns? true)))]
    (if-not debug
      (to-array-2d subs)
      subs)))

(defn ^{DataProvider {:name "multi-contract"}}
  get_multi_contract_subscriptions [_ & {:keys [debug]
                                         :or {debug false}}]
  (tasks/restart-app)
  (register nil)
  (tasks/search {:do-not-overlap? false})
  (let [subs (atom [])
        allsubs (tasks/get-table-elements :all-subscriptions-view 0 :skip-dropdowns? true)]
    (doseq [s allsubs]
      (try+
       (tasks/open-contract-selection s)
       (tasks/ui click :cancel-contract-selection)
       (swap! subs conj [s])
       (catch [:type :subscription-not-available] _)
       (catch [:type :contract-selection-not-available] _
           (tasks/unsubscribe s))))
    (if-not debug
      (to-array-2d @subs)
      @subs)))

(defn ^{DataProvider {:name "installed-products"}}
  get_installed_products [_ & {:keys [debug]
                               :or {debug false}}]
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (tasks/restart-app)
  (tasks/register-with-creds)
  (let [prods (into [] (map vector (tasks/get-table-elements
                                    :installed-view
                                    0)))]
    (build-subscription-map)
    (tasks/search)
    (if-not debug
      (to-array-2d prods)
      prods)))

  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=683550
  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=691784
  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=691788
  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=727631

(gen-class-testng)
