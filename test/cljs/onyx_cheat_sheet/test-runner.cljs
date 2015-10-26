(ns onyx_cheat_sheet.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [onyx_cheat_sheet.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'onyx_cheat_sheet.core-test))
    0
    1))
