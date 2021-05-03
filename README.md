# everclear

generated using Luminus version "3.57"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run

## License

Copyright © 2020, 2021 FIXME

## REPL

``` clojure
(require 'everclear.routes.home)
(in-ns 'everclear.routes.home)


(require 'everclear.state.filewatch)
(in-ns 'everclear.state.filewatch)

; everclear.routes.home=> @connected-uids
; {:ws #{}, :ajax #{}, :any #{}}
; reload browser windowuser=>
; everclear.routes.home=> @connected-uids
; {:ws #{:taoensso.sente/nil-uid}, :ajax #{}, :any #{:taoensso.sente/nil-uid}}

(chsk-send! :taoensso.sente/nil-uid [:h/h "hooo"])
(chsk-send! :taoensso.sente/nil-uid [:h/h "hoooiiiii"])
(chsk-send! :taoensso.sente/nil-uid [:h/h "hoooiiiiioooo"])

(chsk-send! :sente/all-users-without-uid [:h/h "haaaffa"])
; ^^^^^^^ BETTER!
```
