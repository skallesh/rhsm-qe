(ns com.redhat.qe.sm.gui.tests.register-tests
  (:use [test-clj.testng :only [gen-class-testng data-driven]]
	      [com.redhat.qe.sm.gui.tasks.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	      gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks])
  (:import [org.testng.annotations Test BeforeClass]))

(defn ^{BeforeClass {:groups ["setup"]}}
  setup [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)))

(defn ^{Test {:groups ["registration"]}}
  simple_register [_]
  (tasks/register (@config :username) (@config :password))
  (verify (action exists? :unregister-system)))

(defn register_bad_credentials [user pass recovery]
  (let [test-fn (fn [username password expected-error-type]
                    (with-handlers [(handle expected-error-type [e]
                                      (recover e :cancel)
                                      (:type e))]
                      (tasks/register username password)))]
    (let [thrown-error (apply test-fn [user pass recovery])
          expected-error recovery
          register-button :register-system]
     (verify (and (= thrown-error expected-error) (action exists? register-button))))))

(data-driven register_bad_credentials {Test {:groups ["registration"]}}
  [^{Test {:groups ["blockedByBug-718045"]}} ["sdf" "sdf" :invalid-credentials]
   ["" "" :no-username]
   ["" "password" :no-username]
   ["sdf" "" :no-password]])

(defn ^{Test {:groups ["registration"]
	       :dependsOnMethods ["simple_register"]}}
  unregister [_]
  (tasks/unregister))

(gen-class-testng)

