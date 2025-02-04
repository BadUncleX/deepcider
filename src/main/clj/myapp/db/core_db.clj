(ns myapp.db.core-db
  (:require [clojure.tools.logging :as log]
            [myapp.utils.rop-util :refer [None Ok OrElse UnwrapOr ok?]]
            [myapp.utils.core-util :as util :refer [Option]]
            [myapp.db.conn-db :as conn]
            [clojure.core.async :refer [go]]
            [honey.sql :as sql]
            [honey.sql.helpers :as sqlh :refer [select from where values insert-into
                                               order-by left-join join]]))

(def ^:private mywords-cache (atom {}))

(defn log-sql [sql & params]
   (log/info (str "Executing SQL: " sql " with params: " params)))

(defn update-cache [word-details]
  (log/info (str "Updating cache for word: " (:word word-details) " with details: " word-details))
  (swap! mywords-cache assoc (:word word-details) word-details)
  (Ok word-details))

(defn load-mywords-cache []
  (let [query (-> (select :*)
                  (from :mywords)
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/query-many query)]
    (if (ok? result)
      (do
        (reset! mywords-cache
                (into {} (map (fn [row] [(:word row) row]) (:value result))))
        (Ok {:success true}))
      result)))

(defn get-word-from-cache-maybe [word]
  (if-let [cached-word (get @mywords-cache word)]
    (Ok cached-word)
    None))

(defn get-word-level-maybe [word]
  (OrElse
    (get-word-from-cache-maybe word)
    (let [query (-> (select :level)
                    (from :mywords)
                    (where [:= :word word])
                    sql/format)
          _ (log-sql (first query) (rest query))
          result (conn/query-one query)]
      result)))

(defn get-details-of-word-maybe [word]
  (OrElse
    (get-word-from-cache-maybe word)
    (let [query (-> (select :*)
                    (from :mywords)
                    (where [:= :word word])
                    sql/format)
          _ (log-sql (first query) (rest query))
          result (conn/query-one query)]
      result)))

(defn get-ai-by-word-maybe [word]
  (OrElse
    (get-word-from-cache-maybe word)
    (let [query (-> (select :ai)
                    (from :mywords)
                    (where [:= :word word])
                    sql/format)
          _ (log-sql (first query) (rest query))
          result (conn/query-one query)]
      (if (ok? result)
        (do
          (update-cache {:word word :ai (:ai (:value result))})
          result)
        result))))

(defn query-by-word-maybe [word]
  (OrElse
    (get-word-from-cache-maybe word)
    (let [query (-> (select :word)
                    (from :mywords)
                    (where [:= :word word])
                    sql/format)
          _ (log-sql (first query) (rest query))
          result (conn/query-many query)]
      (if (ok? result)
        (Ok {:word (first (:value result))})
        None))))

(defn count-words-preparing []
  (let [query (-> (select [[:count :*]])
                  (from :mywords)
                  (where [:and
                         [:< :level 4]
                         [:>= :preparing 1]])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/query-one query)]
    (Ok (UnwrapOr result 0))))

