# Local Crawler Fixture

Testing server for manual testing of web-crawler

## Prereqs
Tested with Node v24.13.0

## Run

```sh
node server.js
```

Defaults to Port 3000, this can be overridden:

```sh
PORT=3100 node server.js
```

## Routes

| Route | Purpose |
| --- | --- |
| `/` | Root page with internal, duplicate, fragment, external, protocol-relative, special-scheme, and empty links |
| `/about.html`, `/team.html`, `/contact.html` | Ordinary linked HTML pages with cycles and a query-string link |
| `/slow.html` | HTML page delayed by 250 ms |
| `/robots.txt` | Disallows `/private.html` |
| `/private.html` | HTML page that robots-aware crawlers should not fetch |
| `/redirect-internal` | `302` to `/contact.html` |
| `/redirect-external` | `302` to `https://example.com/` |
| `/missing` | `404` response |
| `/asset.pdf` | `200 application/pdf` response |

Serve robots.txt which disallows
```js
Disallow: /private.html
```
