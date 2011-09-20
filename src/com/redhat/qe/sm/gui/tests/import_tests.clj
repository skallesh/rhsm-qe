(ns com.redhat.qe.sm.gui.tests.import-tests
  (:use [test-clj.testng :only (gen-class-testng data-driven)]
        [com.redhat.qe.sm.gui.tasks.test-config :only (config)]
        [com.redhat.qe.verify :only (verify)]
        [error.handler :only (with-handlers handle ignore recover)]
        [clojure.contrib.string :only (split)]
        gnome.ldtp)
  (:require [com.redhat.qe.sm.gui.tasks.tasks :as tasks]
             com.redhat.qe.sm.gui.tasks.ui)
  (:import [org.testng.annotations BeforeClass BeforeGroups Test DataProvider]
           [com.redhat.qe.sm.cli.tests ImportTests]))

(def importtests (atom nil))

(defn ^{BeforeClass {:groups ["setup"]}}
  create_certs [_]
  (reset! importtests (ImportTests.))
  (.setupBeforeClass @importtests))

(defn ^{Test {:groups ["import"]}}
  import_cert [_]
  (let [certlocation (str (.getValidImportCertificate @importtests))]
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
    (tasks/ui click :info-ok)))

(comment 
  (defn ^{Test {:groups ["import"]
                :dependsOnMethods ["import_cert"]}}
    check_import [_]
    ))


(gen-class-testng)

(comment
  ;; to run this in the REPL, do this:
  (do
    (import '[com.redhat.qe.sm.cli.tests ImportTests])
    (def importtests (atom nil))
    (reset! importtests (ImportTests.))
    (.setupBeforeSuite @importtests)
    (.setupBeforeClass @importtests))

  )
