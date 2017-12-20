(defproject org.clojars.jsefler/sm "1.1.0-SNAPSHOT"
  :description "Automated tests for Red Hat Subsciption Manager CLI and GUI"
  :java-source-paths ["src"]
  :main rhsm.runtestng
  :aot [#"^rhsm.gui.tests" #"^rhsm.api.tests" #"^rhsm.cockpit.tests" rhsm.runtestng] ;regex to find tests that testng will run
  :keep-non-project-classes true
  :dependencies [[clj-http "2.0.0"]
                 [com.google.code.guice/guice "1.0"]        ;; required for new testng
                 [com.redhat.qe/assertions "1.0.2"]
                 [com.redhat.qe/bugzilla-testng "1.2.4-SNAPSHOT"
                  :exclusions [com.redhat.qe/bz-checker]]
                 [com.redhat.qe/bz-checker "2.1.3-SNAPSHOT"]
                 [com.redhat.qe/json-java "20110202"]
                 [com.redhat.qe/jul.test.records "1.0.1"]
                 [com.redhat.qe/ssh-tools "1.0.3-SNAPSHOT"]
                 [com.redhat.qe/testng-listeners "1.0.0"
                  :exclusions [com.redhat.qe/bugzilla-testng]]
                 [com.redhat.qe/verify-testng "1.0.0-SNAPSHOT"]
                 [com.redhat.qe/rhsm-dbus "0.9.1-SNAPSHOT"]
                 [gnome.ldtp "1.2.1-SNAPSHOT"]
                 [matchure "0.10.1"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.jdom/jdom "1.1"]
                 [org.testng/testng "6.8.21"]
                 [org.uncommons/reportng "1.1.4"
                  :exclusions [org.testng/testng]]
                 [postgresql/postgresql "8.4-701.jdbc4"]
                 [slingshot "0.8.0"]
                 [cheshire "5.7.0"]
                 [test-clj.testng "1.1.0-SNAPSHOT"]
                 [levand/immuconf "0.1.0"]
                                        ;[com.github.redhatqe.polarize/polarize "0.7.1-SNAPSHOT"]
                 [com.github.redhatqe.polarize/polarize-meta "0.1.0"]
                 [com.github.redhatqe.polarizer/reporter "0.2.1"]
                 [org.json/json "20160810"]
                 [org.seleniumhq.selenium/selenium-java "3.5.2"]
                 [http-kit "2.2.0"]
                 [mount "0.1.10"]]
  :profiles {:dev {:dependencies
                   [[fn.trace "1.3.2.0-SNAPSHOT"]]}}
  :plugins [[lein2-eclipse "2.0.0"]
            [quickie "0.4.1"]]
  ; regexp of namespaces that contains of tests of our tesgit@github.com:RedHatQE/rhsm-qe.gitts. It is used by quickie.
  :test-matcher #"rhsm\..*-test$"
  :repositories [
                 ["sonatype" {:url           "https://oss.sonatype.org/content/repositories/releases"
                              ;; If a repository contains releases only setting
                              ;; :snapshots to false will speed up dependencies.
                              :snapshots     false
                              ;; Disable signing releases deployed to this repo.
                              ;; (Not recommended.)
                              :sign-releases true
                              ;; You can also set the policies for how to handle
                              ;; :checksum failures to :fail, :warn, or :ignore.
                              :checksum      :fail
                              ;; How often should this repository be checked for
                              ;; snapshot updates? (:daily, :always, or :never)
                              :update        :always}]
                 ["sonatype-snaps" {:url     "https://oss.sonatype.org/content/repositories/snapshots"
                              ;; If a repository contains releases only setting
                              ;; :snapshots to false will speed up dependencies.
                              :snapshots     true
                              ;; Disable signing releases deployed to this repo.
                              ;; (Not recommended.)
                              :sign-releases false
                              ;; You can also set the policies for how to handle
                              ;; :checksum failures to :fail, :warn, or :ignore.
                              :checksum      :warn
                              ;; How often should this repository be checked for
                              ;; snapshot updates? (:daily, :always, or :never)
                                    :update        :always}]
                 ["jcenter" {:url "https://jcenter.bintray.com"}]
                 ]
  :javac-options ["-target" "1.8" "-source" "1.8" "-parameters"]
  ;; uncomment this to remote debug.  This is useful to debug annotation processing or xunit importing which can't be
  ;; handled through the regular clojure debugger
  ;:jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007"]

  :manifest {"Class-Path" "lib/clojure-1.8.0.jar"}

  :repl-options {:timeout 120000})

(comment
  (do
    (use '[clojure.repl])
    (use '[clojure.pprint])
    (use '[slingshot.slingshot :only (try+ throw+)])
    (require '[clojure.tools.logging :as log])
    (do
      (use :reload-all '[rhsm.gui.tasks.tools])
      (require :reload-all '[rhsm.gui.tasks.test-config :as config])
      (require :reload-all '[rhsm.gui.tasks.tasks :as tasks])
      (require :reload-all '[rhsm.gui.tasks.candlepin-tasks :as ctasks])
      (require :reload-all '[rhsm.gui.tasks.rest :as rest])
      (require :reload-all '[rhsm.gui.tests.base :as base])
      (require :reload-all '[rhsm.gui.tests.subscribe_tests :as stest])
      (require :reload-all '[rhsm.gui.tests.register_tests :as rtest])
      (require :reload-all '[rhsm.gui.tests.proxy_tests :as ptest])
      (require :reload-all '[rhsm.gui.tests.rhn_interop_tests :as ritest])
      (require :reload-all '[rhsm.gui.tests.autosubscribe_tests :as atest])
      (require :reload-all '[rhsm.gui.tests.firstboot_tests :as fbtest])
      (require :reload-all '[rhsm.gui.tests.firstboot_proxy_tests :as fptest])
      (require :reload-all '[rhsm.gui.tests.facts_tests :as ftest])
      (require :reload-all '[rhsm.gui.tests.import_tests :as itest])
      (require :reload-all '[rhsm.gui.tests.system_tests :as systest])
      (require :reload-all '[rhsm.gui.tests.stacking_tests :as stktest])
      (require :reload-all '[rhsm.gui.tests.repo_tests :as reptest])
      (require :reload-all '[rhsm.gui.tests.product_status_tests :as pstest])
      (require :reload-all '[rhsm.gui.tests.subscription_status_tests :as substattest])
      (require :reload-all '[rhsm.gui.tests.search_status_tests :as sstattest])
      (import '[rhsm.base SubscriptionManagerCLITestScript]))

    (let [cliscript (SubscriptionManagerCLITestScript.)]
      (.setupBeforeSuite cliscript))

    (do
      (config/init)
      (tasks/connect)
      (use 'gnome.ldtp))
    (log/info "INITIALIZATION COMPLETE!!")
    "INITIALIZATION COMPLETE!!")      ;<< Here for all of it

  ;not used
  (require :reload-all '[rhsm.gui.tests.subscription-assistant-tests :as satest])
  (require :reload-all '[rhsm.gui.tests.acceptance_tests :as actest]))
