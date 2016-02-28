(ns logxy.log.protocols)

(defprotocol Logger
  (log-request [logger request])
  (log-response [logger response]))
