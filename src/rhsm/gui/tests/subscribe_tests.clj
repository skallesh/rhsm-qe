(ns rhsm.gui.tests.subscribe_tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [clojure.string :only (split
                               blank?
                               join
                               trim-newline)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            BeforeClass
            BeforeGroups
            Test
            DataProvider]))

(def productlist (atom {}))
(def servicelist (atom {}))
(def contractlist (atom {}))
(def sys-log "/var/log/rhsm/rhsm.log")

(defn build-subscription-map
  "Builds the product map and updates the productlist atom"
  []
  (reset! productlist (ctasks/build-product-map :all? true))
  @productlist)

(defn build-contract-map
  "Builds the contract/virt-type map and updates the cont"
  []
  (reset! contractlist (ctasks/build-virt-type-map :all? true))
  @contractlist)

(defn allsearch
  ([filter]
     (tasks/search :match-system? false
                   :do-not-overlap? false
                   :contain-text filter))
  ([] (allsearch nil)))

(defn ^{BeforeClass {:groups ["setup"]}}
  register [_]
  (tasks/register-with-creds)
  (reset! servicelist (ctasks/build-service-map :all? true))
  (build-contract-map))

(defn subscribe_all
  "Subscribes to everything available"
  []
  (comment ;; No longer used just here for reference
    (allsearch)
    (tasks/do-to-all-rows-in
     :all-subscriptions-view 1
     (fn [subscription]
       (try+ (tasks/subscribe subscription)
             (catch [:type :item-not-available] _)
             (catch [:type :wrong-consumer-type]
                 {:keys [log-warning]} (log-warning))))
     :skip-dropdowns? true))
  (let [all-pools (map :id (ctasks/list-available true))
        syntax (fn [item] (str "--pool=" item " "))
        command (str "subscription-manager subscribe " (clojure.string/join (map syntax all-pools)))]
    (run-command command)))

(defn unsubscribe_all
  "Unsubscribes from everything available"
  []
  (comment  ;; No longer used just here for reference
    (tasks/ui selecttab :my-subscriptions)
    (tasks/do-to-all-rows-in
     :my-subscriptions-view 0
     (fn [subscription]
       (try+
        (tasks/unsubscribe subscription)
        (verify (= (tasks/ui rowexist? :my-subscriptions-view subscription) false))
        (catch [:type :not-subscribed] _)))
     :skip-dropdowns? true))
  (run-command "subscription-manager unsubscribe --all"))

(defn ^{Test {:groups ["subscribe"
                       "acceptance"]
              :dataProvider "subscriptions"
              :priority (int 104)}}
  subscribe_each
  "Asserts that each subscripton can be subscribed to sucessfully."
  [_ subscription]
  (try+
   (tasks/subscribe subscription)
   (catch [:type :item-not-available] _)
   (catch [:type :wrong-consumer-type]
       {:keys [log-warning]} (log-warning))))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-924766"]
              :dataProvider "subscribed"
              :priority (int 100)}}
  check_subscribed_virt_type
  "Asserts that the virt type is displayed properly for all of 'My Subscriptions'"
  [_ subscription]
  (tasks/ui selecttab :my-subscriptions)
  (tasks/skip-dropdown :my-subscriptions-view subscription)
  (let [contract (tasks/ui gettextvalue :contract-number)
        type (tasks/ui gettextvalue :support-type)
        reference (get (get @contractlist subscription) contract)]
    (verify (= type reference))))

