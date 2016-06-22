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


(deftest check_traceback_unregister-test
  (tasks/restart-app)
  (rtests/check_traceback_unregister nil))

(deftest simple-register-test
  (testing "Simple Register Tests"
    (tasks/restart-app)
    (rtests/simple_register nil (:username @c/config) (:password @c/config)
                            (ct/get-owner-display-name (:username @c/config) (:password @c/config) (:owner-key @c/config)))))

(deftest auto_to_register_button-test
  (tasks/restart-app)
  (rtests/check_auto_to_register_button nil))

(deftest register_multi_click-test
  (tasks/restart-app)
  (rtests/register_multi_click nil))

(deftest unregister_traceback-test
  (tasks/restart-app)
  (rtests/unregister_traceback nil))
