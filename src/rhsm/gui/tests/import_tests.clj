(ns rhsm.gui.tests.import_tests
  (:use [test-clj.testng :only (gen-class-testng
                                data-driven)]
        [rhsm.gui.tasks.test-config :only (config
                                           clientcmd)]
        [slingshot.slingshot :only (try+
                                    throw+)]
        [com.redhat.qe.verify :only (verify)]
        [clojure.string :only (split
                               split-lines
                               trim)]
        rhsm.gui.tasks.tools
        gnome.ldtp)
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as cji]
            [rhsm.gui.tasks.tasks :as tasks]
            [rhsm.gui.tasks.candlepin-tasks :as ctasks]
            [rhsm.gui.tests.base :as base]
             rhsm.gui.tasks.ui)
  (:import [org.testng.annotations
            BeforeClass
            BeforeGroups
            Test
            DataProvider
            AfterClass]
           org.testng.SkipException
           [rhsm.cli.tests ImportTests]
           [rhsm.cli.tasks SubscriptionManagerTasks]
           [com.redhat.qe.auto.bugzilla BzChecker]))

(def importedcert (atom nil))
; paths here have to be lowercase because of rhel5 ldtp's
; settextvalue's bullshit
(def tmpcertpath "/tmp/sm-boguscerts/")
(def entitlementspath "/tmp/sm-entitlements")
(def importable-certs-path "/tmp/sm-importcerts")

(defn ^String str-drop [n ^String s]
  (if (< (count s) n)
    ""
    (.substring s n)))

