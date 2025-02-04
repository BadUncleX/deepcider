(ns myapp.core.config)

(def prod-api-base-url "https://api.myapp.com")
(def dev-api-base-url "http://localhost:4000/api")

(defn _get_api_from_env []
      (if js/goog.DEBUG
        dev-api-base-url
        prod-api-base-url))

(def api-address (_get_api_from_env))