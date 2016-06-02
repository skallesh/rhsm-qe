(ns rhsm.gui.tests.register-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [vinyasa.reimport :refer [reimport]]
             )
  (:import [com.redhat.qe.tools RemoteFileTasks]
           [rhsm.cli.tasks CandlepinTasks]
           [rhsm.cli.tasks SubscriptionManagerTasks]
           [com.redhat.qe.tools SSHCommandRunner]
           [rhsm.base SubscriptionManagerBaseTestScript]
           [rhsm.base SubscriptionManagerCLITestScript]
           )
  )
(deftest simple-register-test
  (testing "Simple Register Tests"
    (t/restart-app)
    (rtests/setup nil)
    (rtests/simple_register nil (:username @c/config) (:password @c/config) (:owner-key @c/config))
    )
  )
