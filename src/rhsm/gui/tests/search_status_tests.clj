(ns rhsm.gui.tests.search_status_tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.string :only (split
                               trim
                               blank?
                               trim-newline)]
        [slingshot.slingshot :only [throw+
                                    try+]]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [clojure.tools.logging :as log]
            [rhsm.gui.tests.base :as base]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            BeforeClass
            BeforeGroups
            AfterGroups
            Test
            DataProvider
            AfterClass]
            org.testng.SkipException))

(def servicelist (atom {}))
(def productlist (atom {}))
(def subs-contractlist (atom {}))
(def virt? (atom nil)) ;; Used to hold the virt value of system
(def ns-log "rhsm.gui.tests.search_status_tests")

(defn allsearch
  ([filter]
     (tasks/search :match-system? false
                   :do-not-overlap? false
                   :contain-text filter))
  ([] (allsearch nil)))

(defn build-subscription-map
  "Builds the product map and updates the productlist atom"
  []
  (reset! productlist (ctasks/build-product-map :all? true))
  @productlist)

(defn ^{BeforeClass {:groups ["setup"]}}
  clear_env [_]
  (try
    (if (= "RHEL7" (get-release)) (base/startup nil))
    (tasks/kill-app)
    (tasks/restart-app)
    (tasks/register-with-creds)
    (reset! servicelist (ctasks/build-service-map :all? true))
    (reset! subs-contractlist (ctasks/build-subscription-product-map :all? true))
    (catch Exception e
      (reset! (skip-groups :search_status) true)
      (throw e))))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  restart_env [_]
  (assert-valid-testing-arch)
  (tasks/restart-app :unregister? true))

(defn ^{Test {:groups ["search_status"
                       "tier2"
                       "blockedByBug-707041"]}}
  date_picker_traceback
  "Asserts that the date chooser does not throw a traceback."
  [_]
  (try
    (if-not (bool (tasks/ui guiexist :main-window)) (tasks/restart-app))
    (try+ (tasks/register-with-creds :re-register? false)
          (catch [:type :already-registered] _))
    (tasks/ui selecttab :all-available-subscriptions)
    (let [output (get-logging @clientcmd
                                    "/var/log/ldtpd/ldtpd.log"
                                    "date_picker_traceback"
                                    "Traceback"
                                    (tasks/ui click :calendar)
                                    (verify
                                     (bool (tasks/ui waittillwindowexist :date-selection-dialog 10)))
                                    (tasks/ui click :today))]
      (verify (clojure.string/blank? output)))
    (finally (if (bool (tasks/ui guiexist :date-selection-dialog))
               (tasks/ui closewindow :date-selection-dialog)))))

