(ns sm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.tasks])
  (:import [org.testng.annotations BeforeSuite]))


(defn ^{BeforeSuite {}}
  startup [_]
  (connect)
  (start-app))

(gen-class-testng)
