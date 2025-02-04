(ns myapp.utils.test-utils
  (:require [myapp.utils.core-util :refer [*db-dry-run*]]
            [myapp.db.conn-db :as conn]))

;; Set db-dry-run flag for test environment
(alter-var-root #'myapp.utils.core-util/*db-dry-run* (constantly true))

;; Test transaction macro that always rolls back
(defmacro with-test-tx [& body]
  `(conn/with-transaction [tx# {:rollback-only true}]
     ~@body))