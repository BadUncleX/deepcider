(ns myapp.utils.rop-util)

;; -------------------------
;; 1. Core Types
;; -------------------------

;; Result type represents three states:
;; - Ok: Successful result with a value
;; - Err: Error state with error info
;; - None: Optional value not present
(defrecord Result [value error is-none?])

;; Structured error information
(defrecord RopError [error-type message info])

;; -------------------------
;; 2. Helper Predicates
;; -------------------------

(defn meaningful-value?
  "Checks if a value is meaningful (non-nil and non-empty if collection)"
  [v]
  (or (and (coll? v) (seq v))
      (and (not (coll? v)) (some? v))))

(defn none?
  "Checks if result is None state"
  [result]
  (and (instance? Result result)
       (:is-none? result)))

(defn error?
  "Checks if result is Error state"
  [result]
  (and (instance? Result result)
       (some? (:error result))))

(defn ok?
  "Checks if result is Ok state"
  [result]
  (and (instance? Result result)
       (not (none? result))
       (not (error? result))))

;; -------------------------
;; 3. Constructors
;; -------------------------
(defn Err
  ([error-type message]
   (Err error-type message {}))
  ([error-type message info]
   (->Result nil (->RopError error-type message info) false)))

(def None (->Result nil nil true))

(defn Ok [x]
  (if (meaningful-value? x)
    (->Result x nil false)
    None))

;; -------------------------
;; 4. Core Functions
;; -------------------------

(defn bind
  "Binds a Result monad to a function. Only propagates Err states early.
   Ok and None states continue through the computation chain."
  [result f]
  (if (instance? Result result)
    (if (error? result)
      result  ; Early return only for Err state
      (let [res (f result)]  ; Pass the entire Result (including None) to f
        (if (instance? Result res)
          res
          (throw (Exception. "Function must return a Result")))))
    (throw (Exception. "let-try only accepts Result type"))))

;; -------------------------
;; 5. Macros
;; -------------------------

;let-try Responsibility Definition:
;
;If marked as Err, I help you return the error early
;Ok: normal return
;None: normal return, user handles the logic themselves (None is still kept in let-try because functions returning None typically also return Ok or Err in most cases)
;Functions that may return None follow the convention: xx-maybe, e.g., query-cache-by-id-maybe
;Therefore, Option and Result are now merged into: Result (Ok, Err, None), Option and Some no longer exist

(defmacro let-try
  "A binding macro that allows for Result-based computations.
   - Propagates Err states early
   - Passes Ok and None states through to user code
   Example:
   (let-try [a (some-result)
             b (maybe-result)]
            (if (none? b)
              (handle-none a)
              (handle-success a b)))"
  [bindings & body]
  (when (and (not (vector? bindings)) (not (seq? bindings)))
    (throw (IllegalArgumentException.
             "let-try requires a vector for bindings")))

  (when-not (even? (count bindings))
    (throw (IllegalArgumentException.
             "let-try requires an even number of forms in binding vector")))

  (let [steps (partition 2 bindings)]
    (if (empty? steps)
      (if (= 1 (count body))
        (first body)
        `(do ~@body))
      (let [[sym expr] (first steps)
            rest-bindings (apply concat (rest steps))]
        `(bind ~expr (fn [~sym]
                       (let-try ~rest-bindings ~@body)))))))

(defn UnwrapOr
  "Safely extracts value from a Result or returns the default value.

   Returns:
   - For Ok: the contained value
   - For Err: the default value
   - For None: the default value
   - For direct values: returns value if meaningful, otherwise default"
  ([result default]
   (cond
     (instance? Result result)
     (cond
       (error? result) default
       (none? result) default
       :else (:value result))

     (meaningful-value? result)
     result

     :else
     default))
  ([result]
   (if (and (instance? Result result) (ok? result))
     (:value result)
     nil)))

(defmacro OrElse
  "Try each expression in sequence until one returns Ok.

   Example:
   (OrElse
     (query-cache-maybe id)
     (query-redis-maybe id)
     (query-db id))"
  [& exprs]
  (reduce
    (fn [acc expr]
      `(if (ok? ~acc)
         ~acc
         ~expr))
    (first exprs)
    (rest exprs)))

(defmacro Steps
  "Execute operations in sequence, continue if Ok.
   Early returns if any operation returns Err or None.

   Each operation must return Result type (Ok/Err/None).

   Example:
   (Steps
     (validate-order id)   ;; Only continue if validation Ok
     (charge-payment id)   ;; Only continue if payment Ok
     (send-email id))      ;; Only execute if previous steps Ok

   Returns:
   - First Err/None encountered in the chain
   - Last Ok result if all operations successful"
  [& ops]
  (reduce
    (fn [acc op]
      `(if (ok? ~acc)
         ~op
         ~acc))
    (first ops)
    (rest ops)))