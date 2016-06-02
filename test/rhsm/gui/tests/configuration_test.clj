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

;; ;; initialization of testing environment
(rhsm.runtestng/before-suite true)

;; testing of our testware
(deftest basic-properties-test
  (testing "Our java properties are set properly"
    (testing "- configuration of owner related variables"
      (is (contains?  #{["snowwhite" "Snow White"]
                        ["admin" "Admin Owner"]
                        } (->> ["sm.client1.org" "sm.client1.org.displayName"]
                             (map #(System/getProperty %))
                             ))
       )
      )
    )
  )

(deftest deploy-candlepin-data-test
  (testing "use proper combination of username and org"
    (let [username (:username @c/config)
          owner-key (:owner-key @c/config)
          ]
      (is (contains?  #{["testuser1" "Snow White"]
                        ["testuser2" "Admin Owner"]
                        } [username owner-key] ))
      )
    )
  )
