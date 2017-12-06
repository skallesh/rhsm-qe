(ns rhsm.api.tests.attach_tests
  (:use [test-clj.testng :only (gen-class-testng)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd
                                           auth-proxyrunner
                                           noauth-proxyrunner)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.tools :as tools]
            [rhsm.gui.tasks.test-config :as c]
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
            AfterSuite
            AfterGroups
            DataProvider]
           org.testng.SkipException
           [com.github.redhatqe.polarize.metadata
            TestDefinition
            TestType
            DefTypes
            DefTypes$Project
            DefTypes$TestTypes
            DefTypes$Subtypes
            DefTypes$Level
            DefTypes$PosNeg
            DefTypes$Importance
            DefTypes$Automation
            LinkedItem]))

(defn maybe-append-locale
  [locale command]
  (if (clojure.string/blank? locale)
    command
    (format "LANG=%s %s" locale command)))

(defn ^{BeforeSuite {:groups ["setup"]}}
  startup [_]
  "installs scripts usable for playing with a file /etc/rhsm/rhsm.conf"
  (tools/run-command "mkdir -p ~/bin")
  (tools/run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/get-config-value.py > get-config-value.py")
  (tools/run-command "cd ~/bin && curl --insecure  https://rhsm-gitlab.usersys.redhat.com/rhsm-qe/scripts/raw/master/set-config-value.py > set-config-value.py")
  (tools/run-command "chmod 755 ~/bin/get-config-value.py")
  (tools/run-command "chmod 755 ~/bin/set-config-value.py"))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  set_inotify_to_1
  [ts]
  "set a variable inotify to 1 in /etc/rhsm/rhsm.conf"
  (log/info "set a variable inotify to 1 in /etc/rhsm/rhsm.conf")
  (tools/run-command "~/bin/set-config-value.py /etc/rhsm/rhsm.conf rhsm inotify 1")
  (tools/run-command "systemctl stop rhsm.service")
  (tools/run-command "systemctl start rhsm.service"))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :dataProvider "client-with-english-locales"
              :description "Given a dbus service rhsm is active
When I call 'tree' using busctl for com.redhat.RHSM1
Then the response contains 'Attach' node
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-112602"]}}
  attach_object_is_available
  [ts run-command locale]
  "shell
[root@jstavel-rhel7-latest-server ~]# busctl tree com.redhat.RHSM1
└─/com
  └─/com/redhat
    └─/com/redhat/RHSM1
      ├─/com/redhat/RHSM1/Config
      ├─/com/redhat/RHSM1/Entitlement
      ├─/com/redhat/RHSM1/Attach
      └─/com/redhat/RHSM1/RegisterServer