(defn ^{Test {:groups ["subscribe"
                       "acceptance"]
              :dataProvider "subscribed"
              :priority (int 101)}}
  unsubscribe_each
  "Asserts that each subscripton can be unsubscribed from sucessfully."
  [_ subscription]
  (tasks/ui selecttab :my-subscriptions)
  (try+ (tasks/unsubscribe subscription)
        (sleep 3000)
        (verify (= false (tasks/ui rowexist? :my-subscriptions-view subscription)))
        (catch [:type :not-subscribed] _)))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-703920"
                       "blockedByBug-869028"]
              :dataProvider "multi-contract"}}
  check_contract_selection_dates
  "Asserts that the dates in the contract selection dialog are displayed correctly."
  [_ subscription]
  (try+ (tasks/open-contract-selection subscription)
        (try
          (loop [row (- (tasks/ui getrowcount :contract-selection-table) 1)]
            (if (>= row 0)
              (let [startdate (tasks/ui getcellvalue :contract-selection-table row 3)
                    enddate (tasks/ui getcellvalue :contract-selection-table row 4)
                    datex (get-locale-regex)]
                (verify (not (nil? (re-matches datex startdate))))
                (verify (not (nil? (re-matches datex enddate))))
                (recur (dec row)))))
          (finally
           (tasks/ui click :cancel-contract-selection)))
        (catch [:type :item-not-available] _)
        (catch [:type :wrong-consumer-type]
            {:keys [log-warning]} (log-warning))))

  ;; https://bugzilla.redhat.com/show_bug.cgi?id=723248#c3
(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-766778"
                       "blockedByBug-723248"
                       "blockedByBug-855257"
                       "blockedByBug-962905"]
              :dataProvider "subscriptions"}}
  check_quantity_scroller
  "Tests the quantity scroller assiciated with subscriptions."
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
              multiplier (ctasks/get-instance-multiplier (@config :username)
                                                         (@config :password)
                                                         pool
                                                         :string? false)
              usedmax (tasks/ui getcellvalue :contract-selection-table row 2)
              default (first (split (tasks/ui getcellvalue :contract-selection-table row 5) #"\s"))
              used (first (split usedmax #" / "))
              max (last (split usedmax #" / "))
              available (- (Integer. max) (Integer. used))
              repeat-cmd (fn [n cmd] (apply str (repeat n cmd)))
              enter-quantity (fn [num]
                               (tasks/ui generatekeyevent
                                         (str (repeat-cmd 5 "<right> ")
                                              "<space>"
                                              (when num (str " "
                                                             (- num (mod num multiplier))
                                                             " <enter>")))))
              get-quantity (fn []
                             (first
                              (split (tasks/ui getcellvalue :contract-selection-table row 5) #"\s")))
              get-quantity-int (fn [] (Integer. (get-quantity)))]
          ;verify that the default quantity is sane for BZ: 855257
          (verify (<= (Integer. default) (- (Integer. max) (Integer. used))))
          (if (ctasks/multi-entitlement? (@config :username) (@config :password) pool)
            (do
              ;verify that the quantity can be changed
              (tasks/ui selectrowindex :contract-selection-table row)
              (enter-quantity available)
              (let [quantity (- available (mod available multiplier))]
                (verify (= quantity
                           (get-quantity-int))))
              ;verify that the quantity cannot exceed the max
              (enter-quantity (+ 1 available))
              (verify (>= available
                          (get-quantity-int)))
              ;verify that the quantity cannot exceed the min
              (enter-quantity -1)
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
              (enter-quantity (Integer. max))
              (verify (= default (get-quantity)))))
          (recur (dec row)))))
    (catch [:type :subscription-not-available] _)
    (catch [:type :wrong-consumer-type]
        {:keys [log-warning]} (log-warning))
    (catch [:type :contract-selection-not-available] _)
    (finally (if (tasks/ui showing? :contract-selection-table)
               (tasks/ui click :cancel-contract-selection)))))

;; https://bugzilla.redhat.com/show_bug.cgi?id=723248#c3
(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-723248"
                       "blockedByBug-962905"]
              :dataProvider "multi-entitle"}}
  check_quantity_subscribe
  "Asserts that the selected quantity is given when subscribed to."
  [_ subscription contract]
  (try+
   (run-command "subscription-manager unsubscribe --all")
   (sleep 3000)
   (tasks/search)
   (let [user (@config :username)
         pass (@config :password)
         pool (ctasks/get-pool-id user
                                  pass
                                  (@config :owner-key)
                                  subscription
                                  contract)
         available (ctasks/get-quantity-available user pass pool)
         multiplier (ctasks/get-instance-multiplier user pass pool)
         requested (- available (mod available multiplier))]
     (tasks/subscribe subscription contract requested)
     (tasks/ui selecttab :my-subscriptions)
      (let [row (tasks/ui gettablerowindex :my-subscriptions-view subscription)
            count (Integer. (tasks/ui getcellvalue :my-subscriptions-view row 3))]
        (verify (= count requested))))
    (tasks/unsubscribe subscription)
    (catch [:type :item-not-available] _)
    (catch [:type :wrong-consumer-type]
        {:keys [log-warning]} (log-warning))
    (catch [:type :contract-selection-not-available] _)))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-755861"
                       "blockedByBug-962905"]
              :dataProvider "multi-entitle"}}
  check_quantity_subscribe_traceback
  "Asserts no traceback is shown when subscribing in quantity."
  [_ subscription contract]
  (let [ldtpd-log "/var/log/ldtpd/ldtpd.log"
        output (get-logging @clientcmd
                                  ldtpd-log
                                  "multi-subscribe-tracebacks"
                                  nil
                                  (check_quantity_subscribe nil subscription contract))]
    (verify (not (substring? "Traceback" output)))))

