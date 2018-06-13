## Overview

This is an attempt to implement the runtime event specification and monitoring library proposed by by Murat Karaorman and Jay Freeman in http://www.runtime-verification.org/rv2004/papers/paper11.pdf.

## Status

- [x] Event specification with regular expressions and logic operators
- [x] Matching instructions against their target (e.g. name of accessed field) and the method that immediately caused the caused its invocation
- [ ] Matching instructions against method names in the runtime call stack
- [x] Generating events immediately before and after matching instruction (the *before* and *after* monitors)
- [ ] Generating events executed instead of matching instruction (the *instead* monitors) with user-defined behaviour


## Documentation & Usage

Will appear soon.

## Tests

Will appear soon.

## License

MIT
