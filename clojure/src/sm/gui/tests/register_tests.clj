(ns sm.gui.tests.register-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)])
  (:require [sm.gui.tasks :as tasks])
  (:import [org.testng.annotations Test]))

(defn ^{:test {:groups ["registration"]}}
  simple_register [_]
  (tasks/register (config :username) (config :password)))

(defn ^{:test {:groups [ "registration"]}}
  register_bad_credentials [_]
  (binding [tasks/handler (fn [errtype] (= errtype :invalid-credentials))]
    (tasks/register "sdf" "sdf")))

(defn ^{:test {:groups #{"registration"}
	       :dependsOnTests ["simple_register"]}}
  unregister [_]
  (tasks/unregister))

(defn ^{Test {}} mytest [self] (println "hi"))
(gen-class-testng)