(comment
  ;; TODO: this is not in rhel6.3 branch, finish when that is released
  ;; https://bugzilla.redhat.com/show_bug.cgi?id=801434
  (defn ^{Test {:groups ["subscribe"
                         "blockedByBug-801434"
                         "blockedByBug-707041"]}}
    check_date_chooser_traceback [_]
    (try
      (let [ldtpd-log "/var/log/ldtpd/ldtpd.log"
            output (get-logging @clientcmd
                                      ldtp-log
                                      "date-chooser-tracebacks"
                                      nil
                                      (do
                                        (tasks/ui selecttab :all-available-subscriptions)
                                        (tasks/ui click :calendar)
                                        (tasks/checkforerror)))]
        (verify (not (substring? "Traceback" output))))
      (finally (tasks/restart-app))))

  (defn ^{Test {:groups ["subscribe"
                         "blockedByBug-704408"
                         "blockedByBug-801434"]
                :dependsOnMethods ["check_date_chooser_traceback"]}}
    check_blank_date_click
    "Tests the behavior when the date search field is blank and you click to another area."
    [_]
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
  check_blank_date_search
  "Tests the behavior when the date search is blank and you try to search."
  [_]
  (try
    (tasks/ui selecttab :all-available-subscriptions)
    (tasks/ui settextvalue :date-entry "")
    (let [error (try+ (tasks/ui click :search)
                      (tasks/checkforerror)
                      (catch Object e (:type e)))]
      (verify (= :date-error error)))
    (verify (= "" (tasks/ui gettextvalue :date-entry)))
    (finally (tasks/restart-app))))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-858773"]
              :dataProvider "installed-products"}}
  filter_by_product
  "Tests that the product filter works when searching."
  [_ product]
  (allsearch product)
  (let [expected (@productlist product)
        seen (into [] (tasks/get-table-elements
                       :all-subscriptions-view
                       0
                       :skip-dropdowns? true))
        hasprod? (fn [s] (substring? product s))
        inmap? (fn [e] (some hasprod? (flatten e)))
        matches (flatten (filter inmap? @productlist))]
    (doseq [s seen]
      (verify (not-nil? (some #{s} matches))))
    (doseq [e expected]
      (verify (not-nil? (some #{e} seen))))))

(comment
  ;; TODO:
(defn ^{Test {:groups ["subscribe" "blockedByBug-740831"]}}
  check_subscribe_greyout [_]
  ))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-817901"]}}
  check_no_search_results_message
  "Tests the message when the search returns no results."
  [_]
  (tasks/restart-app :reregister? true)
  (tasks/ui selecttab :all-available-subscriptions)
  (tasks/search :contain-text "DOESNOTEXIST")
  (let [label "No subscriptions match current filters."]
    (verify (tasks/ui showing? :no-subscriptions-label))
    (verify (= label (tasks/ui gettextvalue :no-subscriptions-label))))
  (tasks/search)
  (verify (not (tasks/ui showing? :no-subscriptions-label))))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-817901"]}}
  check_please_search_message
  "Tests for the initial message before you search."
  [_]
  (tasks/restart-app :reregister? true)
  (tasks/ui selecttab :all-available-subscriptions)
  (let [label "Press Update to search for subscriptions."]
    (verify (tasks/ui showing? :no-subscriptions-label))
    (verify (= label (tasks/ui gettextvalue :no-subscriptions-label))))
  (tasks/search)
  (verify (not (tasks/ui showing? :no-subscriptions-label))))

;;https://tcms.engineering.redhat.com/case/77359/?from_plan=2110
(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-911386"]
              :dataProvider "subscriptions"}}
  check_service_levels
  "Asserts that the displayed service levels are correct in the subscriptons view."
  [_ subscription]
  (tasks/ui selecttab :all-available-subscriptions)
  (tasks/skip-dropdown :all-subscriptions-view subscription)
  (let [guiservice (tasks/ui gettextvalue :all-available-support-level-and-type)
        rawservice (get @servicelist subscription)
        service (str (or (:support_level rawservice) "Not Set")
                     (when (:support_type rawservice)
                       (str ", " (:support_type rawservice))))]
    (verify (= guiservice service))))

(defn ^{Test {:groups ["subscribe"
                       "acceptance"
                       "blockedByBug-874624"
                       "blockedByBug-753057"]
              :dataProvider "subscriptions"}}
  check_contracts_and_virt_type
  "Asserts that the contract number and virt type of each subscription is displayed properly"
  [_ subscription]
  (tasks/ui selecttab :all-available-subscriptions)
  (let [row (tasks/skip-dropdown :all-subscriptions-view subscription)
        overall-type (tasks/ui getcellvalue :all-subscriptions-view row 1)]
    (verify (= overall-type (:overall (get @contractlist subscription)))))
  (try+
    (tasks/open-contract-selection subscription)
    (tasks/do-to-all-rows-in
     :contract-selection-table
     0
     (fn [contract]
       (verify (not-nil? (some #{contract}
                               (keys (get @contractlist subscription)))))
       (verify (= (tasks/ui getcellvalue :contract-selection-table
                            (tasks/ui gettablerowindex :contract-selection-table contract)
                            1)
                  (get (get @contractlist subscription) contract)))))
    (catch [:type :contract-selection-not-available] _)
    (finally (if (tasks/ui showing? :contract-selection-table)
               (tasks/ui click :cancel-contract-selection)))))

(defn ^{Test {:groups ["subscribe"
                       "acceptance"
                       "blockedByBug-877579"]
              :dataProvider "unlimited-pools"}}
  check_unlimited_quantities
  "Tests that unlimted pools are displayed properly"
  [_ subscription contract]
  (let [row (tasks/skip-dropdown :all-subscriptions-view subscription)
        quantity (tasks/ui getcellvalue :all-subscriptions-view row 2)]
    (verify (= "Unlimited" quantity)))
  (try+
   (tasks/open-contract-selection subscription)
   (let [row (tasks/ui gettablerowindex :contract-selection-table contract)
         total (last (split (tasks/ui getcellvalue :contract-selection-table row 2) #" / "))
         quantity (first (split (tasks/ui getcellvalue :contract-selection-table row 5) #" "))]
     (verify (= "Unlimited" total))
     (verify (= "Unlimited" quantity)))
   (catch [:type :contract-selection-not-available] _)
   (finally (if (tasks/ui showing? :contract-selection-table)
              (tasks/ui click :cancel-contract-selection)))))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-918617"]
              :priority (int 102)}}
  subscribe_check_syslog
  "Asserts that subscribe events are logged in the syslog."
  [_]
  (allsearch)
  (let [subscription (rand-nth (tasks/get-table-elements :all-subscriptions-view 0))
        output (get-logging @clientcmd
                                  sys-log
                                  "subscribe_check_syslog"
                                  nil
                                  (tasks/subscribe subscription))]
      (verify (not (blank? output)))))

(defn ^{Test {:groups ["subscribe"
                       "blockedByBug-918617"]
              :dependsOnMethods ["subscribe_check_syslog"]
              :priority (int 103)}}
  unsubscribe_check_syslog
  "Asserts that unsubscribe events are logged in the syslog."
  [_]
  (let [subscription (rand-nth (tasks/get-table-elements :my-subscriptions-view 0))
        output (get-logging @clientcmd
                                  sys-log
                                  "unsubscribe_check_syslog"
                                  nil
                                  (tasks/unsubscribe subscription))]
    (verify (not (blank? output)))))

(defn ^{Test {:group ["subscribe"
                      "blockedByBug-951633"]
              :dependsOnMethods ["subscribe_each"]
              :priority (int 105)}}
  product_with_comma_separated_arch
  "This is to assert products with comma seperated products when subscribed are fully subscribed"
  [_]
  (tasks/do-to-all-rows-in :installed-view 0
                           (fn [subscription]
                             (let [index (tasks/skip-dropdown :installed-view subscription)
                                   sub-arch (tasks/ui gettextvalue :arch)
                                   machine-arch (trim-newline (:stdout (run-command "uname -m")))]
                               (if (and (substring? "," (tasks/ui gettextvalue :arch))
                                        (or (substring? sub-arch machine-arch) substring? machine-arch sub-arch()))
                                 (verify ( = "Subscribed" (tasks/ui getcellvalue :installed-view index 2))))))))

(defn ^{Test {:group ["subscribe"
                      "blockedByBug-950672"
                      "blockedByBug-988411"]
              :dependsOnMethods ["subscribe_each"]
              :priority (int 106)}}
  check_subscription_in_subscribed_products
  "Asserts there is a valid subscription value for all Subscribed products"
  [_]
  (tasks/do-to-all-rows-in :installed-view 0
                           (fn [subscription]
                             (tasks/skip-dropdown :installed-view subscription)
                             (if (not (= "Not Subscribed" (tasks/ui gettextvalue :certificate-status)))
                               (verify (not (blank? (tasks/ui gettextvalue :providing-subscriptions))))))))

(defn ^{Test {:group ["subscribe"
                      "blockedByBug-909467"
                      "blockedByBug-988411"]
              :dependsOnMethods ["subscribe-each"]
              :priority (int 107)}}
  check_subscription_compliance
  "Checks for status of subscriptions when archs dont match that of the system"
  [_]
  (let [machine-arch (trim-newline (:stdout (run-command "uname -m")))]
    (tasks/do-to-all-rows-in :installed-view 0
                             (fn [subscription]
                               (tasks/skip-dropdown :installed-view subscription)
                               (let
                                   [sub-name (tasks/ui gettextvalue :providing-subscriptions)
                                    sub-arch (tasks/ui gettextvalue :arch)
                                    check-arch (or (substring? sub-arch machine-arch)
                                                   (substring? machine-arch sub-arch))
                                    arch-all (= "ALL" (tasks/ui gettextvalue :arch))
                                    status (not (= "Not Subscribed" (tasks/ui gettextvalue :certificate-status)))]
                                 (if (and (not (or check-arch arch-all)) status)
                                   (do
                                     (tasks/ui selecttab :my-subscriptions)
                                     (tasks/skip-dropdown :my-subscriptions-view sub-name)
                                     (verify (substring? (str "Covers architecture " sub-arch " but the system is " machine-arch)
                                                         (tasks/ui gettextvalue :status-details)))
                                     (tasks/ui selecttab :my-installed-products))))))))

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
  (allsearch)
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
  (tasks/search :do-not-overlap? false)
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
  (run-command "subscription-manager unregister")
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

(defn ^{DataProvider {:name "unlimited-pools"}}
  get_unlimited_pools [_ & {:keys [debug]
                               :or {debug false}}]
  (run-command "subscription-manager unregister")
  (tasks/restart-app)
  (tasks/register-with-creds)
  (let [all (ctasks/list-available true)
        unlimited? (fn [p] (< (:quantity p) 0))
        all-unlimited (filter unlimited? all)
        itemize (fn [p] (vector (:productName p) (:contractNumber p)))
        pools (into [] (map itemize all-unlimited))]
    (allsearch)
    (if-not debug
      (to-array-2d pools)
      pools)))

  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=683550
  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=691784
  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=691788
  ;; TODO: https://bugzilla.redhat.com/show_bug.cgi?id=727631

(gen-class-testng)