(defn count-words-preparing-by-day []
  (let [sql "SELECT substr(prepare_time, 1, 10) as prepare_time, count(*) as count 
             FROM mywords 
             WHERE preparing >= 1 
             GROUP BY substr(prepare_time, 1, 10) 
             ORDER BY prepare_time DESC 
             LIMIT 30"]
    (log-sql sql)
    (conn/query-many [sql])))

(defn count-words-dialysis-by-day []
  (let [sql "SELECT substr(insert_date, 1, 10) as prepare_time,
                   count(*) as count,
                   count(DISTINCT word) as distinct_count 
             FROM dialysis 
             WHERE prepare_time > '2024-08-25' 
             GROUP BY substr(insert_date, 1, 10) 
             ORDER BY insert_date DESC 
             LIMIT 30"]
    (log-sql sql)
    (conn/query-many [sql])))

(defn count-words-mastered-by-day []
  (let [sql "SELECT substr(update_date, 1, 10) as update_date, count(*) as count 
             FROM mywords 
             WHERE level = 4 
             GROUP BY substr(update_date, 1, 10) 
             ORDER BY update_date DESC 
             LIMIT 30"]
    (log-sql sql)
    (conn/query-many [sql])))

(defn get-mastered-words []
  (let [query (-> (select :word)
                  (from :mywords)
                  (where [:= :level 4])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/query-many query)]
    (if (ok? result)
      (Ok (set (map :word (:value result))))
      result)))

(defn get-words-ai-notnull []
  (let [query (-> (select :word)
                  (from :mywords)
                  (where [:not= :ai nil])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/query-many query)]
    (if (ok? result)
      (Ok (set (map :word (:value result))))
      result)))

(defn- update-word-ai [word ai-json current-time source existing-record]
  (let [query (-> (sqlh/update :mywords)
                  (sqlh/set {:ai ai-json
                            :update_date current-time
                            :source source})
                  (where [:= :word word])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/execute! query)]
    (if (ok? result)
      (do
        (update-cache (assoc existing-record :ai ai-json :update_date current-time :source source))
        result)
      result)))

(defn- insert-new-word-ai-general [word ai-json current-time source]
  (let [query (-> (insert-into :mywords)
                  (values [{:word word
                           :ai ai-json
                           :level 3
                           :insert_date current-time
                           :update_date current-time
                           :source source}])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/execute! query)]
    (if (ok? result)
      (do
        (update-cache {:word word :ai ai-json :level 3 :insert_date current-time :update_date current-time :source source})
        result)
      result)))

(defn save-ai-by-word-general [word ai-json source]
  (let [current-time (util/current-timestamp)
        existing-record (query-by-word-maybe word)]
    (if (ok? existing-record)
      (update-word-ai word ai-json current-time source existing-record)
      (insert-new-word-ai-general word ai-json current-time source))))

(def ^:private cache-loaded? (atom false))

(when-not @cache-loaded?
  (go (load-mywords-cache))
  (reset! cache-loaded? true))

(defn- update-existing-word [current-time word existing-record tx]
  (let [query (-> (sqlh/update :mywords)
                  (sqlh/set {:level 3
                            :update_date current-time})
                  (where [:= :word word])
                  sql/format)
        _ (log-sql (first query) (rest query))
        update-result (conn/execute! tx query)]
    (if (ok? update-result)
      (do
        (update-cache (assoc existing-record :level 3 :update_date current-time))
        update-result)
      update-result)))

(defn- insert-new-word [word current-time tx]
  (let [query (-> (insert-into :mywords)
                  (values [{:word word
                           :level 3
                           :insert_date current-time
                           :update_date current-time}])
                  sql/format)
        _ (log-sql (first query) (rest query))
        insert-result (conn/execute! tx query)]
    (if (ok? insert-result)
      (do
        (update-cache {:word word :level 3 :insert_date current-time :update_date current-time})
        insert-result)
      insert-result)))

(defn update-word-to-3 [word]
  (conn/with-transaction [tx]
    (let [existing (conn/query-one tx ["SELECT * FROM mywords WHERE word = ?" word])
          current-time (util/current-timestamp)]
      (if (ok? existing)
        (update-existing-word current-time word (:value existing) tx)
        (insert-new-word word current-time tx)))))

(defn update-word-to-4 [word]
  (conn/with-transaction [tx]
    (let [existing (conn/query-one tx ["SELECT * FROM mywords WHERE word = ?" word])
          current-time (util/current-timestamp)]
      (if (ok? existing)
        (let [query (-> (sqlh/update :mywords)
                        (sqlh/set {:level 4
                                  :update_date current-time})
                        (where [:= :word word])
                        sql/format)
              _ (log-sql (first query) (rest query))
              update-result (conn/execute! tx query)]
          (if (ok? update-result)
            (update-cache (assoc (:value existing) :level 4 :update_date current-time))
            update-result))
        (let [query (-> (insert-into :mywords)
                        (values [{:word word
                                 :level 4
                                 :insert_date current-time
                                 :update_date current-time}])
                        sql/format)
              _ (log-sql (first query) (rest query))
              insert-result (conn/execute! tx query)]
          (if (ok? insert-result)
            (update-cache {:word word :level 4 :insert_date current-time :update_date current-time})
            insert-result))))))

(defn dialysis-plus-0 [word example]
  (let [query (-> (insert-into :dialysis)
                  (values [{:word word
                           :example example
                           :insert_date (util/current-timestamp)}])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/execute! query)]
    result))

(defn dialysis-plus-1 [word example]
  (conn/with-transaction [tx]
    (let [word-detail (get-details-of-word-maybe word)
          word-detail (UnwrapOr word-detail {})
          preparing (if (ok? word-detail)
                      (inc (Option (:preparing word-detail) 0))
                      0)
          query (-> (insert-into :dialysis)
                    (values [{:word word
                             :example example
                             :insert_date (util/current-timestamp)}])
                    sql/format)
          _ (log-sql (first query) (rest query))
          insert-result (conn/execute! tx query)
          query (-> (sqlh/update :mywords)
                    (sqlh/set {:preparing preparing
                              :update_date (util/current-timestamp)})
                    (where [:= :word word])
                    sql/format)
          _ (log-sql (first query) (rest query))
          update-result (conn/execute! tx query)]
      (update-cache (assoc word-detail :preparing preparing :update_date (util/current-timestamp)))
      {:insert-count (conn/get-update-count-int insert-result)
       :update-count (conn/get-update-count-int update-result)})))

(defn query-dialysis-by-word [word]
  (let [query (-> (select :*)
                  (from :dialysis)
                  (where [:= :word word])
                  (order-by [:insert_date :desc])
                  sql/format)
        _ (log-sql (first query) (rest query))]
    (conn/query-many query)))

(defn get-all-select-options
  []
  (let [query "SELECT s.id AS series_id, s.name AS series_name,
                      c.id AS chapter_id, c.name AS chapter_name
               FROM series s
               LEFT JOIN chapters c ON s.id = c.series_id
               ORDER BY s.id, c.id"]
    (conn/query-many [query]))

  #_(let [query (-> (select [:s.id :series_id]
                          [:s.name :series_name]
                          [:c.id :chapter_id]
                          [:c.name :chapter_name])
                  (from [:series :s])
                  (left-join [:chapters :c] [:= :s.id :c.series_id])
                  (order-by [:s.id :c.id])
                  sql/format)
        _ (log-sql (first query) (rest query))]
    (conn/query-many query)))

