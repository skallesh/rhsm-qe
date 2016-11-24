(ns rhsm.gui.tests.firstboot-test
  (:use gnome.ldtp
        rhsm.gui.tasks.tools
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.firstboot_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tt]
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
    (is (thrown? SkipException (tests/skip-by-rhel-release {:family "RHEL5" :variant "Server" :version "5.7"})))
    (let [[rhel-version-major rhel-version-minor] (tests/skip-by-rhel-release {:family "RHEL6" :variant "Server" :version "6.8"})]
      (is (= "6" rhel-version-major))
      (is (= "8" rhel-version-minor)))

    (let [[rhel-version-major rhel-version-minor] (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.0"})]
      (is (= "7" rhel-version-major))
      (is (= "0" rhel-version-minor)))

    (let [[rhel-version-major rhel-version-minor] (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.1"})]
      (is (= "7" rhel-version-major))
      (is (= "1" rhel-version-minor)))

    (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.1"})
    (is (thrown? SkipException (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.2"})))
    (is (thrown? SkipException (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.3"})))
    (is (thrown? SkipException (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.4"})))
    (is (thrown? SkipException (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.5"})))
    (is (thrown? SkipException (tests/skip-by-rhel-release {:family "RHEL7" :variant "Server" :version "7.6"})))))
