(defproject sm "1.0.0-SNAPSHOT"
  :description "Automated tests for Red Hat Subsciption Manager GUI"
  :java-source-path "src"
  :aot [#"^com.redhat.qe.sm.gui.tests"] ;regex to find tests that testng will run
  :keep-non-project-classes true
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [error.handler "1.0.0-SNAPSHOT"]
		 [net.java.dev.rome/rome "1.0.0"]
		 [org.jdom/jdom "1.1"]
		 [postgresql/postgresql "8.4-701.jdbc4"]
		 [webui-framework "1.0.1-SNAPSHOT"]
                 [gnome.ldtp "1.0.0-SNAPSHOT"]
                 [test_clj.testng "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]]
  :repositories ["hudson" "http://hudson.rhq.lab.eng.bos.redhat.com:8080/archiva/repository/hudson"])
  
  
(comment 
(do 
  (require :reload-all '[com.redhat.qe.sm.gui.tasks.test-config :as config])
  (require :reload-all '[com.redhat.qe.sm.gui.tasks.tasks :as tasks])
  (require :reload-all '[com.redhat.qe.sm.gui.tests.compliance-assistant-tests :as catest])  
  (require :reload-all '[com.redhat.qe.sm.gui.tests.subscribe-tests :as stest])
  (require :reload-all '[com.redhat.qe.sm.gui.tests.register-tests :as rtest])
  (require :reload-all '[com.redhat.qe.sm.gui.tests.proxy-tests :as ptest])
  (require :reload-all '[com.redhat.qe.sm.gui.tests.rhn-interop-tests :as ritest])
  (require :reload-all '[com.redhat.qe.sm.gui.tests.autosubscribe-tests :as atest]))  
)
