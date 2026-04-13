# Grafana Alloy Minimal Setup (KoSpot Backend)

This guide configures the smallest production path:

1. Spring Boot exposes `/actuator/prometheus`.
2. Grafana Alloy scrapes that endpoint on the same server.
3. Alloy sends metrics to Grafana Cloud via `remote_write`.

## Scope

- In scope: server-side Alloy setup, Grafana Cloud token wiring, runtime validation.
- Out of scope: running a separate Prometheus server, logs/traces pipeline.

## Prerequisites

- `build.gradle` includes:
  - `org.springframework.boot:spring-boot-starter-actuator`
  - `io.micrometer:micrometer-registry-prometheus`
- `application.yml` exposes `health,prometheus`.
- Production endpoint is `http://localhost:8080/actuator/prometheus`.
- For local profile only, endpoint path is `/api/actuator/prometheus`.

## 1. Grafana Cloud

Create these values first:

- `GRAFANA_CLOUD_PROM_RW_URL`
- `GRAFANA_CLOUD_PROM_USER`
- `GRAFANA_CLOUD_API_KEY` (`metrics:write` scope)

## 2. Copy configuration files to server

Copy the templates in this repository:

- `docs/deploy-docs/alloy/config.alloy.example` -> `/etc/alloy/config.alloy`
- `docs/deploy-docs/alloy/alloy.env.example` -> `/etc/alloy/alloy.env`
- `docs/deploy-docs/alloy/alloy.service.override.example` -> `/etc/systemd/system/alloy.service.d/override.conf`

Then update `/etc/alloy/alloy.env` with real Grafana Cloud values.

## 3. Install and start Alloy

Example sequence on Ubuntu:

```bash
# Install Alloy (use your approved package source/version policy)
sudo apt-get update
sudo apt-get install -y alloy

# Ensure override directory exists
sudo mkdir -p /etc/systemd/system/alloy.service.d

# Reload and start
sudo systemctl daemon-reload
sudo systemctl enable alloy
sudo systemctl restart alloy
sudo systemctl status alloy --no-pager
```

## 4. Validate metrics flow

### A. App endpoint

```bash
curl -fsS http://localhost:8080/actuator/prometheus | head
```

### B. Alloy runtime

```bash
sudo journalctl -u alloy -n 200 --no-pager
```

Look for scrape/remote_write errors. If none, move to Grafana Cloud.

### C. Grafana Cloud ingestion

In Metrics Explorer, query:

- `up{job="kospot-backend"}`
- `jvm_memory_used_bytes{application="kospot-backend"}`
- `http_server_requests_seconds_count{application="kospot-backend"}`

## 5. Network and security notes

- Prefer Alloy on host + app endpoint via `localhost`.
- Do not hardcode cloud tokens in `config.alloy`.
- Restrict endpoint exposure at network layer even if app path is public.
- Server must allow outbound HTTPS to Grafana Cloud metrics endpoint.

## 6. Container topology note

This guide assumes Alloy runs on host OS.

If Alloy runs in Docker, `127.0.0.1:8080` will not reach the app container.
Set Alloy scrape target to the app container address (for example `kospot-app:8080`)
and ensure both containers share a network.
