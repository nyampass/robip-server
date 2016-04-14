(ns robip-server.sign
 (:use compojure.core)
 (:require [clj-oauth2.client :as oauth2]
           [clj-http.client :as client]
           [clojure.data.json :as json]))

(defn facebook-oauth2 [{:keys [app-id app-secret redirect-uri]}]
  {:authorization-uri "https://graph.facebook.com/oauth/authorize"
   :access-token-uri "https://graph.facebook.com/oauth/access_token"
   :redirect-uri redirect-uri
   :client-id app-id
   :client-secret app-secret
   :access-query-param :access_token
   :scope ["email"]
   :grant-type "authorization_code"})

(defn auth-reqeuest-uri [config]
  (-> config
      facebook-oauth2
      oauth2/make-auth-request
      :uri))

(defn facebook-user-info [{:keys [app-id app-secret redirect-uri] :as config}
                          {code "code" :as query-params}]
  (let [uri (str "https://graph.facebook.com/oauth/access_token?"
                 "client_id=" app-id
                 "&redirect_uri=" redirect-uri
                 "&client_secret=" app-secret
                 "&code=" code)
        response (:body (client/get uri))
        token (get (re-find #"access_token=(.*?)&expires=" response) 1)]
    (-> (str "https://graph.facebook.com/me?fields=email,id,name&access_token=" token)
        client/get
        :body
        json/read-json))) ;; email, id, name
