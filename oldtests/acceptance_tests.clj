(ns rhsm.gui.tests.acceptance_tests
  (:use [test-clj.testng :only [gen-class-testng
                                data-driven]]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.register_tests :as rtest]
            [rhsm.gui.tests.subscribe_tests :as stest]
            [rhsm.gui.tests.autosubscribe_tests :as atest]
            [rhsm.gui.tests.facts_tests :as ftest])
  (:import [org.testng.annotations
            Test
            BeforeClass
            BeforeGroups
            AfterClass
            AfterGroups
            DataProvider]))

(defn ^{Test {:groups ["acceptance"]}}
  register
  "Simple register test using a known username and password."
  [_]
  (rtest/simple_register nil
                         (@config :username)
                         (@config :password)
                         nil))

(defn ^{Test {:groups ["acceptance"]}}
  unregister
  "Simple unregister test."
  [_]
  (rtest/unregister nil))

(defn- check-register []
  (if (tasks/ui showing? :register-system)
    (register nil)))

(defn ^{Test {:groups ["acceptance"]}}
  subscribe_all
  "Attemts to subscribe to all available subscriptions."
  [_]
  (check-register)
  (stest/subscribe_all))

(defn ^{Test {:groups ["acceptance"]}}
  unsubscribe_all
  "Attempts to unsubscribe from all subscribed subscriptions."
  [_]
  (check-register)
  (stest/unsubscribe_all))

(defn ^{BeforeGroups {:groups ["acceptance"]
                      :value ["acceptance_autosubscribe"]}}
  before_autosubscribe [_]
  (atest/setup nil)
  (.configureProductCertDirForAllProductsSubscribableByOneCommonServiceLevel @atest/complytests))

(defn ^{Test {:groups ["acceptance"
                       "acceptance_autosubscribe"
                       "blockedByBug-977851"]}}
  simple_autosubscribe
  "Attempts a simple autosubscibe and verifys results."
  [_]
  (atest/simple_autosubscribe nil))

(defn ^{AfterGroups {:groups ["acceptance"]
                     :value ["acceptance_autosubscribe"]}}
  after_autosubscribe [_]
  (.configureProductCertDirAfterClass @atest/complytests))

(defn ^{AfterClass {:groups ["cleanup"]}}
  cleanup [_]
  (run-command "subscription-manager unregister")
  (tasks/restart-app))

(defn ^{Test {:groups ["acceptance"]}}
  check_releases
  "Tests that all available releases are shown in the GUI"
  [_]
  (check-register)
  (let [certdir (tasks/conf-file-value "productCertDir")
        rhelcerts ["68" "69" "71" "72" "74" "76"]
        certlist (map #(str certdir "/" % ".pem") rhelcerts)
        certexist? (map #(= 0 (:exitcode
                               (run-command (str "test -f " %))))
                        certlist)]
    (verify (some true? certexist?)))
  (ftest/check_available_releases nil))

(gen-class-testng)
