(ns logxy.log.impl.postgres.connection)

(def default-db-conn
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname (System/getenv "TS_PG_SUBNAME")
   :user (System/getenv "TS_PG_USER")
   :password (System/getenv "TS_PG_PASS")})
