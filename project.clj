(defproject sm "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  :source-path "clojure/src"
  :java-source-path [["../../webui-framework/src"]["src"]]
  :repositories {"apacherepository" "http://repository.apache.org/snapshots/"}
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [org.apache.xmlrpc/xmlrpc-client "3.1.3"]
		 [webui-framework "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
		     [lein-javac "1.2.1-SNAPSHOT"] ])
