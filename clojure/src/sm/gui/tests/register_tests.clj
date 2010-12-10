(ns sm.gui.tests.register-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)]
	[sm.gui.ui :only (action)])
  (:require [sm.gui.tasks :as tasks])
  (:import [org.testng.annotations Test]))

(defn ^{Test {:groups ["registration"]}}
  simple_register [_]
  (tasks/register (config :username) (config :password))
  (assert (action exists? :unregister-system)))

(defn ^{Test {:groups [ "registration"]}}
  register_bad_credentials [_]
  (binding [tasks/err-handler (fn [e] {:handled (= e :invalid-credentials)
				   :recovery :cancel})]
    (tasks/register "sdf" "sdf")))

(defn ^{Test {:groups ["registration"]
	       :dependsOnTests ["simple_register"]}}
  unregister [_]
  (tasks/unregister))

(gen-class-testng)

