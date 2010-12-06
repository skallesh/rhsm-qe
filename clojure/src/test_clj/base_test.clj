(ns test-clj.base-test
  (:use [test-clj.testng :only [gen-class-testng]]) )

(def config-data (atom {:version "2.3"}))

(defn ^{:test {:configuration :beforeSuite}}
  mybefore [_]
  (println "doing base stuff, adding data to config")
  (swap! config-data assoc :build 123))

(gen-class-testng)