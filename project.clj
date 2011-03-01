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
                 [gnome.ldtp "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [lein-eclipse "1.0.0"]]
  :repositories ["hudson" "http://hudson.rhq.lab.eng.bos.redhat.com:8080/archiva/repository/hudson"])
