(ns sm.gui.tests.register-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)]
	[clojure.contrib.error-kit :only (with-handler handle)])
  (:require [sm.gui.tasks :as tasks]
	    [sm.gui.errors :as errors]))

(defn ^{:test {:groups ["registration"]}}
  simple-register [_]
  (tasks/register (config :username) (config :password)))

(defn ^{:test {:groups [ "registration"]}}
  register-bad-credentials [_]
  (with-handler (tasks/register "sdf" "sdf")
    (handle errors/*invalid-login* [s])) )

(gen-class-testng)