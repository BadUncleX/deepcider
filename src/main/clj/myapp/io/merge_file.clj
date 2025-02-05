(ns myapp.io.merge-file
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn merge-file
  "using content (which contains SEARCH and REPLACE keywords) to update target file (file-name)"
  [base-path file-name content]
  (let [file-path (str base-path "/" file-name) ; Construct the full file path
        search-start "<<<<<<< SEARCH"
        search-end "======="
        replace-start "======="
        replace-end ">>>>>>> REPLACE"

        search-block (-> content
                         (str/split #"\n")
                         (->> (drop-while #(not (str/includes? % search-start)))
                              (drop 1) ; Drop search-start line
                              (take-while #(not (str/includes? % search-end))) ; Take until search-end
                              (str/join "\n")))

        replace-block (-> content
                          (str/split #"\n")
                          (->> (drop-while #(not (str/includes? % replace-start)))
                               (drop 1) ; Drop replace-start line
                               (take-while #(not (str/includes? % replace-end))) ; Take until replace-end
                               (str/join "\n")))]

    (try
      (let [original-content (slurp file-path)  ; Read the original content of the file
            search-block-empty? (str/blank? search-block) ; Check if the search block is empty
            updated-content (if search-block-empty?
                              (str original-content replace-block) ; Append replace-block if search-block is empty
                              (str/replace original-content search-block replace-block))] ; Replace search with replace block

        (if (and (not search-block-empty?) (= updated-content original-content))  ; Check if the replacement actually happened when search-block is not empty
          (throw (ex-info (str "Search block not found in file '" file-name "'. No changes made.") {:file-path file-path :search-block search-block})) ; Throw exception if no replacement and search block is not empty
          (do
            (spit file-path updated-content) ; Write the updated content back to the file
            (println (str "File '" file-name "' updated successfully.")))) ; Print success message
        )
      (catch Exception e
        (println (str "Error updating file '" file-name "': " (.getMessage e))
                 (println (str "Exception details: " (pr-str e))))))  ; Print exception details for debugging
    ))

; Example usage (for testing - remove before production)
(comment

  ; Create a dummy file for testing (empty file case)
  (spit "/tmp/empty.txt" "")

  ; Example content with EMPTY SEARCH block
  (def empty-search-content
    "<<<<<<< SEARCH\n=======\nimport { useHistory } from 'react-router-dom';\n>>>>>>> REPLACE")

  ; Call the merge-file function with empty search
  (merge-file "/tmp" "empty.txt" empty-search-content)

  ; Check the contents of the file after the merge
  (println (slurp "/tmp/empty.txt")) ; Should show only the replace block

  ; Create a dummy file for testing (existing content)
  (spit "/tmp/test.txt" "This is some original content.\nimport React, { useEffect, useState } from 'react';\nimport axios from 'axios';\nThis is the end of the file.")

  ; Example content with SEARCH and REPLACE blocks
  (def content
    "<<<<<<< SEARCH\nimport React, { useEffect, useState } from 'react';\nimport axios from 'axios';\n=======\nimport React, { useEffect, useState } from 'react';\nimport axios from 'axios';\nimport { useHistory } from 'react-router-dom';\n>>>>>>> REPLACE")

  ; Call the merge-file function
  (merge-file "/tmp" "test.txt" content)

  ; Check the contents of the file after the merge
  (println (slurp "/tmp/test.txt")) ; Should show updated content

  ; Example with missing search block to test error handling
  (spit "/tmp/test_missing.txt" "This is some original content.\nThis is the end of the file.")

  (def missing-content
    "<<<<<<< SEARCH\nTHIS BLOCK DOES NOT EXIST\n=======\nimport { useHistory } from 'react-router-dom';\n>>>>>>> REPLACE")
  (merge-file "/tmp" "test_missing.txt" missing-content) ; This should now throw an exception
  )