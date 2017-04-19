(ns rhsm.gui.tasks.candlepin-tasks-test
  (:require  [clojure.test :refer :all]
                [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tests.base :as base]
             [rhsm.gui.tasks.candlepin-tasks :as ctasks]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :refer (config)]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
             [mount.core :as mount])
  (:import java.time.LocalDateTime))

(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

;; initialization of our testware
(deftest register-with-creds-test
  (is (contains? #{"Snow White" "Admin Owner"} (ctasks/get-owner-display-name (:username @config) (:password @config) (:owner-key @config)))))

(deftest create-activation-key-test
  (ctasks/create-activation-key (@config :username)
                                (@config :password)
                                (@config :owner-key)
                                (->> (System/currentTimeMillis)
                                     (format "rhsm-gui-tests-activation-key-%d"))))
