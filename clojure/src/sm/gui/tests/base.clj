(ns sm.gui.tests.base
  (:use [test-clj.testng :only (gen-class-testng)]
	[sm.gui.tasks]))


(defn ^{:test {:configuration :beforeSuite}}
  startup [_]
  (connect)
  (start-app))

(gen-class-testng)
