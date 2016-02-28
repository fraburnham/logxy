(ns logxy.log.impl.postgres
  "Log http events to postgres"
  (:require [logxy.log.protocols :refer [Logger]]
            [clojure.pprint :refer [pprint]]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [yesql.core :refer [defqueries]]
            [logxy.log.impl.postgres.connection :refer [default-db-conn]]
            [cheshire.core :as json])
  (:gen-class))

(defqueries "queries/postgres-logging.sql"
  {:connection default-db-conn})

(defn worker
  [ch]
  (a/go
    (while true
      (let [[uri request response] (a/<! ch)
            t-id (:id (insert-transaction<! {:uri uri}))]
        (insert-request<! {:request (json/encode (dissoc request :body))
                           :body nil;(:body request)
                           :tid t-id})
        (insert-response<! {:response (json/encode (dissoc response :body))
                            :body nil;(:body response)
                            :tid t-id})))))

(deftype PostgresLogger [ch]
  ;; wouldn't it be cool if binding a var to this type started up a
  ;; worker?
  Logger
  (log
    [_ msg]
    (println "Putting msg on channel!")
    (a/>!! ch msg)))
