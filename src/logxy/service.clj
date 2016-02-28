(ns logxy.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :refer [interceptor]]
            [ring.util.response :as ring-resp]
            [logxy.proxy :refer [proxy]]))

(defn home-page
  [request]
  (ring-resp/response ""))

(defroutes routes
  [[["/" {:get home-page}]]])

(def service {:env :prod
              ::bootstrap/routes routes
              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :jetty
              ::bootstrap/host "localhost"
              ::bootstrap/port 8080
              ;; fix this to just be another interceptor instead of stealing not found
              ;; interceptors can cause early termination...
              ::bootstrap/not-found-interceptor proxy})
