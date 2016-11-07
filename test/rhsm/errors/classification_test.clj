(ns rhsm.errors.classification-test
  (:use [slingshot.slingshot :only (try+ throw+)])
  (:require [rhsm.errors.classification :as sut]
            [clojure.test :refer [deftest is]])
  (:import org.testng.SkipException))

(deftest skip-exception-failure-level-test
  (is (= :skip-exception (sut/failure-level (new SkipException "some exception")))))

(deftest exception-test
  (is (= :unknown-exception (sut/failure-level (new Exception "some exception in general manner")))))

(deftest unknown-string-test
  (is (= :unknown (sut/failure-level "unknown string"))))

(deftest unknown-integer-test
  (is (= :unknown (sut/failure-level (Integer. 10)))))

(deftest network-candlepin-connection-error-test
  (let [e (try+ (throw+  {:type :network-error
                          :msg "Unable to reach the server at jsefler-candlepin6.usersys.redhat.com:8443/candlepin"
                          :log-warning :some-object-reference
                          :cancel :some-object-reference})
                (catch Object e
                  e))]
    (is (= :network-error (sut/failure-level e)))))
