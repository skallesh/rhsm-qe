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
           [com.redhat.qe.auto.bugzilla BzChecker OldBzChecker]))

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
  (-> (format "semanage fcontext -a -s system_u -t cert_t \"%s(/.*)?\""  (clojure.string/replace cert-dir #"/*$" ""))
      run-command)
  (-> (format "restorecon -v -R \"%s\"" cert-dir)
      run-command)
  (let [entitlements (split (:stdout (run-command (format "ls -A %s | grep -v key " entitle-dir))) #"\.pem\n")]
    (doseq [ent entitlements]
      (let [e (str entitle-dir "/" ent)
            k (str e "-key.pem")
            p (str e ".pem")]
        (run-command (format "cat %s %s > %s/%s.pem" p k cert-dir ent))))))

(defn setup-entitlement-certs
  "Generates entitlement certs into directory imp-ent-cert-dir"
  [^String imp-ent-cert-dir]
  (run-command "killall -9 yum") ;; prevent yum from blowing up
  (safe-delete imp-ent-cert-dir)
  (make-dir imp-ent-cert-dir)
  (-> (format "semanage fcontext -a -s system_u -t cert_t \"%s(/.*)?\""  (clojure.string/replace imp-ent-cert-dir #"/*$" ""))
      run-command)
  (-> (format "restorecon -v -R \"%s\"" imp-ent-cert-dir)
      run-command)
  (let [[user pw org] (for [x [:username :password :owner-key]] (x @config))
        orig-ent-cert-dir (tasks/conf-file-value "entitlementCertDir")]
    (run-command (format "subscription-manager register --user=%s --password=%s --org=%s" user pw org))
    (try
      (tasks/set-conf-file-value "entitlementCertDir" imp-ent-cert-dir)
      (let [pools (random-from-pool (map :id (ctasks/list-available true)) 5)
            args (apply str " --pool=" (interpose " --pool=" pools))]
        (run-command (str "subscription-manager attach " args)))
      (finally
        (log/info "moving entitlementCertDir back to original state")
        ;; Copy all of the entitlement .pem files to entitle-dir because unregistering deletes the *.pem files
        (tasks/set-conf-file-value "entitlementCertDir" orig-ent-cert-dir)
        ;; unregistering will delete all the entitlements, , then restore
        (run-command "subscription-manager unregister")))))

(defn make-importable-cert
  "Given a path to entitlement cert, generate an importable cert and store it in import-path"
  [ent-path import-path]
  (let [paths (split ent-path #"/")
        fname (last paths)
        dir (clojure.string/join "/" (butlast paths))
        id (re-find #"\d+" fname)
        key-pem (str dir "/" id "-key.pem")
        full-import-path (str (if (not (trailing-slash? import-path))
                                (add-trailing-slash import-path)
                                import-path) fname)]
    {:importable-cert full-import-path
     :result          (run-command (format "cat %s %s > %s/%s.pem" ent-path key-pem import-path id))}))

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
                       "tier2"
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
    (verify (not (.isBugOpen (OldBzChecker/getInstance) "860344"))))
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
                       "tier2"]
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
                       "tier2"
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
                       "tier2"
                       "blockedByBug-702075"]}}
  import_random
  "Asserts that a random file cannot be imported."
  [_]
  (let [certname "/tmp/randomcert.pem"]
    (safe-delete certname)
    (run-command (str "dd if=/dev/urandom of=" certname " bs=1M count=2"))
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier3"
                       "blockedByBug-702075"]}}
  import_entitlement
  "Asserts that an entitlement cannot be imported."
  [_]
  (let [certname (get-random-file entitlementspath " | grep -v key")]
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier3"
                       "blockedByBug-702075"]}}
  import_key
  "Asserts that a product key cannot be imported."
  [_]
  (let [certname (get-random-file entitlementspath " | grep key")]
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier3"
                       "blockedByBug-702075"]}}

  import_product
  "Asserts that a product pem cannot be imported."
  [_]
  (let [certname (get-random-file "/etc/pki/product/")]
    (import-bad-cert certname :invalid-cert)))

(defn ^{Test {:groups ["import"
                       "tier2"
                       "blockedByBug-691788"]
              :dependsOnMethods ["import_random"]}}
  import_and_reopen
  "Asserts that the GUI can be restarted after an import has taken place."
  [_]
  (import_random nil)
  (tasks/restart-app)
  (verify (bool (tasks/ui guiexist :main-window))))