(defn get-valid-import-cert
  "Returns a random importable cert file from a given directory

  cert-dir: a directory path containing importable certs"
  ([^String cert-dir]
   (let [certs (:stdout (run-command (format "ls -A %s" cert-dir)))
         cert (first (shuffle (split certs #"\n")))]
     (-> (cji/file cert-dir cert) .getPath)))
  ([]
    (get-valid-import-cert importable-certs-path)))

(defn create-import-certs
  "Creates importable .pem files in cert-dir given valid entitlements in entitle-dir

  Concatenates the pem and key-pem files to make importable entitlement certs.  The cert-dir
  is where the importable certs will be created, and the entitle-dir is the path to where
  the entitlement pem and key pem files exist"
  [^String cert-dir ^String entitle-dir]
  (safe-delete cert-dir)
  (make-dir cert-dir)
  (let [entitlements (split (:stdout (run-command (format "ls -A %s | grep -v key " entitle-dir))) #"\.pem\n")]
    (doseq [ent entitlements]
      (let [e (str entitle-dir "/" ent)
            k (str e "-key.pem")
            p (str e ".pem")]
        (run-command (format "cat %s %s > %s/%s.pem" p k cert-dir ent))))))

(defn setup-entitlement-certs
  "Generates entitlement certs into directory imp-ent-cert-dir"
  [^String imp-ent-cert-dir]
  (run-command "killall -9 yum")                            ;; prevent yum from blowing up
  (safe-delete imp-ent-cert-dir)
  (make-dir imp-ent-cert-dir)
  (let [[user pw org] (for [x [:username :password :owner-key]] (x @config))
        _ (run-command (format "subscription-manager register --user=%s --password=%s --org=%s" user pw org))
        orig-ent-cert-dir (tasks/conf-file-value "entitlementCertDir")
        _ (tasks/set-conf-file-value "entitlementCertDir" imp-ent-cert-dir)
        pools (random-from-pool (map :id (ctasks/list-available true)) 5)
        args (apply str " --pool=" (interpose " --pool=" pools))
        _ (run-command (str "subscription-manager attach " args))]
    ;; Copy all of the entitlement .pem files to entitle-dir because unregistering deletes the *.pem files
    (tasks/set-conf-file-value "entitlementCertDir" orig-ent-cert-dir)
    ;; unregistering will delete all the entitlements, , then restore
    (run-command "subscription-manager unregister")))

(defn ^{BeforeClass {:groups ["setup"]}}
  create_certs [_]
  (try
    (skip-if-bz-open "1170761")
    (tasks/kill-app)
    (setup-entitlement-certs entitlementspath)
    (create-import-certs importable-certs-path entitlementspath)
    (tasks/start-app)
    (catch Exception e
      (reset! (skip-groups :import) true)
      (throw e))))

(defn ^{AfterClass {:groups ["cleanup"]
                    :alwaysRun true}}
  cleanup_import [_]
  (assert-valid-testing-arch)
  (tasks/restart-app))

(defn- cert-version-one? []
  (let [version (System/getProperty "sm.client.certificateVersion")]
    (if (and version (re-find #"^1\." version))
      true
      false)))

(defn ^{Test {:groups ["import"
                       "tier1"
                       "blockedByBug-712980"
                       ;checking this one in the function
                       ;"blockedByBug-860344"
                       "blockedByBug-712978"]
              :priority (int 10)}}
  import_valid_cert
  "Tests to see if a valid certificate can be imported sucessfully."
  [_]
  ;only run this test if the bug is fixed or if we're using version 1.x certs
  (if-not (cert-version-one?)
    (verify (not (.isBugOpen (BzChecker/getInstance) "860344"))))
  (tasks/restart-app)
  (let [certlocation (str (get-valid-import-cert))
        certdir (tasks/conf-file-value "entitlementCertDir")
        cert (last (split certlocation #"/"))
        key (clojure.string/replace cert ".pem" "-key.pem")
        command (if (cert-version-one?)
                  (str "openssl x509 -text -in "
                       certlocation
                       " | grep 2312.9.4.1: -A 1 | grep -v 2312.9.4.1")
                  (str "rct cat-cert --no-content "
                       certlocation
                       " | grep Order -A 10 | grep Name | cut -d: -f 2"))
        entname (if (cert-version-one?)
                  (str-drop
                   2 (trim
                      (:stdout (run-command command))))
                  (trim (:stdout (run-command command))))]
    (tasks/import-cert certlocation)
    (verify (bool (tasks/ui guiexist
                            :information-dialog
                            "Certificate import was successful.")))
    (tasks/ui click :info-ok)
    ;verify that it added to My Subscriptons
    (tasks/ui selecttab :my-subscriptions)
    (sleep 5000)
    (verify (< 0 (tasks/ui getrowcount :my-subscriptions-view)))
    (verify (not-nil?
             (some #{entname}
                   (tasks/get-table-elements :my-subscriptions-view 0))))
    ;verify that it split the key and the pem
    (let [certdirfiles (split-lines
                        (:stdout (run-command (str "ls " certdir))))]
      (verify (not-nil? (some #{cert} certdirfiles)))
      (verify (not-nil? (some #{key} certdirfiles))))
    (reset! importedcert
            {:certlocation certlocation
             :certdir certdir
             :cert cert
             :key key
             :entname entname})))

(defn ^{Test {:groups ["import"
                       "tier1"]
              :dependsOnMethods ["import_valid_cert"]}}
  import_unregister
  "Asserts that imported certs are sucessfully removed after unregister."
  [_]
  (try+ (tasks/register-with-creds :re-register? false)
        (catch [:type :already-registered] _))
  (tasks/ui selecttab :my-subscriptions)
  (let [assert-in-table? (fn [pred]
                           (pred
                            (some #{(:entname @importedcert)}
                                  (tasks/get-table-elements :my-subscriptions-view
                                                            0
                                                            :skip-dropdowns? true))))]
    (verify (assert-in-table? not-nil?))
    (tasks/unregister)
    (verify (assert-in-table? nil?)))
  (let [certdirfiles (split-lines
                      (:stdout (run-command (str "ls " (:certdir @importedcert)))))
        does-not-exist? (fn [file]
                          (nil? (some #{file} certdirfiles)))]
    (verify (does-not-exist? (:cert @importedcert)))
    (verify (does-not-exist? (:key @importedcert))))
  (reset! importedcert nil))

(defn ^{Test {:groups ["import"
                       "tier1"
                       "blockedByBug-691784"
                       "blockedByBug-723363"
                       "blockedByBug-1142400"]
              :dependsOnMethods ["import_valid_cert"]}}
  import_unsubscribe
  "Tests that an imported cert can be unsubscribed from in the GUI."
  [_]
  (tasks/restart-app :unregister? true)
  (import_valid_cert nil)
  (let [assert-in-table? (fn [pred]
                           (pred
                            (some #{(:entname @importedcert)}
                                  (tasks/get-table-elements :my-subscriptions-view
                                                            0
                                                            :skip-dropdowns? true))))]
    (verify (assert-in-table? not-nil?))
    (tasks/unsubscribe (:entname @importedcert))
    (verify (assert-in-table? nil?)))
  (let [certdirfiles (split-lines
                      (:stdout (run-command (str "ls " (:certdir @importedcert)))))
        does-not-exist? (fn [file]
                          (nil? (some #{file} certdirfiles)))]
    (verify (does-not-exist? (:cert @importedcert)))
    (verify (does-not-exist? (:key @importedcert))))
  (reset! importedcert nil))

(defn import-bad-cert [certname
                       expected-error]
  (let [test-fn (fn []
                  (try+
                   (tasks/import-cert certname)
                   (catch Object e
                     (:type e))))]
    (let [thrown-error (test-fn)]
      (verify (= thrown-error expected-error)))))

(defn get-random-file
  ([path filter]
     (let [path (if-not (trailing-slash? path)
                  (add-trailing-slash path)
                  path)
           certsindir (split-lines
                       (:stdout
                        (run-command (str "ls " path filter))))
           tmpcertname (rand-nth certsindir)
           certname (str tmpcertpath "a" tmpcertname)]
       (run-command (str "/bin/cp -f " path tmpcertname " " certname))
       certname))
  ([path]
     (get-random-file path "")))

(defn ^{Test {:groups ["import"
                       "tier1"
                       "blockedByBug-702075"]}}
  import_random
  "Asserts that a random file cannot be imported."
  [_]
  (let [certname "/tmp/randomcert.pem"]
    (safe-delete certname)
    (run-command (str "dd if=/dev/urandom of=" certname " bs=1M count=2"))
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier2"
                       "blockedByBug-702075"]}}
  import_entitlement
  "Asserts that an entitlement cannot be imported."
  [_]
  (let [certname (get-random-file entitlementspath " | grep -v key")]
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier2"
                       "blockedByBug-702075"]}}
  import_key
  "Asserts that a product key cannot be imported."
  [_]
  (let [certname (get-random-file entitlementspath " | grep key")]
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier2"
                       "blockedByBug-702075"]}}

  import_product
  "Asserts that a product pem cannot be imported."
  [_]
  (let [certname (get-random-file "/etc/pki/product/")]
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier1"
                       "blockedByBug-691788"]
              :dependsOnMethods ["import_random"]}}
  import_and_reopen
  "Asserts that the GUI can be restarted after an import has taken place."
  [_]
  (import_random nil)
  (tasks/restart-app)
  (verify (bool (tasks/ui guiexist :main-window))))

(defn ^{Test {:groups ["import"
                       "tier1"
                       "blockedByBug-702075"
                       "blockedByBug-748912"
                       "blockedByBug-877452"]}}
  import_nonexistant
  "Asserts the correct error message when a non-existant file is imported."
  [_]
  (verify
   (not (substring?
         "Traceback" (get-logging
                      @clientcmd
                      "/var/log/rhsm/rhsm.log"
                      "import_nonexistant"
                      (import-bad-cert "/this_does_not_exist.pem" :cert-does-not-exist))))))

(gen-class-testng)
