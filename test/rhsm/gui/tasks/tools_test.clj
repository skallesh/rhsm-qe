(ns rhsm.gui.tasks.tools-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tasks :as t]
             [rhsm.gui.tasks.tools :as tt]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]
             [rhsm.runtestng]
             [slingshot.slingshot :as sl]
             [clojure.string :as s]
))


(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest rhel-version-test
  (let [{:keys [family variant version]} (tt/get-release :true)]
    (is (re-find #"^RHEL[0-9]$" family))
    (is (re-find #"^Server|Client$" variant))
    (is (re-find #"^[0-9]\.[0-9]$" version))))

(deftest get-locale-regex-test
  (testing "This test validates that get-locale-regex provides the right regex"
    (let [ datex (tt/get-locale-regex)]
      (is (not (nil? (re-matches datex "07/07/2016")))))))
