(ns logxy.proxy
  (:require [clj-http.client :as http]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.impl.interceptor :refer [terminate]]
            [logxy.log.protocols :refer [log-request log-response]])
  (:import [logxy.log.impl.stdout STDOUTLogger]))

(def logger (STDOUTLogger.))

(defn build-request-uri
  [request]
  ;; protocol://host/uri
  (str (name (:scheme request))
       "://"
       (get-in request [:headers "host"])
       (:uri request)))

(defmulti proxy-request
  (fn [request] (:request-method request)))

(defmethod proxy-request :get
  [request]
  (-> (build-request-uri request)
      (http/get {:headers (:headers request)
                 :as :stream
                 :decompress-body false
                 :throw-exceptions? false}))) ;; instead of decompress false i could just gzip all thangs...

(defmethod proxy-request :post
  [request]
  (-> (build-request-uri request)
      (http/post {:headers (:header request)
                  :body (:body request)
                  :as :stream
                  :decompress-body false
                  :force-redirects true
                  :throw-exceptions? false})))

(def proxy
  (interceptor
   {:name ::not-found-interceptor
    :enter (fn [context]
             (let [response (proxy-request (:request context))]
               (log-request logger (:request context))
               (log-response logger response)
               (-> (assoc context :response response)
                   terminate)))}))
