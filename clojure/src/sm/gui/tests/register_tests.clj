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
  (let [alltestdata [["sdf" "sdf" :invalid-credentials]
                     ["" "" :no-username]
                     ["" "password" :no-username]
                     ["sdf" "" :no-password]]
        test-fn (fn [username password expected-error-type]
                  (with-handlers [(handle-type expected-error-type [e]
                                               (recover-by :cancel)
                                               (:type e))]
                    (tasks/register username password)))]
    (doall (for [testargs alltestdata]
       (let [thrown-error (apply test-fn testargs)
             expected-error (last testargs)
             register-button :register-system]
         (verify (and (= thrown-error expected-error) (action exists? register-button))))))))

(defn ^{Test {:groups ["registration"]
	       :dependsOnTests ["simple_register"]}}
  unregister [_]
  (tasks/unregister))

(gen-class-testng)

