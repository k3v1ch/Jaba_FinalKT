# ─── Stage 1: build a purged, static Tailwind stylesheet ────────────────────
# Replaces the runtime Tailwind Play CDN (cdn.tailwindcss.com). That CDN ships a
# full in-browser compiler that installs a MutationObserver on the document and
# recompiles CSS on every DOM change; since this SPA re-renders #app on each
# click/route, it pinned the CPU at ~100%. We compile once here instead.
FROM node:22-alpine AS css
WORKDIR /build
COPY index.html app.js tailwind.config.js input.css ./
RUN npx --yes tailwindcss@3.4.17 -c tailwind.config.js -i input.css -o tailwind.css --minify

# ─── Stage 2: static nginx server ───────────────────────────────────────────
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html /usr/share/nginx/html/
COPY app.js /usr/share/nginx/html/
COPY --from=css /build/tailwind.css /usr/share/nginx/html/
EXPOSE 80
