(ns myapp.utils.error-types 
  (:require
    [myapp.utils.rop-util :refer [Err]]))

;; Define all possible error types as constants
(def general-error :general-error)
(def parameter-is-null :parameter-is-null)
(def user-disabled :user-disabled)
(def invalid-user-id :invalid-user-id)
(def user-not-found :user-not-found)
(def internal-error :internal-error)
;; Add more error types as needed

(defn user-not-found-error [word]
  (Err :not-found
       (str "Word not found in database: " word)
       {:word word}))