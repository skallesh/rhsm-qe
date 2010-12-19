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
  register_bad_credentials [_]
  (let [testdata [["sdf" "sdf" :invalid-credentials]
                  ["" "" :no-username]
                  ["" "password" :no-username]
                  ["sdf" "" :no-password]]
        test-fn (fn [username password expected-error-type]
                  (with-handlers [(handle-type expected-error-type (recover-by :cancel))]
                    (tasks/register username password)))]
    (for [test testdata] (apply test-fn test))))

(defn ^{Test {:groups [ "registration"]}}
  register_no_username [_]
  (with-handlers [(handle-type :no-username (recover-by :cancel))]
    (tasks/register "" "password")))

(defn ^{Test {:groups ["registration"]
	       :dependsOnTests ["simple_register"]}}
  unregister [_]
  (tasks/unregister))

(gen-class-testng)

