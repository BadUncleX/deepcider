(ns myapp.cli.core
  "Main entry point for the CLI application.
   Handles command routing and error management."
  (:gen-class)
  (:require [myapp.cli.parser :as parser]
            [myapp.cli.commands.chat :as chat]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

(def commands 
  "Set of available commands in the CLI"
  #{"chat" "help" "exit" "config"})

(defn- print-commands
  "Print a list of all available commands to stdout"
  []
  (println "\nAvailable commands:")
  (doseq [cmd commands]
    (println (format "/%s" cmd))))

(defn handle-command
  "Handle a command input string.
   
   Args:
     input - Command string in format '/command [args...]'
   
   Throws:
     ex-info if command is not recognized"
  [input]
  (let [input (str/trim input)
        [cmd & args] (str/split input #"\s+")]
    (when-not (str/blank? cmd)
      (case (str/replace cmd #"^/" "")
        "chat" (chat/execute! args)
        "help" (print-commands)
        "exit" (System/exit 0)
        "config" (println "Configure settings")
        (throw (ex-info "Unknown command" {:command cmd}))))))

(defn start-interactive-mode
  "Start the interactive command line mode.
   Continuously reads user input and executes commands until exit.
   Commands are prefixed with '/' and can include arguments.
   
   Example:
     /chat Hello world
     /help
     /exit"
  []
  (log/info "Starting interactive mode (type /help for commands)")
  (print-commands)
  (loop []
    (print "\ndeep> ")
    (flush)
    (when-let [input (read-line)]
      (when-not (str/blank? input)
        (try
          (handle-command input)
          (catch Exception e
            (log/error e "Command execution error"))))
      (recur))))

(defn -main
  "Application entry point.
   
   Args:
     & args - Command line arguments. If empty, starts interactive mode.
             Otherwise, treats arguments as a single command.
   
   Example:
     (-main)                    ;; Start interactive mode
     (-main \"/chat\" \"hello\")  ;; Execute chat command"
  [& args]
  (try
    (if (empty? args)
      (start-interactive-mode)
      (handle-command (str/join " " args)))
    (catch Exception e
      (log/error e "Application error")
      (System/exit 1))))

(comment
  (-main)  ;; Start interactive mode
  (-main "/chat" "Hello, world!")  ;; Execute single command
  )

;; factorial function with tail recursion
(defn factorial
  "Calculate the factorial of a number using tail recursion."
  [n]
  (loop [n n
         acc 1]
    (if (zero? n)
      acc
      (recur (dec n) (* n acc)))))
