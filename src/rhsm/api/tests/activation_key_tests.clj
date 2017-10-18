(ns rhsm.api.tests.activation_key_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           auth-proxyrunner
                                           noauth-proxyrunner)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [rhsm.dbus.parser :as dbus]
            [rhsm.api.rest :as rest]
            [clojure.core.match :refer [match]]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
            [rhsm.gui.tests.activation-key-tests :as atests]
            [clojure.string :as str])
  (:import [org.testng.annotations
            Test
            BeforeClass
            AfterClass
            BeforeGroups
            BeforeSuite
            AfterGroups
            DataProvider]
           org.testng.SkipException
           [com.github.redhatqe.polarize.metadata TestDefinition]
           [com.github.redhatqe.polarize.metadata DefTypes$Project]))

(def created-activation-keys (atom (list)))
(def http-options {:timeout 3000
                   :insecure? true
                   :accept :json
                   :content-type :json
                   :keepalive 30000})

(defn ^{BeforeSuite {:groups ["setup"]}}
  setup
  [_]
  "installs scripts usable for playing with a file /etc/rhsm/rhsm.conf"
  (run-command "mkdir -p ~/bin")
  (run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/get-config-value.py > get-config-value.py")
  (run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/set-config-value.py > set-config-value.py")
  (run-command "chmod 755 ~/bin/get-config-value.py")
  (run-command "chmod 755 ~/bin/set-config-value.py"))

(defn ^{Test {:groups ["activation-key"
                       "REST"
                       "API"
                       "tier2"]
              :description "Given the candlepin knows an owner {owner}
When a rest client sends POST to '/owners/{owner key}/activation_keys'
   with a json data {'name': '{owner}'}
Then the candlepin replies a status 200
  and the answer is the same as the candlepin returns
  for an ask GET to '/activation_keys/{returned-activation-key id}'"}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
  create_activation_key_using_rest
  [ts]
  (let [timestamp (System/currentTimeMillis)
        base-url (ctasks/server-url)
        url-of-owner-activation-keys (format "%s/owners/%s/activation_keys" base-url (@config :owner-key))
        activation-key-name (format "rhsm-rest-testing-key-%d" timestamp)
        options (assoc http-options :basic-auth [(@config :username) (@config :password)])]
    (let [response-01 @(http/post url-of-owner-activation-keys
                                  (assoc options :body (json/json-str {:name activation-key-name})))]
      (let [activation-key (-> response-01 :body json/read-json)
            url-of-the-key (str base-url "/activation_keys/" (-> activation-key :id))]
        (swap! created-activation-keys conj activation-key)
        (log/info "actually created activation key: " activation-key)
        ;; check of the key to be created
        (let [response-02 @(http/get url-of-the-key options)]
          (verify (= (-> response-02 :status) 200))
          (verify (= (-> response-02 :body json/read-json)
                     (-> response-01 :body json/read-json))))))))

(defn ^{Test {:groups ["activation-key"
                       "REST"
                       "API"
                       "tier2"]
              :description "Given the candlepin knows and owner {owner}
   and his activation key {activation-key}
When a rest client sends DELETE to '/activation_keys/{activation-key id}'
Then the candlepin replises a status 204
  and the answer contains of '' as body."
              :dataProvider "new-activation-key"}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
  delete_activation_key_using_rest
  [ts activation-key]
  (let [base-url (ctasks/server-url)
        options (assoc http-options :basic-auth [(@config :username) (@config :password)])
        url-of-the-key (str base-url "/activation_keys/" (-> activation-key :id))]
    (let [response @(http/delete url-of-the-key options)]
      (verify (= (-> response :body) ""))
      (verify (= (-> response :status) 204)))))

(defn ^{Test {:groups ["activation-key"
                       "REST"
                       "API"
                       "tier2"]
              :description "Given the candlepin knows an owner {owner}
When a rest client creates an activation key {activation-key}
  and the client deletes the activation key
  and the client sends an ask GET '/owners/{owner key}/activation_keys/{activation-key id}
Then the candlepin replies a status 404
  and the response body is 'ActivationKey with id {activation-key id} could not be found.'"}
        TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
  create_activation_key_and_delete_it_using_rest
  [ts]
  (let [timestamp (System/currentTimeMillis)
        base-url (ctasks/server-url)
        url-of-owner-activation-keys (format "%s/owners/%s/activation_keys" base-url (@config :owner-key))
        activation-key-name (format "rhsm-rest-testing-key-%d" timestamp)
        options (assoc http-options :basic-auth [(@config :username) (@config :password)])]
    (let [response-01 @(http/post url-of-owner-activation-keys
                                  (assoc options :body (json/json-str {:name activation-key-name})))]

      (let [activation-key (-> response-01 :body json/read-json)
            url-of-the-key (str base-url "/activation_keys/" (-> activation-key :id))]

        (swap! created-activation-keys conj activation-key)
        (log/info "actually created activation key: " activation-key)

        ;; check of the key to be created
        (let [response-02 @(http/get url-of-the-key options)]
          (verify (= (-> response-02 :status) 200))
          (verify (= (-> response-02 :body json/read-json)
                     (-> response-01 :body json/read-json))))

        ;; delete the activation key
        (let [response-delete @(http/delete url-of-the-key options)]
          (verify (= (-> response-delete :body) ""))
          (verify (= (-> response-delete :status) 204)))

        ;; check an existence of it
        (let [response-get-check @(http/get url-of-the-key options)]
          (verify (= (-> response-get-check :status) 404))
          (verify (= (-> response-get-check :body json/read-json :displayMessage)
                     (format "ActivationKey with id %s could not be found." (-> activation-key :id)))))

        ;; check that list of activation keys doesnot contain of the activation key already
        (let [response-all-activation-keys @(http/get url-of-owner-activation-keys options )]
          (verify (-> response-all-activation-keys :body json/read-json
                      (->> (map :name))
                      (.contains activation-key-name)
                      (= false))))

        ;; delete non existing activation key
        (let [response-delete-nonexisting @(http/delete url-of-the-key options)]
          (verify (= (-> response-delete-nonexisting :status) 404))
          (verify (= (-> response-delete-nonexisting :body json/read-json :displayMessage)
                     (format "ActivationKey with id %s could not be found." (-> activation-key :id)))))))))

(declare register-socket)
(defn ^{Test {:groups ["activation-key"
                       "DBUS"
                       "API"
                       "blockedByBug-1477958"
                       "tier2"]
              :description "Given a system is unregistered
   and a simple activation key exists for my account
When I call 'RegisterWithActivationKeys' using busctl
  with the key
Then the response contains of keys ['content','headers','status']
  and a value of 'status' is 200
"
              :dataProvider "new-activation-key"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  register_with_activation_key_using_dbus
  [ts activation-key]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (run-command "subscription-manager unregister")
  (rest/activation-key-exists (-> activation-key :id))
  (Thread/sleep 3000)
  (let [socket (register-socket ts)]
    (let [response (-> (format "busctl --address=unix:abstract=%s call com.redhat.RHSM1 /com/redhat/RHSM1/Register com.redhat.RHSM1.Register RegisterWithActivationKeys 'sasa{sv}a{sv}' %s 1 %s 0 0" socket (@config :owner-key) (-> activation-key :name))
                       run-command)]
      (verify (=  (:stderr response) ""))
      (let [[parsed-response rest-of-the-string] (-> response :stdout (.trim) dbus/parse)]
        (verify (->> rest-of-the-string (= ""))) ;; no string left unparsed
        (let [data (-> parsed-response (str/replace #"\\\"" "\"") json/read-str)]
          (verify (-> data keys set
                      (clojure.set/superset? #{"entitlementStatus"
                                               "capabilities"
                                               "owner"
                                               "created"
                                               "contentTags"
                                               "contentAccessMode"
                                               "id"
                                               "autoheal"
                                               "installedProducts"
                                               "name"
                                               "uuid"}))))))))

(defn ^{Test {:groups ["activation-key"
                       "DBUS"
                       "API"
                       "blockedByBug-1477958"
                       "tier2"]
              :description "Given a system is unregistered
   and a simple activation key exists for my account
When I call 'RegisterWithActivationKeys' using busctl
  with the key
Then the response contains of json structure with entitlement information"
              :dataProvider "new-activation-key"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  dbus_register_with_activation_key_reflects_identity_change
  [ts activation-key]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (run-command "subscription-manager unregister")
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      run-command   :stdout  (.contains "Registering to:")  verify)
  (register_with_activation_key_using_dbus ts activation-key))

(defn ^{Test {:groups ["activation-key"
                       "DBUS"
                       "API"
                       "blockedByBug-1477958"
                       "tier2"]
              :description "Register With Activation key works even a variable 'inotify' is set to 0 and identity is changed."
              :dataProvider "new-activation-key"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]}}
  dbus_register_with_activation_key_reflects_identity_change_even_inotify_is_zero
  [ts activation-key]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (run-command "~/bin/set-config-value.py /etc/rhsm/rhsm.conf rhsm inotify 0")
  (run-command "systemctl stop rhsm.service")
  (run-command "systemctl start rhsm.service")
  (run-command "systemctl status rhsm.service -l")
  (run-command "subscription-manager unregister")
  (-> (format "subscription-manager register --username=%s --password=%s --org=%s"
              (@config :username) (@config :password) (@config :owner-key))
      run-command   :stdout  (.contains "Registering to:")  verify)
  (register_with_activation_key_using_dbus ts activation-key))

;; (defn ^{Test {:groups ["activation-key"
;;                        "tier2"]}
;;         TestDefinition {:projectID [`DefTypes$Project/RHEL6 `DefTypes$Project/RedHatEnterpriseLinux7]}}
;;   list_activation_keys
;;   [ts]
;;   (http/get (format "%s/owners/%s/activation_keys"
;;                     (ctasks/server-url)
;;                     (@config :owner-key))
;;             {:basic-auth [(@config :username) (@config :password)]
;;              :accept :json
;;              :content-type :json
;;              :insecure? true}))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  delete_all_actually_created_activation_keys
  [ts]
  "delete all activation keys created by this test run"
  (log/info "delete all actually created activation keys")
  (log/info "actually created activation keys to be deleted: " @created-activation-keys)
  (doseq [activation-key @created-activation-keys]
    (let [activation-key-id (-> activation-key :id)
          url-of-the-key (format "%s/activation_keys/%s"
                                 (ctasks/server-url)
                                 activation-key-id)]
      @(http/delete url-of-the-key
                    (assoc http-options
                           :basic-auth [(@config :username) (@config :password)]))
      (swap! created-activation-keys (fn [coll] (filter (fn [x] (not= (-> x :id) activation-key-id)) coll))))))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  set_inotify_to_1
  [ts]
  "set a variable inotify to 1 in /etc/rhsm/rhsm.conf"
  (log/info "set a variable inotify to 1 in /etc/rhsm/rhsm.conf")
  (run-command "~/bin/set-config-value.py /etc/rhsm/rhsm.conf rhsm inotify 1")
  (run-command "systemctl stop rhsm.service")
  (run-command "systemctl start rhsm.service"))

(defn ^{DataProvider {:name "new-activation-key"}}
  new_activation_key [ts]
  (let [response
        @(http/post
          (format "%s/owners/%s/activation_keys"
                  (ctasks/server-url) (@config :owner-key))
          (assoc http-options
                 :basic-auth [(@config :username) (@config :password)]
                 :body (-> {:name (format "rhsm-api-tests-%d" (System/currentTimeMillis))}
                           json/json-str)))
        status-of-the-response (-> response :status)]
    (verify (->> status-of-the-response (= 200)))
    (let [activation-key (-> response :body json/read-json)]
      (log/info "new activation key has been created" activation-key)
      (swap! created-activation-keys conj activation-key)
      (-> activation-key
          vector
          vector
          to-array-2d))))

(defn register-socket
  "provides a socket that is used by DBus RHSM Register service"
  [ts]
  "response:   s \"unix:abstract=/var/run/dbus-XP0szXbntD,guid=e234053b12b7e78b2c48cac758a60d17\""
  (-> (->> "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/RegisterServer com.redhat.RHSM1.RegisterServer Start"
           run-command
           :stdout
           (re-seq #"abstract=([^,]+)"))
      (nth 0)
      (nth 1)))

(gen-class-testng)