(defn create-new-series [name]
  (conn/with-transaction [tx]
    (let [query (-> (insert-into :series)
                    (values [{:name name}])
                    sql/format)
          _ (log-sql (first query) (rest query))
          _ (conn/execute! tx query)
          result (UnwrapOr (conn/query-one tx ["SELECT last_insert_rowid() as last_insert_rowid"]))
          id (:last_insert_rowid result)
          query (-> (select :*)
                    (from :series)
                    (where [:= :id id])
                    sql/format)
          _ (log-sql (first query) (rest query))
          series (conn/query-one tx query)]
      series)))

(defn create-new-chapter [name series-id]
  (conn/with-transaction [tx]
    (let [query (-> (insert-into :chapters)
                    (values [{:name name
                             :series_id series-id}])
                    sql/format)
          _ (log-sql (first query) (rest query))
          _ (conn/execute! tx query)
          result (UnwrapOr (conn/query-one tx ["SELECT last_insert_rowid() as last_insert_rowid"]))
          id (:last_insert_rowid result)
          query (-> (select :*)
                    (from :chapters)
                    (where [:= :id id])
                    sql/format)
          _ (log-sql (first query) (rest query))
          chapter (conn/query-one tx query)]
      chapter)))

(defn get-series-by-id [id]
  (let [query (-> (select :*)
                  (from :series)
                  (where [:= :id id])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/query-one query)]
    result))

(defn get-chapter-by-id [id]
  (let [query (-> (select :*)
                  (from :chapters)
                  (where [:= :id id])
                  sql/format)
        _ (log-sql (first query) (rest query))
        result (conn/query-one query)]
    result))

(defn save-dialysis-text [text series-id chapter-id desc]
  (conn/with-transaction [tx]
    (let [query (-> (insert-into :dialysis_text)
                    (values [{:content text
                             :series_id series-id
                             :chapter_id chapter-id
                             :desc desc
                             :insert_date (util/current-timestamp)}])
                    sql/format)
          ;;_ (log-sql (first query) (rest query))
          insert-result (conn/execute! tx query)]
      (if (ok? insert-result)
        (Ok {:insert-count (conn/get-update-count-int insert-result)})
        insert-result))))

(defn get-dialysis-text
  []
  (let [query (-> (select [:a.content :content]
                          [:a.desc :desc]
                          [:a.insert_date :insert_date]
                          [:b.id :series_id]
                          [:b.name :series_name]
                          [:c.id :chapter_id]
                          [:c.name :chapter_name])
                  (from [:dialysis_text :a])
                  (join [:chapters :c] [:= :a.chapter_id :c.id])
                  (join [:series :b] [:= :c.series_id :b.id])
                  (order-by [:a.insert_date :desc])
                  sql/format)
        _ (log-sql (first query) (rest query))]
    (conn/query-many query)))
