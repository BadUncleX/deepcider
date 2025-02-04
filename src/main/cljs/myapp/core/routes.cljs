(ns myapp.core.routes
  (:require [myapp.clock.views :as clock]
            [myapp.core.views :refer [home-page sub-page1 sub-page2]]))

(def routes
  ["/"
   ["" {:name :home
        :view home-page
        :link-text "Home"
        :controllers [{:start #_{:clj-kondo/ignore [:unused-binding]}
                              (fn [& params] (js/console.log "Entering home page"))
                       :stop (fn [& params] (js/console.log "Leaving home page"))}]}]
   ["sub-page1" {:name :sub-page1
                 :view sub-page1
                 :link-text "Sub page 1"
                 :controllers [{:start (fn [& params] (js/console.log "Entering sub-page 1"))
                                :stop (fn [& params] (js/console.log "Leaving sub-page 1"))}]}]
   ["sub-page2" {:name :sub-page2
                 :view sub-page2
                 :link-text "Sub-page 2"
                 :controllers [{:start (fn [& params] (js/console.log "Entering sub-page 2"))
                                :stop (fn [& params] (js/console.log "Leaving sub-page 2"))}]}]
   ["Clock" {:name :clock
             :view clock/ui
             :link-text "Colorful Clock"
             :controllers [{:start (fn [& params] (js/console.log "Entering clock"))
                            :stop (fn [& params] (js/console.log "Leaving clock"))}]}]


   ])