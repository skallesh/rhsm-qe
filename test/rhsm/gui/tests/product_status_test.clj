(ns rhsm.gui.tests.product-status-test
  (:use [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp
        rhsm.gui.tasks.tools)
  (:require  [clojure.test :refer :all]
             [clojure.core.match :refer [match]]
             [rhsm.gui.tests.product_status_tests :as tests]
             [rhsm.gui.tasks.tasks :as tasks]
             [rhsm.gui.tasks.tools :as tools]
             [rhsm.gui.tasks.ui :as ui]
             [rhsm.gui.tasks.test-config :as c]
             [rhsm.gui.tests.base :as base]))

;; ;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (tests/before_check_product_status nil)
                      (f)))

(defn tables-are-sortable? [{:keys [major minor patch]}] ;; a version from (tools/subman-version)
  "Some tables in Subscription namager GUI are not sortabled for some versions of sub-man GUI"
  (match (vec (for [v [major minor patch]] (Integer. v)))
         [1 15 _] true
         [1 17 (_ :guard #(> % 16))] true
         [1 18 _] true
         [(_ :guard #(> % 1)) _ _] true
         :else  false))

(deftest my_installed_products_table_is_sortable-01-test
  (if (tables-are-sortable? (tools/subman-version))
    (tests/my_installed_products_table_is_sortable  nil :my-installed-products-product-header 0)
    (is (thrown? AssertionError (tests/my_installed_products_table_is_sortable
                                 nil
                                 :my-installed-products-product-header
                                 0)))))

(deftest my_installed_products_table_is_sortable-02-test
  (if (tables-are-sortable? (tools/subman-version))
    (tests/my_installed_products_table_is_sortable  nil :my-installed-products-version-header 1)
    (is (thrown? AssertionError (tests/my_installed_products_table_is_sortable
                                 nil
                                 :my-installed-products-version-header
                                 1)))))

(deftest my_installed_products_table_is_sortable-03-test
  (if (tables-are-sortable? (tools/subman-version))
    (tests/my_installed_products_table_is_sortable  nil :my-installed-products-status-header 2)
    (is (thrown? AssertionError (tests/my_installed_products_table_is_sortable
                                 nil
                                 :my-installed-products-status-header
                                 2)))))

(deftest my_installed_products_table_is_sortable-04-test
  (if (tables-are-sortable? (tools/subman-version))
    (tests/my_installed_products_table_is_sortable  nil :my-installed-products-startdate-header 3)
    (is (thrown? AssertionError (tests/my_installed_products_table_is_sortable
                                 nil
                                 :my-installed-products-startdate-header
                                 3)))))
