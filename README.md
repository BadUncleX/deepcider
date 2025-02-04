# ReadMe

## Run app
1. `npx shadow-cljs watch app`
2. `clj -M:dev`
3. open `http://localhost:8080/` in browser

## package
package `repomix ~/PROJECT_DIR --include "src/**/*.clj,src/**/*.cljs" --style xml`

## repl with shadow-cljs
1. launch repl
2. not switch to `cljs`
3. run `(shadow/repl :app)`
4. open `http://localhost:8080/` in browser
