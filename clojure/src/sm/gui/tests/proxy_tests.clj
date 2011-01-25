(ns sm.gui.tests.proxy-tests
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
	 sm.gui.ldtp)
  (:require [sm.gui.tasks :as tasks])
  (:import [org.testng.annotations Test BeforeClass]))

(defn ^{BeforeClass {}}
  setup [_]
  (with-handlers [(ignore :not-registered)]
    (tasks/unregister)))

(defn ^{Test {:groups ["proxy"]}}
  enable_proxy [_]
  )


