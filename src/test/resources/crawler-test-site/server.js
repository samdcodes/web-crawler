import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { extname, join, normalize } from "node:path";
import { fileURLToPath } from "node:url";

const host = "127.0.0.1";
const port = Number.parseInt(process.env.PORT ?? "3000", 10);
const publicDirectory = join(fileURLToPath(new URL(".", import.meta.url)), "public");

const contentTypes = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8"
};

const server = createServer(async (request, response) => {
  const url = new URL(request.url, `http://${request.headers.host}`);
  console.log(`${request.method} ${url.pathname}`);

  if (url.pathname === "/robots.txt") {
    respond(response, 200, "text/plain; charset=utf-8", "User-agent: *\nDisallow: /private.html\n");
    return;
  }

  if (url.pathname === "/redirect-internal") {
    response.writeHead(302, { Location: "/contact.html" });
    response.end();
    return;
  }

  if (url.pathname === "/redirect-external") {
    response.writeHead(302, { Location: "https://example.com/" });
    response.end();
    return;
  }

  if (url.pathname === "/missing") {
    respond(response, 404, "text/plain; charset=utf-8", "This response is intentionally missing.\n");
    return;
  }

  if (url.pathname === "/asset.pdf") {
    respond(response, 200, "application/pdf", "%PDF-1.4\nThis is a crawl fixture, not a real PDF.\n");
    return;
  }

  if (url.pathname === "/slow.html") {
    setTimeout(() => serveFile(response, "/slow.html"), 250);
    return;
  }

  serveFile(response, url.pathname);
});

async function serveFile(response, pathname) {
  const requestedPath = pathname === "/" ? "/index.html" : pathname;
  const filePath = normalize(join(publicDirectory, requestedPath));

  if (!filePath.startsWith(publicDirectory)) {
    respond(response, 403, "text/plain; charset=utf-8", "Forbidden\n");
    return;
  }

  try {
    const body = await readFile(filePath);
    const contentType = contentTypes[extname(filePath)] ?? "application/octet-stream";
    respond(response, 200, contentType, body);
  } catch {
    respond(response, 404, "text/plain; charset=utf-8", "Not found\n");
  }
}

function respond(response, statusCode, contentType, body) {
  response.writeHead(statusCode, { "Content-Type": contentType });
  response.end(body);
}

server.listen(port, host, () => {
  console.log(`Crawler fixture listening at http://${host}:${port}/`);
});