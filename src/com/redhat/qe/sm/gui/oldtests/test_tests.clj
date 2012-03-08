(comment
(ns com.redhat.qe.sm.gui.tests.test-tests
  (:use [test-clj.testng :only (gen-class-testng)]))

(defn ^{:test {} } faketest [_]
  (println "w00t"))

(gen-class-testng)
)
