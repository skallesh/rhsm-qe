(ns rhsm.gui.tests.configuration-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [vinyasa.reimport :refer [reimport]]
             [clojure.string :as s]
             )
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [rhsm.cli.tasks CandlepinTasks]
           [rhsm.base SubscriptionManagerBaseTestScript]
           [rhsm.base SubscriptionManagerCLITestScript]
           )
  )

;; (reimport '[rhsm.cli.tasks CandlepinTasks])
;; ;; initialization of testing environment
;; (rhsm.runtestng/before-suite true)

;; ;; ;; testing of our tests

;; (deftest basic-configuration-test
;;   (testing "the first user's credentials must be set"
;;     (is (not (s/blank? (:username @c/config))))
;;     (is (not (s/blank? (:password @c/config))))
;;     (is (not (s/blank? (:owner-key @c/config)))))
;;   (testing "the second user's credentials must be set "
;;     (is (not (s/blank? (:username1 @c/config))))
;;     (is (not (s/blank? (:password1 @c/config)))))
;;   (comment
;;     (testing "configuration of database connection"
;;       (is (not (s/blank? (SubscriptionManagerBaseTestScript/sm_dbHostname)))))
;;     )
;;   (comment
;;     (testing "connection to db exists?"
;;       (is (not (nil?  (SubscriptionManagerCLITestScript/dbConnection))))))
;;   )

;; (deftest deploy-candlepin-data-test
;;   (testing "use proper combination of username and org"
;;     (let [username (:username @c/config)
;;           owner-key (:owner-key @c/config)
;;           ]
;;       (is (contains?  #{["testuser1" "Snow White"]
;;                         ["testuser2" "Admin Owner"]
;;                         } [username owner-key] ))
;;       )
;;     )
;;   )

;; (deftest data-migration-test
;;   (testing "git repo should exist"
;;     (let [repo (System/getProperty "sm.rhn.definitionsGitRepository") ]
;;       (is (or (s/blank? repo)
;;               (re-find #"\tHEAD" (->> repo (format "git ls-remote %s") tt/run-command  :stdout))
;;               )
;;           )
;;       )
;;     )
;;   )
