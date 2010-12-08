(ns sm.gui.tests.register-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)])
  (:require [sm.gui.tasks :as tasks]))

(defn ^{:test {:groups ["registration"]}}
  simple_register [_]
  (tasks/register (config :username) (config :password)))

(defn ^{:test {:groups [ "registration"]}}
  register_bad_credentials [_]
  (binding [tasks/handler (fn [errtype] (= errtype :invalid-credentials))]
    (tasks/register "sdf" "sdf")))

(gen-class-testng)