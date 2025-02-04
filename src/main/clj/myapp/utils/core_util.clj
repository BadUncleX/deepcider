(ns myapp.utils.core-util
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.tools.logging :as log]))

(def ^:dynamic *db-dry-run* false)

(defn dry-run? []
  *db-dry-run*)

(defn current-timestamp []
  (let [timezone (t/time-zone-for-offset 8)]
    (f/unparse (f/with-zone (f/formatter "yyyy-MM-dd HH:mm:ss") timezone) (t/now))))

;; satisfy the requirement of coll and common variable
(defn Option [expr default]
  (if (or (and (coll? expr) (seq expr))
          (and (not (coll? expr)) (some? expr)))
    expr
    default))

(defn contains-chinese? [s]
  (boolean (re-find #"\p{InCJK_Unified_Ideographs}" s)))


(defn is-valid-word [word]
  (let [cleaned-word (clojure.string/replace word #"^\"|\"$" "") ;; 去掉首尾的双引号
        cleaned-word (clojure.string/trim cleaned-word)             ;; 去掉首尾的空格
        cleaned-word (clojure.string/replace cleaned-word #"[,.!)?]+$" "") ;; 去掉末尾的逗号、句号、问号、感叹号
        matcher (re-matcher #"^[a-z]+$" cleaned-word)]
    (if (.find matcher)
      cleaned-word
      nil)))

(defn remove-trailing-punctuation [word]
  (let [cleaned-word (clojure.string/replace word #"^\"|\"$" "") ;; 去掉首尾的双引号
        cleaned-word (clojure.string/trim cleaned-word)             ;; 去掉首尾的空格
        cleaned-word (clojure.string/replace cleaned-word #"[,.!?]+$" "") ;; 去掉末尾的逗号、句号、问号、感叹号
        cleaned-word (clojure.string/replace cleaned-word #"^\(" "") ;; 去掉开头的括号
        ]
    cleaned-word))
(defn validate-json-again [input-str]
  (try
    (let [pattern #"\{[\s\S]*\}"  ;; 匹配 {} 包裹的内容
          json-content (re-find pattern input-str)]  ;; 提取 JSON 内容
      (if json-content
        (json/parse-string json-content true)
        (do
          (log/error (str "No valid JSON object found in input: " input-str))
          {})))
    (catch Exception e
      (log/error (str "Invalid JSON format for input: " input-str))
      (log/error (str "Exception message: " (.getMessage e)))
      {})))

(defn validate-json-again-batch [input-str]
  (try
    (let [pattern #"\[[\s\S]*\]"  ;; 匹配 [] 包裹的内容
          json-content (re-find pattern input-str)]  ;; 提取 JSON 内容
      (if json-content
        (json/parse-string json-content true)
        (do
          (log/error (str "No valid JSON object found in input: " input-str))
          {})))
    (catch Exception e
      (log/error (str "Invalid JSON format for input: " input-str))
      (log/error (str "Exception message: " (.getMessage e)))
      {})))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn parse-clj-object-from-json-batch [json-str]
  (try
    (json/parse-string json-str true)
    (catch Exception e
      (log/error (str "Invalid JSON format for input: " json-str))
      (log/error (str "Exception message: " (.getMessage e)))
      (log/info "ReValidate json again!")
      ;; (validate-json-again-batch json-str)
      )))

(defn parse-clj-object-from-json [json-str]
  (try
    (json/parse-string json-str true)
    (catch Exception e
      (log/error (str "Invalid JSON format for input: " json-str))
      (log/error (str "Exception message: " (.getMessage e)))
      (log/info "ReValidate json again!")
      (validate-json-again json-str)
      )))




