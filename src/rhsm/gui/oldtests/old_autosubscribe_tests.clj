
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OLD TESTS NO LONGER IN USE ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  ;; tests no longer usable in the GUI
  (defn ^{Test {:groups ["autosubscribe"]}}
    register_autosubscribe [_]
    (let [beforesubs (tasks/warn-count)
          user (@config :username)
          pass (@config :password)
          key  (@config :owner-key)
          ownername (if (= "" key)
                      nil
                      (ctasks/get-owner-display-name user pass key))]
      (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do
          (tasks/register user
                          pass
                          :skip-autosubscribe false
                          :owner ownername)
          (verify (<= (tasks/warn-count) beforesubs))))))

  (defn ^{Test {:groups ["autosubscribe"
                         "configureProductCertDirForSomeProductsSubscribable"]
                :dependsOnMethods ["register_autosubscribe"]}}
    some_products_subscribable [_]
    (.runCommandAndWait @clientcmd "subscription-manager unregister")
    (tasks/restart-app)
    (verify (dirsetup? somedir))
    (let [beforesubs (tasks/warn-count)
          dircount (trim (.getStdout
                          (.runCommandAndWait
                           @clientcmd
                           (str "ls " somedir " | wc -l"))))
          user (@config :username)
          pass (@config :password)
          key  (@config :owner-key)
          ownername (ctasks/get-owner-display-name user pass key)]
      (verify (= (str beforesubs)
                 dircount))
      (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do
          (tasks/register user
                          pass
                          :skip-autosubscribe false
                          :owner ownername)
          (tasks/ui waittillwindownotexist :register-dialog 600)
          (tasks/sleep 20000)
          (verify (<= (tasks/warn-count) beforesubs))
          (verify (not (tasks/compliance?)))))))

  (defn ^{Test {:groups ["autosubscribe"
                         "configureProductCertDirForAllProductsSubscribable"]
                :dependsOnMethods ["register_autosubscribe"]}}
    all_products_subscribable [_]
    (.runCommandAndWait @clientcmd "subscription-manager unregister")
    (tasks/restart-app)
    (verify (dirsetup? alldir))
    (let [beforesubs (tasks/warn-count)
          user (@config :username)
          pass (@config :password)
          key  (@config :owner-key)
          ownername (ctasks/get-owner-display-name user pass key)]
      (verify (= (str beforesubs)
                 (trim (.getStdout
                        (.runCommandAndWait @clientcmd (str "ls " alldir " | wc -l"))))))
      (if (= 0 beforesubs)
        (verify (tasks/compliance?))
        (do
          (tasks/register user
                          pass
                          :skip-autosubscribe false
                          :owner ownername)
          (tasks/ui waittillwindownotexist :register-dialog 600)
          (tasks/sleep 20000)
          (verify (= (tasks/warn-count) 0))
          (verify (tasks/compliance?))))))

  )
