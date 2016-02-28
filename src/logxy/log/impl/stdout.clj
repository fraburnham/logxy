(ns logxy.log.impl.stdout
  "Smiple implementation of logging via stdout"
  (:require [logxy.log.protocols :refer [Logger]]
            [clojure.pprint :refer [pprint]])
  (:gen-class))

;; should be using clojure logging

(deftype STDOUTLogger []
  Logger
  (log-request
    [_ request]
    (pprint request))
  (log-response
    [_ response]
    (pprint response)))
