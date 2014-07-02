(ns rhsm.gui.tests.product_status_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           cli-tasks
                                           candlepin-runner)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.string :only (blank?
                               split-lines
                               split
                               trim)]
        rhsm.gui.tasks.tools
        clojure.pprint
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.base :as base]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
            rhsm.gui.tasks.ui
            [clojure.tools.logging :as log])
  (:import [org.testng.annotations
            BeforeClass
            AfterClass
            Test
            DataProvider]
           org.testng.SkipException))

(def ns-log "rhsm.gui.tests.product_status_tests")
(def productstatus (atom nil))

(defn ^{BeforeClass {:groups ["setup"]}}
  before_check_product_status [_]
  (try
    (if (= "RHEL7" (get-release)) (base/startup nil))
    (tasks/restart-app :reregister? true)
    (let [output (:stdout
                  (run-command "subscription-manager attach --auto"))
          not-blank? (fn [s] (not (blank? s)))
    	    raw-cli-data (filter not-blank? (drop 1 (split-lines output)))
          status (fn [s] (re-find #"Status.*" s))
          products (fn [s] (re-find #"Product Name.*" s))
          grab-value (fn [item] (trim (last (split item #":"))))
          not-nil? (fn [s] (not (nil? s)))
          list-status (into [] (map grab-value (filter not-nil? (map status raw-cli-data))))
          list-products (into [] (map grab-value (filter not-nil? (map products raw-cli-data))))
          cli-data (zipmap list-products list-status)]
    (reset! productstatus cli-data))
    (catch Exception e
      (reset! (skip-groups :product_status) true)
      (throw e))))

(defn ^{Test {:groups ["product_status"
                       "tier3"
                       "blockedByBug-964332"]
              :dataProvider "installed-products"}}
  check_product_status
  "Asserts that all product statuses match the known statuses in the CLI."
  [_ product row]
  (let [gui-value (tasks/ui getcellvalue :installed-view row 2)
        cli-value (get @productstatus product)]
    (verify (= gui-value cli-value))))

(defn ^{Test {:groups ["product_status"
                       "tier3"]
              :dependsOnMethods ["check_product_status"]
              :dataProvider "installed-products"}}
  check_product_status_unsubscribe
  "Checks product status is correct after unsubscribing."
  [_ product row]
  (let [gui-status (tasks/ui getcellvalue :installed-view row 2)]
    (verify (= gui-status "Not Subscribed"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DATA PROVIDERS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^{DataProvider {:name "installed-products"}}
  get_installed_products [_ & {:keys [debug]
                               :or {debug false}}]
  (log/info (str "======= Starting DataProvider: " ns-log "/get_installed_products()"))
  (if-not (assert-skip :facts)
    (do
      (let [prods (tasks/get-table-elements :installed-view 0)
            indexes (range 0 (tasks/ui getrowcount :installed-view))
            prodlist (map (fn [item index] [item index]) prods indexes)]
        (if-not debug
          (to-array-2d (vec prodlist))
          prodlist)))
    (to-array-2d [])))

(gen-class-testng)
