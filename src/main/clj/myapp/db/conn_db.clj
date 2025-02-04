;; src/main/clj/myapp/db/conn.clj
(ns myapp.db.conn-db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            [next.jdbc.result-set :as result-set]
            [next.jdbc.transaction :as tx-api]
            [myapp.utils.rop-util :refer [Ok Err None UnwrapOr]]
            [myapp.utils.core-util :refer [*db-dry-run*]]
            [clojure.tools.logging :as log])
  (:import [com.zaxxer.hikari HikariDataSource]))

;; Base SQLite settings
(def base-sqlite-opts
  {:foreign_keys "true"
   :journal_mode "WAL"
   :busy_timeout "10000"
   :synchronous "NORMAL"})

;; Production connection pool configuration
(def db-spec
  (merge
    {:dbtype "sqlite"
     :dbname "mywordsNew.sqlite"
     :maximumPoolSize 5
     :minimumIdle 1
     :connectionTimeout 30000
     :idleTimeout 600000
     :maxLifetime 1800000}
    base-sqlite-opts))

;; Test connection pool configuration
(def test-db-spec
  (merge
    {:dbtype "sqlite"
     :dbname "mywordsNew.sqlite"
     :maximumPoolSize 2
     :minimumIdle 1
     :connectionTimeout 5000
     :idleTimeout 300000
     :maxLifetime 900000}
    base-sqlite-opts))

;; Create datasource based on environment
(defonce datasource
  (connection/->pool HikariDataSource 
    (if *db-dry-run*
      test-db-spec
      db-spec)))

;; Helper function to create a test datasource
(defn create-test-datasource []
  (let [ds-config (merge test-db-spec
                        {:auto-commit false  ;; Disable auto-commit
                         :connection-init-sql "PRAGMA foreign_keys = ON;"})]
    (log/info "Creating test datasource with config:" ds-config)
    (connection/->pool HikariDataSource ds-config)))

(defn unqualify-keys
  "Removes the namespace from map keys."
  [result]
  (into {} (map (fn [[k v]] [(keyword (name k)) v]) result)))

;; Core database operation abstractions with ROP
(defn query-one
  "Execute a query expecting a single result.
   Returns:
   - Ok with result if found
   - None if no result
   - Err if database error"
  ([sql]
   (query-one datasource sql))
  ([conn sql]
   (try
     (if-let [result (jdbc/execute-one! conn sql {:builder-fn result-set/as-unqualified-maps})]
       (Ok result)
       None)
     (catch Exception e
       (log/error e "Database error executing query:" sql)
       (Err :database-error
            "Database error occurred"
            {:sql sql
             :error (.getMessage e)})))))


(defn query-many [sql]
  (try
    (if-let [results (jdbc/execute! db-spec sql {:builder-fn result-set/as-unqualified-maps})]
      (Ok results)
      None)
    (catch Exception e
      (Err :database-error
           "Database query failed"
           {:sql sql
            :error-message (.getMessage e)}))))

(defn execute!
  "Execute a database modification operation.
   Parameters:
   - conn: (optional) A transaction connection; if nil, uses the default datasource.
   - sql: The SQL statement and parameters.
   Returns:
   - Ok with update count on success
   - Err if a database error occurs"
  ([sql]
   (execute! nil sql)) ; Calls the version with conn as nil (default datasource)
  ([conn sql]
   (try
     (let [result (jdbc/execute! (or conn datasource) sql {:builder-fn result-set/as-unqualified-maps})]
       (Ok (map unqualify-keys result))) ;
     (catch Exception e
       (log/error e "Database error executing update:" sql)
       (Err :database-error
            "Database error occurred"
            {:sql sql
             :error (.getMessage e)})))))

(defn get-update-count-int [result]
  (:update-count (first (UnwrapOr result [{}]))))

(defn with-transaction*
  "Execute operations in a transaction.
   Takes a function that receives a transaction connection.
   Returns the result of the function wrapped in Ok/Err."
  [f opts]
  (try
    (jdbc/with-transaction [tx datasource opts]
      (f tx))
    (catch Exception e
      (log/error e "Transaction error")
      (Err :transaction-error
           "Transaction failed"
           {:error (.getMessage e)}))))

;; Transaction macro for better syntax
(defmacro with-transaction
  "Execute body in a transaction.
   Makes tx binding available to body.
   Returns Ok/Err."
  [[tx & [opts]] & body]
  `(with-transaction* (fn [~tx] ~@body) ~(or opts {})))
