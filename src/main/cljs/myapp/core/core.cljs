(ns myapp.core.core
  (:require [reagent.dom.client :as rdc]
            [re-frame.core :as rfc]
            [reitit.core :as reititcore]
            [reitit.coercion.spec :as reititcoe]
            [reitit.frontend :as reititfrontend]

            [reitit.frontend.easy :as reititeasy]
            [myapp.core.root :refer [root-container]]
            [myapp.core.events :as events]
            [myapp.core.routes :refer [routes]]))



;;; Routes ;;;
(defn href
      "Return relative url for given route. Url can be used in HTML links."
      ([k]
       (href k nil nil))
      ([k params]
       (href k params nil))
      ([k params query]
       (reititeasy/href k params query)))

(defn on-navigate [new-match]
      (when new-match
            (rfc/dispatch [::events/navigated new-match])))

(def router
  (reititfrontend/router
    routes
    {:data {:coercion reititcoe/coercion}}))

(defn init-routes! []
      (js/console.log "initializing routes")
      (reititeasy/start!
        router
        on-navigate
        {:use-fragment true}))

(defn nav [{:keys [router current-route]}]
      (js/console.log "Current route:" current-route)
      [:nav.breadcrumb {:aria-label "breadcrumbs" :translate "no"}
       [:ul
        (for [route-name (reititcore/route-names router)
              :let [route (reititcore/match-by-name router route-name)
                    text (-> route :data :link-text)]]
             (do (js/console.log "Route:" route "Text:" text)
                 [:li {:key route-name}
                  (when (= route-name (-> current-route :data :name))
                        "> ")
                  ;; Create a normal links that user can click
                  [:a {:href (href route-name)} text]]))]])

(defn router-component [{:keys [router]}]
      (let [current-route @(rfc/subscribe [::events/current-route])]
           [:div
            [nav {:router router :current-route current-route}]
            (when current-route
                  [(-> current-route :data :view)])]))
;; end import reitit route

(defn run                                                   ;; Your app calls this when it starts. See shadow-cljs.edn :init-fn.
      []
      (rfc/clear-subscription-cache!)
      (rfc/dispatch-sync [::events/initialize-db])          ;; put a value into application state
      (init-routes!)                                        ;; Reset routes on figwheel reload
      ;(.render root-container [router-component {:router router}])
      (rdc/render root-container [router-component {:router router}]
                  )
      ;(mount-ui)
      )

(defn ^:dev/after-load clear-cache-and-render!
      []
      ;; The `:dev/after-load` metadata causes this function to be called
      ;; after shadow-cljs hot-reloads code. We force a UI update by clearing
      ;; the Reframe subscription cache.
      (rfc/clear-subscription-cache!)
      )
(run)
