(ns rhsm.gui.tests.firstboot-proxy-test
  (:use gnome.ldtp
        rhsm.gui.tasks.tools
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)])
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tests.firstboot_proxy_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.runtestng]
             [rhsm.gui.tests.base :as base])
  (:import   org.testng.SkipException))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/firstboot_proxy_init nil)
                      (f)))

(deftest firstboot_enable_proxy_auth_connect-test
  (tests/firstboot_enable_proxy_auth_connect nil))

(deftest firstboot_enable_proxy_noauth_connect-test
  (tests/firstboot_enable_proxy_noauth_connect nil))
