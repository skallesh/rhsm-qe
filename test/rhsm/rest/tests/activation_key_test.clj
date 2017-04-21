(ns rhsm.rest.tests.activation-key-test
  (:require  [clojure.test :refer :all]
             [rhsm.rest.tests.activation_key_tests :as tests]
             [org.httpkit.client :as http]
             [rhsm.gui.tests.base :as base]
             [clojure.data.json :as json]))

;;
;; lein quickie rhsm.rest.tests.activation-key-test
;; lein test :only rhsm.rest.tests.activation-key-test/register-using-activation-key-test
;;

;; initialization of our testware
(use-fixtures :once (fn [f]
                      (base/startup nil)
                      (f)
                      (tests/delete_all_actually_created_activation_keys nil)))

(deftest create-activation-key-test
  (tests/create_activation_key nil))

;; (deftest list-activation-keys-test
;;   (tests/list_activation_keys nil))

(deftest delete-activation-key-test
  (let [activation-key (tests/new_activation_key nil)]
    (tests/delete_activation_key nil (-> activation-key first first))))

(deftest delete-and-check-existence-of-activation-key-test
  (let [timestamp (System/currentTimeMillis)
        base-url "https://jstavel-candlepin.usersys.redhat.com:8443/candlepin"
        url-of-owner-activation-keys (str base-url "/owners/admin/activation_keys")
        activation-key-name (format "rhsm-rest-testing-key-%d" timestamp)
        options {:basic-auth ["testuser1" "password"]
                 :timeout 3000
                 :insecure? true
                 :accept :json
                 :content-type :json
                 :keepalive 30000}]
    (let [response-01 @(http/post url-of-owner-activation-keys
                                  (assoc options :body (json/json-str {:name activation-key-name})))]
      (let [activation-key (-> response-01 :body json/read-json)
            url-of-the-key (str base-url "/activation_keys/" (-> activation-key :id))]

        ;; check of the key to be created
        (let [response-02 @(http/get url-of-the-key options)]
          (is (= (-> response-02 :status) 200))
          (is (= (-> response-02 :body json/read-json)
                 (-> response-01 :body json/read-json))))

        ;; delete the activation key
        (let [response-delete @(http/delete url-of-the-key options)]
          (is (= (-> response-delete :body) ""))
          (is (= (-> response-delete :status) 204)))

        ;; check an existence of it
        (let [response-get-check @(http/get url-of-the-key options)]
          (is (= (-> response-get-check :status) 404))
          (is (= (-> response-get-check :body json/read-json :displayMessage)
                 (format "ActivationKey with id %s could not be found." (-> activation-key :id)))))

        ;; check that list of activation keys doesnot contain of the activation key already
        (let [response-all-activation-keys @(http/get url-of-owner-activation-keys options )]
          (is (-> response-all-activation-keys :body json/read-json
                  (->> (map :name))
                  (.contains activation-key-name)
                  (= false))))

        ;; delete non existing activation key
        (let [response-delete-nonexisting @(http/delete url-of-the-key options)]
          (is (= (-> response-delete-nonexisting :status) 404))
          (is (= (-> response-delete-nonexisting :body json/read-json :displayMessage)
                 (format "ActivationKey with id %s could not be found." (-> activation-key :id)))))))))


