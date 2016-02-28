(ns logxy.log.protocols)

(defprotocol Logger
  ;; msg is [request response]
  ;; I should probably make a type for that...
  (log [logger msg]))
