(ns myapp.cli.commands.chat
  "Chat command implementation."
  (:require [myapp.llm.providers.openai :as openai]
            [clojure.spec.alpha :as s]))

(s/def ::message string?)
(s/def ::response string?)

(defn execute!
  "Execute chat command with given arguments.
   Returns the AI response as a string."
  [args]
  {:pre [(s/valid? (s/coll-of string?) args)]
   :post [(s/valid? ::response %)]}
  (let [message (clojure.string/join " " args)]
    (if (empty? message)
      (throw (ex-info "Message cannot be empty" {:type :validation-error}))
      (openai/chat! message))))

(comment
  (execute! ["Hello" "world"])
  (execute! []))
