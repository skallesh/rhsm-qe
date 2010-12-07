(ns sm.gui.tests.register-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only config])
  (:require [sm.gui.tasks :as tasks]))

(defn ^{:test {:groups "registration"}}
  simple-register [_]
  (tasks/register (config :username) (config :password)))

(gen-class-testng)