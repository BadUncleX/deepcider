(ns myapp.core.core-core
    (:require [ring.adapter.jetty9 :as jetty]
      [myapp.handler.core-handler :refer [app]]
      [clojure.tools.logging :as log]))

(defn -main [& args]
      (log/info "Starting server...")
      (jetty/run-jetty app {:port 4000
                           :join? false
                           :async? true
                           :async-timeout 30000
                           :min-threads 10
                           :max-threads 50})
      (log/info "Server running on port 4000"))