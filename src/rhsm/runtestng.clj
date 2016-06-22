(ns rhsm.runtestng
  (:use [clojure.pprint])
  (:require [clojure.tools.logging :as log]
            [clojure.xml :as xml]
            [clojure.pprint :only pprint]
            [clojure.tools.cli :as cli]
            [immuconf.config :as cfg]
            [rhsm.gui.tasks.test-config :as config]
            [rhsm.gui.tasks.tasks :as tasks]
            [mount.core :as mount])
  (:import [org.testng.xml Parser XmlSuite]
           [org.testng TestNG]
           (java.io FileNotFoundException))
  (:gen-class))

(defn get-test-names
  [suite]
  (map (fn [y] (:name (get y :attrs)))
       (filter (fn [x] (= :test (get x :tag)))
               (:content (xml/parse suite)))))

(def usage
  "This is a simplified cli utility for launching them rhsm-qe testng suite.

Usage:   lein run [OPTIONS] [TEST-NAMES..] SUITE.XML
Example: lein run 'GUI: REGISTRATION' 'GUI: FACTS' suites/sm-gui-testng-suite.xml")

;; TODO:
;; 1. Figure out why we are not only running a single test group of a test suite (specified on command line)
;; 2. Add testng listeners
(defn test-main
  [& args]
  (let [[options args banner]
        (cli/cli args
                 usage
                 ["--list-testnames"
                  "Lists the available test names in the given suite.xml"
                  :default false
                  :flag true]
                 ["-i" "--info"
                  "Show help info"
                  :default false
                  :flag true])]
    (when (:help options)
      (println banner)
      (System/exit 0))
    (let [suites (filter (fn [x] (re-matches #".*\.xml" x)) args)
          psuite (if-not (empty? suites)
                   (.parse (new Parser (first suites)))
                   nil)
          tests (filter (fn [x] (nil? (re-matches #".*\.xml" x))) args)
          testng (new TestNG)]
      (cond
        (:list-testnames options)
        (doseq [s suites]
          (println (str "\n" s ":"))
          (pprint (get-test-names s)))
        :else (do
                (when psuite (.setXmlSuites testng psuite))
                (if-not (empty? tests) (.setTestNames testng tests))
                (when psuite (.run testng)))))))


(defn -main
  [& args]
  (TestNG/main (into-array String args)))

(defn get-config
  "Retrieves the edn file configuration as a map

  Can specify a path the the user's development edn file which will be merged with the
  resources/dev.edn file.

  dev-file-path: path to the user's dev.edn file"
  ([^String dev-file-path]
   (try
     (cfg/load "resources/dev.edn" dev-file-path)
     (catch Exception e
       (log/warn "For development testing, please create a ~/.rhsm-qe/dev.edn file"))))
  ([]
   (let [home (System/getProperty "user.home")
         user-edn (format "%s/.rhsm-qe/dev.edn" home)]
     (get-config user-edn))))


(def dev-config (get-config))

(defn before-suite
  ([setup]
   (when setup
     (let [cliscript (rhsm.base.SubscriptionManagerCLITestScript.)]
       (.setupBeforeSuite cliscript))
     (config/init)
     (tasks/connect)
     (use 'gnome.ldtp)))
  ([]
   (before-suite (:runtestng-beforesuite dev-config))))

(defn run-imports
  []
  (use '[clojure.repl])
  (use '[clojure.pprint])
  (use '[slingshot.slingshot :only (try+ throw+)])
  (require '[clojure.tools.logging :as log])

  (use :reload-all '[rhsm.gui.tasks.tools])
  (require :reload-all '[rhsm.gui.tasks.candlepin-tasks :as ctasks]
           '[rhsm.gui.tasks.rest :as rest]
           '[rhsm.gui.tests.base :as base]
           '[rhsm.gui.tests.subscribe_tests :as stest]
           '[rhsm.gui.tests.register_tests :as rtest]
           '[rhsm.gui.tests.proxy_tests :as ptest]
           '[rhsm.gui.tests.rhn_interop_tests :as ritest]
           '[rhsm.gui.tests.autosubscribe_tests :as atest]
           '[rhsm.gui.tests.firstboot_tests :as fbtest]
           '[rhsm.gui.tests.firstboot_proxy_tests :as fptest]
           '[rhsm.gui.tests.facts_tests :as ftest]
           '[rhsm.gui.tests.import_tests :as itest]
           '[rhsm.gui.tests.system_tests :as systest]
           '[rhsm.gui.tests.stacking_tests :as stktest]
           '[rhsm.gui.tests.repo_tests :as reptest]
           '[rhsm.gui.tests.product_status_tests :as pstest]
           '[rhsm.gui.tests.subscription_status_tests :as substattest]
           '[rhsm.gui.tests.search_status_tests :as sstattest]
           '[rhsm.gui.tasks.test-config :as config]
           '[rhsm.gui.tasks.tasks :as tasks]
           '[rhsm.gui.tasks.tools :as tools]))

(when (:runtestng-setup dev-config)
  (run-imports)
  (before-suite)
  (log/info "INITIALIZATION COMPLETE!!"))
