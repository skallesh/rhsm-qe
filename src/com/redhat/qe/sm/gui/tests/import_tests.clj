(ns com.redhat.qe.sm.gui.tests.import-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config
                                                       clientcmd)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.string :only (split
                                       split-lines
                                       trim
                                       replace-str)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass
                                   BeforeGroups
                                   Test
                                   DataProvider
                                   AfterClass]
           [com.redhat.qe.sm.cli.tests ImportTests]))

(def importtests (atom nil))
(def importedcert (atom nil))

(defn not-nil? [b] (not (nil? b)))

(defn ^String str-drop [n ^String s]
  (if (< (count s) n)
    ""
    (.substring s n)))

(defn ^{BeforeClass {:groups ["setup"]}}
  create_certs [_]
  (reset! importtests (ImportTests.))
  (.setupBeforeClass @importtests)
  (.runCommandAndWait @clientcmd "subscripton-manager unregister"))

(defn ^{AfterClass {:groups ["setup"]}}
  cleanup_import [_]
  (.cleanupAfterClass @importtests)
  (tasks/restart-app))

(defn ^{Test {:groups ["import"
                       ;"blockedByBug-737675"
                       "blokecdByBug-712980"]}}
  import_cert [_]
  (tasks/restart-app)
  (let [certlocation (str (.getValidImportCertificate @importtests))
        certdir (tasks/conf-file-value "entitlementCertDir")
        cert (last (split #"/" certlocation))
        key (replace-str ".pem" "-key.pem" cert)
        command (str "openssl x509 -text -in "
                     certlocation
                     " | grep 2312.9.4.1: -A 1 | grep -v 2312.9.4.1")
        entname (str-drop
                 2 (trim
                    (.getStdout (.runCommandAndWait @clientcmd command))))]
    (tasks/ui click :import-certificate)
    (tasks/ui click :choose-cert)
    (tasks/ui waittillguiexist :file-chooser)
    (tasks/ui check :text-entry-toggle)
    (tasks/ui generatekeyevent certlocation)
    (tasks/ui click :file-open)
    (tasks/ui click :import-cert)
    (tasks/checkforerror)
    (verify (= 1 (tasks/ui guiexist
                           :information-dialog
                           "Certificate import was successful.")))
    (tasks/ui click :info-ok)
    ;verify that it added to My Subscriptons
    (tasks/ui selecttab :my-subscriptions)
    (tasks/sleep 5000)
    (verify (< 0 (tasks/ui getrowcount :my-subscriptions-view)))
    (verify (not-nil?
             (some #{entname}
                   (tasks/get-table-elements :my-subscriptions-view 0))))
    ;verify that it split the key and the pem
    (let [certdirfiles (split-lines
                        (.getStdout (.runCommandAndWait
                                     @clientcmd (str "ls " certdir))))]
      (verify (not-nil? (some #{cert} certdirfiles)))
      (verify (not-nil? (some #{key} certdirfiles))))
    (reset! importedcert
            {:certlocation certlocation
             :certdir certdir
             :cert cert
             :key key
             :entname entname})))
  
(defn ^{Test {:groups ["import"]
              :dependsOnMethods ["import_cert"]}}
  import_unsubscribe [_]
  (tasks/register-with-creds :re-register? false)
  (tasks/ui selecttab :my-subscriptions)
  (let [assert-in-table? (fn [pred]
                           (pred
                            (some #{(:entname @importedcert)}
                                  (tasks/get-table-elements :my-subscriptions-view
                                                            0
                                                            :skip-dropdowns? true))))]
    (assert-in-table? not-nil?)
    (tasks/unregister)
    (assert-in-table? nil?))
  (let [certdirfiles (split-lines
                      (.getStdout (.runCommandAndWait
                                   @clientcmd
                                   (str "ls " (:certdir @importedcert)))))
        does-not-exist? (fn [file]
                          (nil? (some #{file} certdirfiles)))]
    (verify (does-not-exist? (:cert @importedcert)))
    (verify (does-not-exist? (:key @importedcert))))
  (reset! importedcert nil))


(gen-class-testng)

(comment
  ;; to run this in the REPL, do this:
  (do
    (import '[com.redhat.qe.sm.cli.tests ImportTests])
    (def importtests (atom nil))
    (def importedcert (atom nil))
    (reset! importtests (ImportTests.))
    (.setupBeforeSuite @importtests)
    (.setupBeforeClass @importtests))

  )


; TODO https://bugzilla.redhat.com/show_bug.cgi?id=702075
; TODO 
