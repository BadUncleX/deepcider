(ns myapp.handler.core-handler
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [muuntaja.core :as m]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [hiccup2.core :refer [html]]))

(defn error-response
  "Create an error response with status and optional details"
  ([status message]
   (error-response status message nil))
  ([status message details]
   {:status status
    :body (merge {:error message}
                (when details {:details details}))}))

(defn success-response
  "Create a success response with optional custom status"
  ([body]
   (success-response 200 body))
  ([status body]
   {:status status
    :body body}))

(defn sandbox [request respond raise]
  (try
    (respond {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (html
                 [:div
                  [:h1 "Sandbox"]
                  [:p "This is a sandbox page."]])
               str)})
    (catch Exception e
      (raise e))))

(defn wrap-sync-async-params [handler]
  (fn
    ([request]
     (handler request))
    ([request respond raise]
     (handler request respond raise))))

(def app
  (-> (ring/ring-handler
       (ring/router
         [["/api"
           ;;["/process-words" {:post process-words}]
           ["/sandbox" {:get sandbox}]]]
         {:data {:muuntaja m/instance
                :middleware [wrap-sync-async-params
                             parameters/parameters-middleware
                            muuntaja/format-middleware
                            [wrap-cors
                             :access-control-allow-origin [#".*"]
                             :access-control-allow-methods [:get :put :delete :post]]
                             ]}})
       (ring/create-default-handler
         {:not-found (fn [_] {:status 404 :body "Not found"})
          :method-not-allowed (fn [_] {:status 405 :body "Method not allowed"})
          :not-acceptable (fn [_] {:status 406 :body "Not acceptable"})}))
      wrap-keyword-params
      wrap-params))
