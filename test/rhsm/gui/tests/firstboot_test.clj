(ns rhsm.gui.tests.firstboot-test
  (:use gnome.ldtp
        rhsm.gui.tasks.tools
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.firstboot_tests :as ftests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [rhsm.gui.tests.base :as base])
  (:import   org.testng.SkipException))

;; ;; initialization of our testware
(use-fixtures :once (fn [f] (base/startup nil)(f)))

(deftest skip-by-rhel-release-test
  (testing "A method raises SkipException is some cases."
    (is (thrown? SkipException (ftests/skip-by-rhel-release {:family "RHEL5" :variant "Server" :version "5.7"})))
    (ftests/skip-by-rhel-release {:family "RHEL6" :variant "Server" :version "6.8"})
    (ftests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.0"})
    (ftests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.1"})
    (is (thrown? SkipException (ftests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.2"})))
    (is (thrown? SkipException (ftests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.3"})))
    (is (thrown? SkipException (ftests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.4"})))
    (is (thrown? SkipException (ftests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.5"})))
    (is (thrown? SkipException (ftests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.6"})))))
