(defproject org.clojars.jsefler/sm "1.0.0-SNAPSHOT"
  :description "Automated tests for Red Hat Subsciption Manager CLI and GUI"
  :java-source-path "src"
  :aot [#"^com.redhat.qe.sm.gui.tests"] ;regex to find tests that testng will run
  :keep-non-project-classes true
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [error.handler "1.0.0-SNAPSHOT"]
                 [net.java.dev.rome/rome "1.0.0"]
                 [org.jdom/jdom "1.1"]
                 [postgresql/postgresql "8.4-701.jdbc4"]
                 [webui-framework "1.0.2-SNAPSHOT"]
                 [gnome.ldtp "1.0.0-SNAPSHOT"]
                 [test_clj.testng "1.0.1-SNAPSHOT"]
                 [clj-http "0.1.3"]
                 [matchure "0.10.1"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :repositories {"clojars.org" {:url "http://clojars.org/repo"
                                :snapshots {:update :always}}}
  :javac-options {:debug "on"})
  
  
(comment
  (do   
    (do 
      (require :reload-all '[com.redhat.qe.sm.gui.tasks.test-config :as config])
      (require :reload-all '[com.redhat.qe.sm.gui.tasks.tasks :as tasks])
      (require :reload-all '[com.redhat.qe.sm.gui.tasks.candlepin-tasks :as ctasks])
      (require :reload-all '[com.redhat.qe.sm.gui.tasks.rest :as rest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.subscription-assistant-tests :as satest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.subscribe-tests :as stest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.register-tests :as rtest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.proxy-tests :as ptest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.rhn-interop-tests :as ritest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.autosubscribe-tests :as atest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.firstboot-tests :as fbtest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.facts-tests :as ftest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.acceptance-tests :as actest])
      (require :reload-all '[com.redhat.qe.sm.gui.tests.import-tests :as itest])

      (import '[com.redhat.qe.sm.base SubscriptionManagerCLITestScript])
      )

    (let [cliscript (SubscriptionManagerCLITestScript.)]
      (.setupBeforeSuite cliscript))
    
    (do
      (config/init)
      (tasks/connect)
      (use 'gnome.ldtp))
    )     ;<< here for all of it
  
)