"
  (let [list-of-dbus-objects (->> "busctl tree com.redhat.RHSM1"
                                         (maybe-append-locale locale)
                                         tools/run-command
                                         :stdout
                                         (re-seq #"(├─|└─)/com/redhat/RHSM1/([^ ]+)")
                                         (map (fn [xs] (nth xs 2)))
                                         (map clojure.string/trim)
                                         (into #{}))]
    (verify (clojure.set/subset? #{"Attach"} list-of-dbus-objects))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :dataProvider "client-with-english-locales"
              :description "Given a dbus service rhsm is active
When I call 'introspect' using busctl for com.redhat.RHSM1
   and an interface /com/redhat/RHSM1/Attach
Then the methods list contains of methods 'AutoAttach', 'PoolAttach'
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-112602"]}}
  attach_methods
  [ts run-command locale]
  (let [methods-of-the-object
        (->> "busctl introspect com.redhat.RHSM1 /com/redhat/RHSM1/Attach"
             (maybe-append-locale locale)
             run-command
             :stdout
             clojure.string/split-lines
             (filter (fn [s] (re-find #"[\ \t]method[\ \t]" s)))
             (map (fn [s] (clojure.string/replace s #"^([^\ \t]+).*" "$1")))
             (into #{}))]
    (verify (clojure.set/subset? #{".AutoAttach" ".Introspect" ".PoolAttach"} methods-of-the-object))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"
                       "blockedByBug-1477958"]
              :dataProvider "client-with-english-locales"
              :description "Given a system is registered without any attached pool
When I call a DBus method 'PoolAttach' at com.redhat.RHSM1.Attach object
Then the response contains of list of json strings described attached pools
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-112602"]}}
  attach_pool_using_dbus
  [ts run-command locale]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true tools/get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (->> "subscription-manager unregister" run-command)
  (let [output (->> (format "subscription-manager register --username=%s --password=%s --org=%s"
                            (@config :username) (@config :password) (@config :owner-key))
                    (maybe-append-locale locale)
                    run-command
                    :stdout) ]
    (verify (.contains output "Registering to:")))
  (Thread/sleep 3000)
  ;;(->> "subscription-manager remove --all" tools/run-command)
  (let [list-of-available-pools (rest/list-of-available-pools
                                 (ctasks/server-url)
                                 (@config :owner-key)
                                 (@config :username)
                                 (@config :password))
        ids-of-available-pools (map :id list-of-available-pools)
        num-of-wanted-pools 2
        a-few-pool-ids (take num-of-wanted-pools ids-of-available-pools)]
    (let [{:keys [stdout stderr exitcode]}
          (->> (str "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/Attach com.redhat.RHSM1.Attach PoolAttach"
                    " asia{sv} "
                    (format " %d " num-of-wanted-pools) (clojure.string/join " " a-few-pool-ids)
                    " 1 " ;;quantity
                    " 0 ")
               (maybe-append-locale locale)
               run-command)]
      (verify (= stderr ""))
      (verify (= exitcode 0))
      (let [[response-data rest] (-> stdout dbus/parse)]
        (assert (= rest ""))
        (assert (= (type response-data) clojure.lang.PersistentVector))
        (assert (= (count response-data) num-of-wanted-pools))
        (let [entitlements (map (fn [x] (-> x (str/replace #"\\\"" "\"") json/read-str)) response-data)]
          (let [ids-from-response (->> entitlements
                                       (map (fn [ent] (-> ent first (get "pool") (get "id"))))
                                       set)
                set-of-a-few-ids(set a-few-pool-ids)]
            (verify (= ids-from-response set-of-a-few-ids))))))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"
                       "blockedByBug-1477958"]
              :dataProvider "client-with-english-locales"
              :description "Given a system is registered without any attached pool
When I call a DBus method 'PoolAttach' at com.redhat.RHSM1.Attach object
Then the response contains of list of json strings described attached pools
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-112602"]}}
  dbus_attach_pool_reflects_identity_change_even_inotify_is_zero
  [ts run-command locale]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true tools/get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (run-command "~/bin/set-config-value.py /etc/rhsm/rhsm.conf rhsm inotify 0")
  (run-command "systemctl stop rhsm.service")
  (run-command "systemctl start rhsm.service")
  (attach_pool_using_dbus ts run-command locale))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"]
              :dataProvider "client-with-english-locales"
              :description "Given a system is registered without any attached pool
When I call a DBus method 'AutoAttach' at com.redhat.RHSM1.Attach object
with the proper Service Level
Then the response contains of keys ['status','overall_status','reasons']
  and a value of 'status' is 1"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-112602"]}}
  autoattach_pool_using_dbus
  [ts run-command locale]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true tools/get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (->> "subscription-manager unregister"
       (maybe-append-locale locale)
       run-command)
  (let [output (->> (format "subscription-manager register --username=%s --password=%s --org=%s"
                           (@config :username) (@config :password) (@config :owner-key))
                   (maybe-append-locale locale)
                   run-command
                   :stdout)]
    (verify (.contains output "Registering to:")))
  (verify (.contains (-> "subscription-manager list --consumed" run-command :stdout)
                     "No consumed subscription pools were found."))
  (let [list-of-available-pools (rest/list-of-available-pools
                                 (ctasks/server-url)
                                 (@config :owner-key)
                                 (@config :username)
                                 (@config :password))]
    (let [{:keys [stdout stderr exitcode]}
          (->> (str "busctl call com.redhat.RHSM1 /com/redhat/RHSM1/Attach com.redhat.RHSM1.Attach AutoAttach"
                    " sa{sv} "
                    " Standard "
                    " 0")
               (maybe-append-locale locale)
               run-command)]
      (verify (= stderr ""))
      (verify (= exitcode 0))
      (let [[response-data rest] (-> stdout dbus/parse)]
        ;; the value is pretty big to it is not necessary to see it in a command verity
        (assert (= rest ""))
        (assert (= (type response-data) java.lang.String))
        (let [parsed (-> response-data (str/replace #"\\\"" "\"") json/read-str)
              compliance-levels (->> parsed
                                     (map (fn [pool] (-> pool
                                                        (get "pool")
                                                        (get "calculatedAttributes")
                                                        (get "compliance_type"))))
                                     set)]
          (verify (some #{"Standard"} compliance-levels))))
      (verify (not (.contains (-> "subscription-manager list --consumed" run-command :stdout)
                              "No consumed subscription pools were found."))))))

(defn ^{Test {:groups ["DBUS"
                       "API"
                       "tier1"
                       "blockedByBug-1477958"]
              :dataProvider "client-with-english-locales"
              :description "Given a system is registered without any attached pool
When I call a DBus method 'AutoAttach' at com.redhat.RHSM1.Attach object
Then the response contains of list of json strings described attached pools
"}
        TestDefinition {:projectID [`DefTypes$Project/RedHatEnterpriseLinux7]
                        :testCaseID ["RHEL7-112602"]}}
  dbus_autoattach_pool_reflects_identity_change_even_inotify_is_zero
  [ts run-command locale]
  (let [[_ major minor] (re-find #"(\d)\.(\d)" (-> :true tools/get-release :version))]
    (match major
           (a :guard #(< (Integer. %) 7 )) (throw (SkipException. "busctl is not available in RHEL6"))
           :else nil))
  (run-command "~/bin/set-config-value.py /etc/rhsm/rhsm.conf rhsm inotify 0")
  (run-command "systemctl stop rhsm.service")
  (run-command "systemctl start rhsm.service")
  (autoattach_pool_using_dbus ts run-command locale))

(defn run-command
  "Runs a given command on the client using SSHCommandRunner()."
  [runner command]
  (let [result (.runCommandAndWait runner command)
        out (.getStdout result)
        err (.getStderr result)
        exit (.getExitCode result)]
    {:stdout out
     :stderr err
     :exitcode exit}))

(defn ^{DataProvider {:name "client-with-english-locales"}}
  client_with_english_locales
  "It provides a run-command for a locally run command. And locales dict to solve problems with locales."
  [_]
  (-> [[(partial run-command @c/clientcmd) "en_US.UTF-8"]]
      to-array-2d))

(gen-class-testng)
