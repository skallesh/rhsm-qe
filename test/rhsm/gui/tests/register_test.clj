(ns rhsm.gui.tests.register-test
  (:use gnome.ldtp
        rhsm.gui.tasks.tools
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.register_tests :as rtests]
             [rhsm.gui.tests.import_tests :as itests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.candlepin-tasks :as ct]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [rhsm.gui.tests.base :as base]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f] (base/startup nil)
                      (itests/create_certs nil)
                      (rtests/setup nil)
                      (f)))


(deftest simple-register-test
  (testing "Simple Register Tests"
    (tasks/restart-app)
    (rtests/simple_register nil (:username @c/config) (:password @c/config)
                            (ct/get-owner-display-name (:username @c/config) (:password @c/config) (:owner-key @c/config)))))

(deftest check_default_subscription_url-test
  (rtests/check_default_subscription_url nil))

(deftest unregister_traceback-test
  (rtests/unregister_traceback nil))
