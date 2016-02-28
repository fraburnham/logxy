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
  (with-open [os (java.io.ByteArrayOutputStream.)]
    (io/copy stream os)
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

(defmethod proxy-request :put
  [uri request]
  (http/put uri {:headers (:header request)
                 :body (:body request)
                 :as :stream
                 :decompress-body false
                 :force-redirects true
                 :throw-exceptions? false}))

(defmethod proxy-request :delete
  [uri request]
  (http/delete uri {:headers (:header request)
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
                   response (-> (proxy-request uri request)
                                (update-in [:body] input-stream->byte-array))
                   ;; this stream fuckery creates a "copy" so that the
                   ;; original stream doesn't get consumed
                   request (update-in request [:body] input-stream->byte-array)]
               (log logger [uri
                            (-> request
                                (dissoc :servlet-request
                                        :servlet-response
                                        :io.pedestal.http.impl.servlet-interceptor/async-supported?
                                        :io.pedestal.http.impl.servlet-interceptor/protocol
                                        :servlet-context
                                        :servlet-path
                                        :servlet))
                            response])
               (-> (assoc context :request request)
                   (assoc :response response)
                   (update-in [:request :body] io/input-stream) ;; make the copied bytes into streams again
                   (update-in [:response :body] io/input-stream)
                   terminate)))}))
