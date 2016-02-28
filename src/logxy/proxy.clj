(ns logxy.proxy
  (:require [clj-http.client :as http]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.impl.interceptor :refer [terminate]]
            [logxy.log.protocols :refer [log]]
            [logxy.log.impl.postgres :refer [worker]]
            [clojure.core.async :refer [chan]]
            [clojure.java.io :as io])
  (:import [logxy.log.impl.postgres PostgresLogger]))

(def ch (chan))
(worker ch)
(def logger (PostgresLogger. ch))

(defn build-request-uri
  [request]
  ;; protocol://host/uri
  (str (name (:scheme request))
       "://"
       (get-in request [:headers "host"])
       (:uri request)
       (:query-string request)))

(defn input-stream->byte-array
  [stream]
  ;; read bytes from the stream and into a byte array returning the array
  (with-open [os (java.io.ByteArrayOutputStream.)]
    (println "Begin copy")
    (io/copy stream os)
    (println "Begin bytes")
    (.toByteArray os)))

(defmulti proxy-request
  (fn [uri request] (:request-method request)))

(defmethod proxy-request :get
  [uri request]
  (http/get uri {:headers (:headers request)
                 :as :stream
                 :decompress-body false  ;; instead of decompress false i could just gzip all thangs...
                 :throw-exceptions? false}))

(defmethod proxy-request :post
  [uri request]
  (http/post uri {:headers (:header request)
                  :body (:body request)
                  :as :stream
                  :decompress-body false
                  :force-redirects true
                  :throw-exceptions? false}))

(def proxy
  (interceptor
   {:name ::not-found-interceptor
    :enter (fn [{:keys [request] :as context}]
             (let [uri (build-request-uri request)
                   response (proxy-request uri request)]
               (log logger [uri
                            (-> request
                                (dissoc :servlet-request
                                        :servlet-response
                                        :io.pedestal.http.impl.servlet-interceptor/async-supported?
                                        :io.pedestal.http.impl.servlet-interceptor/protocol
                                        :servlet-context
                                        :servlet-path
                                        :servlet))
                            (update-in response [:body] input-stream->byte-array)])
               (-> (assoc context :response response)
                   terminate)))}))
