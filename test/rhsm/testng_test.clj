(ns rhsm.testng-test
  (:require  [clojure.test :refer :all])
  (:import [org.testng TestNG]))

;;
;;
;; this test is run this way:
;;
;;       lein test rhsm.testng-test
;; or
;;
;;     lein test :only rhsm.testng-test/tier1-all-suites
;;
;; this test is to run the whole testng machinery with our tests
;;
;
(deftest tier1-all-suites
  (TestNG/main (into-array String ["suites/sm-cli-tier1-testng-suite.xml" "suites/sm-gui-tier1-testng-suite.xml"])))

(deftest tier1-cli-suite
  (TestNG/main (into-array String ["suites/sm-cli-tier1-testng-suite.xml"])))

(deftest tier1-gui-suite
  (TestNG/main (into-array String ["suites/sm-gui-tier1-testng-suite.xml"])))

(deftest tier1-cli-one-suite
  (TestNG/main (into-array String ["suites/sm-cli-one-testng-suite.xml"])))

(deftest tier1-api-suite
  (TestNG/main (into-array String ["suites/sm-api-tier1-testng-suite.xml"])))

(deftest tier2-api-suite
  (TestNG/main (into-array String ["suites/sm-api-tier2-testng-suite.xml"])))
