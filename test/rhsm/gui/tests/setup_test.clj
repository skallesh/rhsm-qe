(ns rhsm.gui.tests.setup-test
  (:require  [clojure.test :refer :all])
  (:import [org.testng TestNG]))

;; this test is run this way:
;; lein test rhsm.gui.tests.setup-test
;; this test is to run the whole testng machinery with our tests
(deftest setup-consistency-test
  (TestNG/main (into-array String ["suites/sm-gui-tier2-testng-suite.xml"])))
