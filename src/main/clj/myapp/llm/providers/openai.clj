(ns myapp.llm.providers.openai
  "OpenAI API integration."
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

(def ^:private ^:const api-endpoint
  ;;"https://api.openai.com/v1/chat/completions"
  "https://api.siliconflow.cn/v1/chat/completions"
  )

(def ^:private api-key
  (System/getenv "SILICONFLOW_API_KEY"))

(s/def ::api-response
  (s/keys :req-un [::choices]))

(defn chat!
  "Send a chat message to OpenAI and return the response."
  [message]
  {:pre [(string? message)]}
  (when (nil? api-key)
    (throw (ex-info "OPENAI_API_KEY not set" {:type :config-error})))
  
  (log/info "Sending message to OpenAI:" message)
  (try
    (let [response (http/post api-endpoint
                    {:headers {"Authorization" (str "Bearer " api-key)
                             "Content-Type" "application/json"}
                     :body (json/generate-string
                            {:model "deepseek-ai/DeepSeek-V3"
                             :messages [{:role "user"
                                       :content message}]})
                     :as :json})]
      (log/info "Received response from OpenAI")
      (let [content (-> response
                       :body
                       :choices
                       first
                       :message
                       :content)]
        (println content)  ;; 直接输出到控制台
        content))
    (catch Exception e
      (log/error e "Failed to get response from OpenAI")
      (throw (ex-info "Failed to get response from OpenAI"
                     {:type :api-error
                      :cause (.getMessage e)})))))

(comment
  (chat! "Hello, world!")
  (chat! ""))