(defn ^{Test {:groups ["import"
                       "tier2"
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

(defn select-random-row
  "Selects a random row from a table"
  [table]
  (tasks/search :match-system? false
                :do-not-overlap? false
                :contain-text nil)
  (let [rows (tasks/ui getrowcount table)
        rand-row (rand-int (inc rows))]
    (tasks/ui selectrowindex table rand-row)))

(defn cert-map-seq
  "Converts the contents of an entitlement cert into an intermediate map form
  The contents is the text of a certifate (eg output of rct cat-cert some-cert.pem"
  [content]
  (let [section-re #"^\w+:\s+$"
        kv-re #"^\s+(.+):\s*(.+)\n$"
        section (atom "")]
    (for [line (clojure.string/split content #"\n")]
      (let [line (str line "\n")
            [_ key val] (re-find kv-re line)
            key (try
                  (keywordize key)
                  (catch Exception e nil))
            newsection (re-matches section-re line)]
        (if (and newsection (not= newsection @section))
          (reset! section (-> (clojure.string/trim-newline newsection) (clojure.string/replace #"\:" "") keyword)))
        {:section @section key val}))))

(defn- reduce-parse-cert
  "A reducing function that cleans up the intermediate map produced by cert-map-seq

  *Args*
  - m1: an accumulating map
  - m2: a map passed in from reduce function"
  [m1 m2]
  (let [s (:section m2)
        kv (dissoc m2 :section)]
    (if (contains? m1 s)
      (if (not= s :Content)
        (assoc-in m1 [s] (merge kv (m1 s)))
        (assoc-in m1 [s] (conj (m1 s) kv)))
      (if (not= s :Content)
        (assoc m1 s kv)
        (assoc m1 s [kv])))))

(defn parse-cert
  "Reads a cert file and produces a nice keywordized map

  *Args*
  - cert: path to the certificate"
  [cert]
  (let [fname (last (split cert #"/"))
        contents (-> (format "rct cat-cert %s" cert) run-command :stdout)
        ms (cert-map-seq contents)
        filtered (filter #(not (nil-keys? %)) ms)]
    {fname (reduce reduce-parse-cert {} filtered)}))

(defn map-all-certs
  "Generates a lazy sequence of all the certs in a pretty map form

  *Args*
  - cert-dir: path to the certificates"
  [cert-dir]
  (into {} (let [files (-> (format "ls -A %s/*.pem" cert-dir) run-command :stdout (split #"\n"))
                 entitle-paths (filter #(not (.contains % "key")) files)]
             (for [f entitle-paths]
               (parse-cert f)))))

(defn cert->poolid
  "Creates a 2 element vector of cert filename and poolid

  *Args*
  - certmap: a map version of an entitlement cert (as produced by parse-cert"
  [certmap]
  (first (for [[ecert data] certmap]
           [ecert (get-in data [:Certificate :pool-id])])))

(defn poolid->cert
  "Given a poolid and a sequence of certificate maps, find the entitlement file that corresponds
  to the poolid"
  [poolid certs]
  (let [fltr (fn [[ecert id]] (= id poolid))
        matched (filter fltr (map cert->poolid certs))]
    (first matched)))

(defn ^{Test {:groups ["import"
                       "tier2"
                       "blockedByBug-1240553"]
              :description "Tests if imported cert shows up in both CLI consumed list and the My Subscriptions tab

              1. Attaches a random subscription
              2. Finds the entitlement associated with the pool-id of the subscription above
              3. Creates an importable cert based on the entitlement
              4. Removes local data via a subscription-manager clean command
              5. Imports the cert associated with the random product we attached
              6. Runs the list --consumed command to verify that product is still consumed
              7. Verifies that the product shows in the My Subscriptions tab"}}
  import_validate_cert_shows_in_consumed
  [_]
  (tasks/restart-app)
  (try+ (tasks/register-with-creds :re-register? false)
        (catch [:type :already-registered] _))
  (let [[subscription _] (first (shuffle (for [[prod id] (ctasks/get-pool-ids)]
                                           [prod id])))
        _ (do
            (tasks/search)
            (tasks/subscribe subscription))
        consumed-before (-> (run-command "subscription-manager list --consumed") :stdout (tasks/parse-list))
        rand-pool (-> (filter #(= (:subscription-name %) subscription) consumed-before) first :pool-id)
        certmaps (map-all-certs "/etc/pki/entitlement")
        cert-file (first (for [[k v] certmaps :when (= rand-pool (get-in v [:Certificate :pool-id]))]
                           k))
        entitle-path (str "/etc/pki/entitlement/" cert-file)
        {:keys [importable-cert]} (make-importable-cert entitle-path importable-certs-path)
        _ (run-command "subscription-manager clean")
        _ (tasks/import-cert importable-cert)
        consumed-after (filter #(= rand-pool (:pool-id %))
                               (-> (run-command "subscription-manager list --consumed") :stdout (tasks/parse-list)))]
    (verify (-> (first consumed-after) :status-details (= "Subscription management service doesn't support Status Details.")))
    ;; verify that the product we subscribed is showing up in My Subscriptions
    (tasks/ui selecttab :my-subscriptions)
    (verify (some #{subscription}
                  (tasks/get-table-elements :my-subscriptions-view 0)))))

(defn ^{Test {:groups ["import"
                       "tier2"
                       "blockedByBug-1141128"]
              :dependsOnMethods ["import_validate_cert_shows_in_consumed"]
              :description "Verifies that a product that was attached via an import can be removed"}}
  import_validate_cert_removal
  [_]
  ;; We can't be sure which product we imported through the cert, so remove all of them
  (tasks/do-to-all-rows-in :my-subscriptions-view 0 tasks/unsubscribe)
  ;; Verify that there are no more subscriptions
  (verify (= 0 (count (tasks/get-table-elements :my-subscriptions-view 0)))))

(gen-class-testng)
