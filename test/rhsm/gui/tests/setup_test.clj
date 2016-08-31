(ns rhsm.gui.tests.setup-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tests.base :as base]
             [rhsm.gui.tests.import_tests :as itests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.candlepin-tasks :as ct]
             [rhsm.gui.tasks.tools :as tt])
  (:import [org.testng TestNG]))

;; this test is run this way:
;; lein test rhsm.gui.tests.setup-test
;; this test is to run the whole testng machinery with our tests
(deftest setup-consistency-test
  (TestNG/main (into-array String ["suites/sm-gui-tier1-testng-suite.xml"])))