(defn ^{Test {:groups ["search_status"
                       "tier1"
                       "acceptance"
                       "blockedByBug-818282"]}}
  check_ordered_contract_options
  "Checks if contracts in contract selection dialog are ordered based on host type"
  [_]
  (tasks/restart-app :reregister? true)
  (tasks/ui selecttab :all-available-subscriptions)
  (tasks/search)
  (let
      [sub-map (zipmap (range 0 (tasks/ui getrowcount :all-subscriptions-view))
                       (tasks/get-table-elements :all-subscriptions-view 0 :skip-dropdowns? false))
       both? (fn [pair] (=  "Both" (try
                                    (tasks/ui getcellvalue :all-subscriptions-view (key pair) 1)
                                    (catch Exception e))))
       row-sub-map (into {} (filter both? sub-map))
       cli-out (:stdout (run-command "subscription-manager facts --list | grep virt.is_guest"))
       virt? (= "true" (.toLowerCase (trim (last (split (trim-newline cli-out) #":")))))]
    (if-not (empty? row-sub-map)
      (do
        (doseq [map-entry row-sub-map]
          (try
            (tasks/ui selectrowindex :all-subscriptions-view (key map-entry))
            (tasks/ui click :attach)
            (tasks/ui waittillguiexist :contract-selection-dialog)
            (let [type-list (tasks/get-table-elements :contract-selection-table 1)]
              (if virt?
                (verify (not (sorted? type-list)))
                  (verify (sorted? type-list))))
            (finally
             (if (bool (tasks/ui guiexist :contract-selection-dialog))
               (tasks/ui click :cancel-contract-selection)))))))))

(defn ^{Test {:groups ["search_status"
                       "tier3"
                       "check_subscription_type_all_available"]
              :dataProvider "all-subscriptions"}}
  check_subscription_type_all_subscriptions
  "Checks for subscription type in all available subscriptions"
  [_ product]
  (tasks/ui selecttab :all-available-subscriptions)
  (tasks/skip-dropdown :all-subscriptions-view product)
  (verify (not (blank? (tasks/ui gettextvalue :all-available-subscription-type)))))

(defn ^{AfterGroups {:groups ["search_status"
                              "tier3"]
                     :value ["check_subscription_type_all_available"]
                     :alwaysRun true}}
  after_check_subscription_type_all_available
  [_]
  (tasks/unsubscribe_all)
  (tasks/unregister))

(defn ^{Test {:groups ["search_status"
                       "acceptance"
                       "tier1"]}}
  check_physical_only_pools
  "Identifies physical only pools from JSON and checks
   whether it throws appropriate error message"
  [_]
  (tasks/unsubscribe_all)
  (tasks/ui selecttab :my-installed-products)
  (if (tasks/ui showing? :register-system)
    (tasks/register-with-creds))
  (try
    (let [cli-cmd (:stdout
                   (run-command "subscription-manager facts --list | grep \"virt.is_guest\""))
          virt (trim (re-find #" .*" cli-cmd))
          prod-attr-map (ctasks/build-subscription-attr-type-map :all? true)
          phy-only? (fn [v] (if (not (nil? (re-find #"physical_only" v)))
                             true false))
          filter-product (fn [m] (if (not (empty? (filter phy-only? (val m)))) (key m)))
          subscriptions (into [] (filter string? (map filter-product prod-attr-map)))
          subscribe (fn [sub] (try
                          (tasks/subscribe sub)
                          (catch Exception e
                            (let [result (substring? "Error getting subscription:" (.getMessage e))]
                              result))))]
      (reset! virt? virt)
      (if (= @virt? "False")
        (do
          (tasks/write-facts "{\"virt.is_guest\": \"True\"}")
          (run-command "subscription-manager facts --update")))
      (tasks/search :match-system? false :do-not-overlap? false)
      (verify (not (some false? (map subscribe subscriptions)))))
    (finally
      (if (= @virt? "False")
        (do
          (tasks/write-facts (str "{\"virt.is_guest\":" \space "\"" @virt? "\"" "}"))
          (run-command "subscription-manager facts --update")))
      (tasks/unsubscribe_all))))

(defn ^{Test {:groups ["search_status"
                       "tier2"
                       "blockedByBug-801434"
                       "blockedByBug-707041"]}}
  check_date_chooser_traceback
  "Checks if there is any traceback in the logs after clicking on calendar icon"
  [_]
  (try
    (let [ldtpd-log "/var/log/ldtpd/ldtpd.log"
          output (get-logging @clientcmd
                              ldtpd-log
                              "date-chooser-tracebacks"
                              nil
                              (do
                                (tasks/ui selecttab :all-available-subscriptions)
                                (tasks/ui click :calendar)
                                (tasks/checkforerror)))]
      (verify (not (substring? "Traceback" output))))
    (finally (tasks/restart-app))))

(defn ^{Test {:groups ["search_status"
                       "tier2"
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
    (finally (tasks/restart-app))))

(defn ^{Test {:groups ["search_status"
                       "tier2"
                       "blockedByBug-688454"
                       "blockedByBug-704408"]}}
  check_blank_date_search
  "Tests the behavior when the date search is blank and you try to search."
  [_]
  (try
    (if-not (bool (tasks/ui guiexist :main-window))
      (tasks/start-app)
      (if (tasks/ui showing? :register-system)
        (tasks/register-with-creds)))
    (tasks/ui selecttab :all-available-subscriptions)
    (tasks/ui settextvalue :date-entry "")
    (let [error (try+ (tasks/ui click :search)
                      (tasks/checkforerror)
                      (catch Object e (:type e)))]
      (verify (= :date-error error)))
    (verify (= "" (tasks/ui gettextvalue :date-entry)))
    (finally (tasks/restart-app))))

(defn ^{Test {:groups ["search_status"
                       "tier3"
                       "blockedByBug-858773"]
              :dataProvider "installed-products"}}
  filter_by_product
  "Tests that the product filter works when searching."
  [_ product]
  (tasks/ui selecttab :my-installed-products)
  (if-not (bool (tasks/ui guiexist :main-window))
    (tasks/start-app)
    (if (tasks/ui showing? :register-system)
      (tasks/register-with-creds)))
  (if (tasks/assert-valid-product-arch product)
    (do
      (try
        (if (nil? (@productlist product))
          (throw (SkipException.
            (str "Product: '" product "' does not valid subscription"))))
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
            (verify (not-nil? (some #{e} seen)))))
    (finally
      (if (bool (tasks/ui guiexist :filter-dialog))
        (tasks/ui click :close-filters))
      (tasks/search))))))

(defn ^{Test {:groups ["search_status"
                       "tier2"
                       "blockedByBug-817901"]}}
  check_no_search_results_message
  "Tests the message when the search returns no results."
  [_]
  (tasks/restart-app :reregister? true)
  (tasks/ui selecttab :all-available-subscriptions)
  (tasks/search :contain-text "DOESNOTEXIST")
  (let [label "No subscriptions match current filters."]
    (if (= "RHEL5" (get-release))
      (do
        (verify (tasks/ui showing? :all-available-subscriptions label))
        (tasks/search)
        (verify (tasks/ui showing? :all-subscriptions-view)))
      (do
        (verify (tasks/ui showing? :no-subscriptions-label))
        (verify (= label (tasks/ui gettextvalue :no-subscriptions-label)))
        (tasks/search)
        (verify (not (tasks/ui showing? :no-subscriptions-label)))))))

(defn ^{Test {:groups ["search_status"
                       "tier2"
                       "blockedByBug-817901"]}}
  check_please_search_message
  "Tests for the initial message before you search."
  [_]
  (tasks/restart-app :reregister? true)
  (tasks/ui selecttab :all-available-subscriptions)
  (let [label "Press Update to search for subscriptions."]
    (if (= "RHEL5" (get-release))
      (do
        (verify (tasks/ui showing? :all-available-subscriptions label))
        (tasks/search)
        (verify (tasks/ui showing? :all-subscriptions-view)))
      (do
        (verify (tasks/ui showing? :no-subscriptions-label))
        (verify (= label (tasks/ui gettextvalue :no-subscriptions-label)))
        (tasks/search)
        (verify (not (tasks/ui showing? :no-subscriptions-label)))))))

(defn ^{Test {:groups ["search_status"
                       "tier3"
                       "blockedByBug-911386"]
              :dataProvider "all-subscriptions"}}
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

(defn ^{Test {:group ["search_status"
                      "tier2"
                      "blockedByBug-865193"]
              :dataProvider "all-subscriptions"
              :priority (int 99)}}
  check_provides_products
  "Checks if provide products is populated in all available subscriptions view"
  [_ subscription]
  (tasks/skip-dropdown :all-subscriptions-view subscription)
  (verify ( = (sort (get @subs-contractlist subscription))
              (sort (tasks/get-table-elements :all-available-bundled-products 0)))))

(defn ^{Test {:group ["search_status"
                      "tier3"]
              :dataProvider "all-subscriptions"}}
  check_subscription_selected_after_update
  "Checks if subscription remains selected after update is clicked
   case 1: If subscription is stackable, it no longer remains selected
   case 2: If subscription is non-stakable, it remanins selected"
  [_ subscription]
  (tasks/skip-dropdown :all-subscriptions-view subscription)
  (let [sub-type (tasks/ui gettextvalue :all-available-subscription-type)
        get-sub-name (fn [] (tasks/ui gettextvalue :all-available-subscription))]
    (tasks/ui click :search)
    (tasks/checkforerror)
    (if (= sub-type "Stackable")
      (verify (empty? (get-sub-name)))
      (verify (= (get-sub-name) subscription)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;      DATA PROVIDERS      ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^{DataProvider {:name "installed-products"}}
  get_installed_products [_ & {:keys [debug]
                               :or {debug false}}]
  (log/info (str "======= Starting DataProvider: " ns-log "get_installed_products()"))
  (if-not (assert-skip :search_status)
    (do
      (tasks/restart-app :reregister? true)
      (let [prods (into [] (map vector (tasks/get-table-elements
                                        :installed-view
                                        0)))]
        (build-subscription-map)
        (tasks/search)
        (if-not debug
          (to-array-2d prods)
          prods)))
    (to-array-2d [])))

(defn ^{DataProvider {:name "all-subscriptions"}}
  get_subscriptions [_ & {:keys [debug]
                          :or {debug false}}]
  (log/info (str "======= Starting DataProvider: " ns-log "get_subscriptions()"))
  (if-not (assert-skip :search_status)
    (do
      (tasks/restart-app)
      (clear_env nil)
      (build-subscription-map)
      (allsearch)
      (let [subs (into [] (map vector (tasks/get-table-elements
                                       :all-subscriptions-view
                                       0
                                       :skip-dropdowns? true)))]
        (if-not debug
          (to-array-2d subs)
          subs)))
    (to-array-2d [])))

(gen-class-testng)
