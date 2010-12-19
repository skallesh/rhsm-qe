(ns sm.gui.tests.register-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [com.redhat.qe.handler :only (with-handlers handle-type recover-by)]
	 sm.gui.ldtp)
  (:require [sm.gui.tasks :as tasks])
  (:import [org.testng.annotations Test]))

(defn ^{Test {:groups ["registration"]}}
  simple_register [_]
  (tasks/register (@config :username) (@config :password))
  (verify (action exists? :unregister-system)))

(defn ^{Test {:groups [ "registration"]}}
  register_bad_credential0s [_]
  (with-handlers [(handle-type :invalid-credentials (recover-by :cancel))]
    (tasks/register "sdf" "sdf")))

(defn ^{Test {:groups [ "registration"]}}
  register_no_credentials [_]
  (with-handlers [(handle-type :no-credentials (recover-by :cancel))]
    (tasks/register "" "")))

(defn ^{Test {:groups ["registration"]
	       :dependsOnTests ["simple_register"]}}
  unregister [_]
  (tasks/unregister))

(gen-class-testng)

