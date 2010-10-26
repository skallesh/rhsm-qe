(ns sm.gui.ui)

;;A map of keywords to the GNOME ui data
(def windows {:mainWindow  {:id "manage_subscriptions_dialog"
                            :elements {:close-main "button_close"
                                       :add "add_button"
                                       :registration "account_settings"}}
              :registerDialog {:id "register_dialog"
                               :elements {:redhat-login "account_login"
                                          :password "accound_password"
                                          :system-name "consumer-name"
                                          :automatically-subscribe "auto_bind"
                                          :register "register_button"}}
              :registrationSettingsDialog {:id "register_token_dialog"
                                           :elements {:registration-token "regtoken-entry"}}
              :errorDialog "Error"
              :questionDialog "Question"
              :factsDialog "facts_dialog"
              :subscribeDialog "dialog_add"})


        
