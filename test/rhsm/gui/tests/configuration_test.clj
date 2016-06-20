(ns rhsm.gui.tests.configuration-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [rhsm.runtestng]
             [clojure.string :as s]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (println "use fixtures for proxy")
                      (base/startup nil)
                      (f)))


;; testing of our testware
(deftest basic-properties-test
  (testing "Our java properties are set properly"
    (testing "- configuration of owner related variables"
      (is (contains?  #{"snowwhite" "admin"} (System/getProperty "sm.client1.org"))))))

(deftest deploy-candlepin-data-test
  (testing "use proper combination of username and org"
    (is (contains?  #{["testuser1" "snowwhite"]
                      ["testuser2" "admin"]} (->> [:username :owner-key] (map #(% @c/config)))))))
