(ns myapp.cli.integration-test
  (:require [clojure.test :refer :all]
            [myapp.cli.core :as core]
            [clojure.java.shell :refer [sh]]))

(deftest ^:integration cli-integration-test
  (testing "CLI execution"
    (let [{:keys [out exit]}
          (sh "clojure" "-M" "-m" "myapp.cli.core" "chat" "hello")]
      (is (zero? exit))
      (is (string? out)))))

(comment
  (run-tests))
