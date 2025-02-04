(ns myapp.utils.core
  (:require [clojure.string :as str]))

(defn Option [expr default]
  (if (or (and (coll? expr) (not-empty expr))
          (and (not (coll? expr)) (some? expr)))
    expr
    default))

(defn re-seq-iter [re s]
      (let [match-data (atom (.exec re s))]
           (reify
             ISeqable
             (-seq [_]
                   (when @match-data
                         (cons @match-data
                               (re-seq-iter (js/RegExp. (.-source re) "g") s))))
             ISeq
             (-first [_] @match-data)
             (-rest [this]
                    (reset! match-data (.exec re s))
                    (or (seq this) ())))))

(defn re-matcher [re s]
      (re-seq-iter (js/RegExp. (.-source re) "g") s))

(defn is-valid-word [word]
      (let [cleaned-word (-> word
                             (str/replace #"^\"|\"$" "") ;; 去掉首尾的双引号
                             str/trim                    ;; 去掉首尾的空格
                             (str/replace #"[,.!?)]+$" "") ;; 去掉末尾的逗号、句号、问号、感叹号
                             (str/replace #"^\(" "");; 去掉开头的括号

                             str/lower-case)             ;; 转换为小写（可选，取决于你的需求）
            valid? (re-matches #"^[a-z]+$" cleaned-word)]
           (when valid?
                 cleaned-word)))