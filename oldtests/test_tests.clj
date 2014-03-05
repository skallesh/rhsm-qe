(comment
(ns rhsm.gui.tests.test_tests
  (:use [test-clj.testng :only (gen-class-testng)]))

(defn ^{:test {} } faketest [_]
  (println "w00t"))

(gen-class-testng)
)
