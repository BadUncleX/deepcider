(ns myapp.cli.core-test
  (:require [clojure.test :refer :all]
            [myapp.cli.core :as core]
            [myapp.cli.parser :as parser]
            [clojure.spec.test.alpha :as stest]))

(deftest command-parsing-test
  (testing "Basic command parsing"
    (let [result (parser/parse-args ["chat" "hello"])]
      (is (= "chat" (:command result)))
      (is (= ["hello"] (:args result)))
      (is (map? (:options result)))))
  
  (testing "Help command"
    (let [result (parser/parse-args ["--help"])]
      (is (= "help" (:command result)))
      (is (empty? (:args result)))
      (is (:help (:options result)))))
  
  (testing "Empty arguments"
    (let [result (parser/parse-args [])]
      (is (nil? (:command result)))
      (is (empty? (:args result)))
      (is (map? (:options result)))))
  
  (testing "Multiple arguments"
    (let [result (parser/parse-args ["chat" "hello" "world"])]
      (is (= "chat" (:command result)))
      (is (= ["hello" "world"] (:args result)))
      (is (map? (:options result))))))

(deftest chat-command-test
  (testing "Chat command execution"
    (with-redefs [myapp.llm.providers.openai/chat!
                  (constantly "Test response")]
      (let [response (core/handle-command
                      {:command "chat"
                       :args ["hello"]})]
        (is (string? response))
        (is (= "Test response" response))))))

(comment
  (run-tests))
