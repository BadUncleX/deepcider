(ns myapp.cli.parser
  "Command line argument parsing utilities."
  (:require [clojure.tools.cli :as cli]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(def ^:private cli-options
  [["-h" "--help" "Show help"]
   ["-v" "--verbose" "Verbose mode"]])

;; Specs for command line arguments
(s/def ::args (s/coll-of string?))
(s/def ::command (s/nilable string?))  ;; 允许nil，因为空参数的情况
(s/def ::options map?)
(s/def ::parse-result
  (s/keys :req-un [::command ::args ::options]))

(defn parse-args
  "Parse command line arguments into a structured format.
   Returns: {:command string :args [string] :options map}"
  [args]
  {:pre [(s/valid? ::args args)]
   :post [(s/valid? ::parse-result %)]}
  (let [{:keys [options arguments errors]}
        (cli/parse-opts args cli-options)]
    (cond
      errors
      (throw (ex-info "Invalid command line arguments" 
                     {:type :parse-error
                      :errors errors}))
      
      (:help options)
      {:command "/help"
       :args []
       :options options}
      
      (empty? arguments)  ;; 处理空参数的情况
      {:command nil
       :args []
       :options options}
      
      :else
      (let [cmd (first arguments)]
        (when-not (str/starts-with? cmd "/")
          (throw (ex-info "Commands must start with /" 
                         {:command cmd})))
        {:command cmd
         :args (vec (rest arguments))  ;; 确保args是vector
         :options options}))))

(comment
  (parse-args ["/chat" "Hello"])
  (parse-args ["--help"])
  (parse-args []))
