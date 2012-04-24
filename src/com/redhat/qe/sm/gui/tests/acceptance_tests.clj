(ns com.redhat.qe.sm.gui.tests.acceptance-tests
  (:use [test-clj.testng :only [gen-class-testng data-driven]]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
            [com.redhat.qe.sm.gui.tests.register-tests :as rtest]
            [com.redhat.qe.sm.gui.tests.subscribe-tests :as stest]
            [com.redhat.qe.sm.gui.tests.autosubscribe-tests :as atest])
  (:import [org.testng.annotations Test BeforeClass DataProvider]))


(defn ^{Test {:groups ["acceptance"]}}
  register [_]
  (rtest/simple_register nil
                         (@config :username)
                         (@config :password)
                         nil))

(defn ^{Test {:groups ["acceptance"]}}
  unregister [_]
  (rtest/unregister nil))

(defn- check-register []
  (if (tasks/ui exists? :register-system)
    (register nil)))

(defn ^{Test {:groups ["acceptance"]}}
  subscribe_all [_]
  (check-register)
  (stest/subscribe_all))

(defn ^{Test {:groups ["acceptance"]}}
  unsubscribe_all [_]
  (check-register)
  (stest/unsubscribe_all))


(defn ^{Test {:groups ["acceptance"]}}
  simple_autosubscribe [_]
  (.runCommandAndWait @clientcmd "subscription-manager unregister")
  (tasks/restart-app)
  (atest/simple_autosubscribe nil))

(gen-class-testng)

