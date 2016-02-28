(ns logxy.log.impl.stdout
  "Smiple implementation of logging via stdout"
  (:require [logxy.log.protocols :refer [Logger]]
            [clojure.pprint :refer [pprint]]
            [clojure.core.async :as a])
  (:gen-class))

(defn worker
  [ch]
  (a/go
    (while true
      (let [[request response] (a/<! ch)]
        (pprint request)
        (pprint response)))))

(deftype STDOUTLogger [ch]
  Logger
  (log
    [_ msg]
    (a/>!! ch)))
