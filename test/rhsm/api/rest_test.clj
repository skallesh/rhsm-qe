(ns rhsm.api.rest-test
  (:require  [clojure.test :refer :all]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.candlepin-tasks :as ctasks]
             [rhsm.api.rest :as rest]
             [rhsm.gui.tasks.test-config :refer [config]]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))
;;
;; lein quickie rhsm.api.rest-test
;; lein test :only rhsm.rest.tests.entitlement-test/getpools-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)))

(deftest list-of-available-pools-test
  (let [list-of-pools (rest/list-of-available-pools
                       (ctasks/server-url)
                       (@config :owner-key)
                       (@config :username)
                       (@config :password))]
    (let [one-pool (-> list-of-pools first)
          keys-of-the-pool (-> one-pool keys)]
      (is (= #{:accountNumber :productAttributes :updated :orderNumber
               :contractNumber :startDate :subscriptionSubKey
               :activeSubscription :type :created :calculatedAttributes
               :productName :productId :stacked :endDate :developmentPool
               :providedProducts :derivedProvidedProducts :id :consumed
               :branding :shared :quantity :derivedProductAttributes
               :attributes :exported :owner :href :subscriptionId}
             (into #{} keys-of-the-pool))))))
