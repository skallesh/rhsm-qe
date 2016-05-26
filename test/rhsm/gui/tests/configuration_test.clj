(ns rhsm.gui.tests.configuration-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [clojure.string :as s]
             )
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [rhsm.cli.tasks CandlepinTasks]
           [rhsm.base SubscriptionManagerBaseTestScript]
           [rhsm.base SubscriptionManagerCLITestScript]
           )
  )

;; ;; ; initialization of testing environment
;; (rhsm.runtestng/before-suite true)

;; ;; testing of our tests

;; (deftest basic-configuration-test
;;   (testing "the first user's credentials must be set"
;;     (is (not (s/blank? (:username @c/config))))
;;     (is (not (s/blank? (:password @c/config))))
;;     (is (not (s/blank? (:owner-key @c/config)))))
;;   (testing "the second user's credentials must be set "
;;     (is (not (s/blank? (:username1 #spy/d @c/config))))
;;     (is (not (s/blank? (:password1 @c/config)))))
;;   (comment
;;     (testing "configuration of database connection"
;;       (is (not (s/blank? (SubscriptionManagerBaseTestScript/sm_dbHostname)))))
;;     )
;;   (comment
;;     (testing "connection to db exists?"
;;       (is (not (nil?  (SubscriptionManagerCLITestScript/dbConnection))))))
;;   )

;; (deftest data-migration-test
;;   (testing "git repo should exist"
;;     (let [repo (System/getProperty "sm.rhn.definitionsGitRepository") ]
;;       (is (re-find #"\tHEAD" (->> repo (format "git ls-remote %s") tt/run-command  :stdout)))
;;       )
;;     )
;;   )
