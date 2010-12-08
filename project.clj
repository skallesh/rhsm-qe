(defproject sm "1.0.0-SNAPSHOT"
  :description "Automated tests for Red Hat Subsciption Manager GUI"
  :source-path "clojure/src"
  :java-source-path "src"
  :aot [#"^sm.gui.tests"]
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [org.apache.xmlrpc/xmlrpc-client "3.1.3"]
		 [net.java.dev.rome/rome "1.0.0"]
		 [org.jdom/jdom "1.1"]
		 [postgresql/postgresql "8.4-701.jdbc4"]
		 [webui-framework "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [lein-javac "1.2.1-SNAPSHOT"]
		     [lein-eclipse "1.0.0"]])
