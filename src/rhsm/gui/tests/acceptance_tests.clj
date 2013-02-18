(ns rhsm.gui.tests.acceptance-tests
  (:use [test-clj.testng :only [gen-class-testng
                                data-driven]]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp)
  (:require [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tests.register-tests :as rtest]
            [rhsm.gui.tests.subscribe-tests :as stest]
            [rhsm.gui.tests.autosubscribe-tests :as atest])
  (:import [org.testng.annotations
            Test
            BeforeClass
            BeforeGroups
            AfterClass
            AfterGroups
            DataProvider]))

(defn ^{Test {:groups ["acceptance"]}}
  register [_]
  (rtest/simple_register nil
                         (@config :username)
                         (@config :password)
                         nil))

(defn ^{Test {:groups ["acceptance"]}}
  unregister [_]
  (rtest/unregister nil))

(defn- check-register []
  (if (tasks/ui showing? :register-system)
    (register nil)))

(defn ^{Test {:groups ["acceptance"]}}
  subscribe_all [_]
  (check-register)
  (stest/subscribe_all))

(defn ^{Test {:groups ["acceptance"]}}
  unsubscribe_all [_]
  (check-register)
  (stest/unsubscribe_all))

(defn ^{BeforeGroups {:groups ["acceptance"]
                      :value ["acceptance_autosubscribe"]}}
  before_autosubscribe [_]
  (atest/setup nil)
  (.configureProductCertDirForAllProductsSubscribableByOneCommonServiceLevel @atest/complytests))

(defn ^{Test {:groups ["acceptance"
                       "acceptance_autosubscribe"]}}
  simple_autosubscribe [_]
  (atest/simple_autosubscribe nil))

(defn ^{AfterGroups {:groups ["acceptance"]
                     :value ["acceptance_autosubscribe"]}}
  after_autosubscribe [_]
  (.configureProductCertDirAfterClass @atest/complytests))

(defn ^{AfterClass {:groups ["cleanup"]}}
  cleanup [_]
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (tasks/restart-app))

(gen-class-testng)
