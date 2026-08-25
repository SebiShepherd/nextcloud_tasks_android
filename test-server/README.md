# Throwaway self-signed Nextcloud (for cert4android device testing)

Spins up a Nextcloud reachable only over a **self-signed HTTPS cert** (Caddy internal CA), so you
can test the cert4android trust flow (PR #69 / #61) from your phone. Throwaway: SQLite, plaintext
admin password, no persistence guarantees.

## 1. Set your LAN IP

Default is `192.168.178.145`. If different:

```bash
ip -4 addr show scope global | grep -oP 'inet \K[\d.]+' | head -1   # find it
export SERVER_IP=<your-lan-ip>
```

Phone and this machine must be on the **same WiFi**.

## 2. Start

```bash
cd test-server
docker compose up -d
# wait ~1-2 min for first-run install; watch with:
docker compose logs -f nextcloud   # ready when you see the apache start / "initializing finished"
```

Open `https://<SERVER_IP>` in a desktop browser first — you should get a **cert warning** (expected;
that's the self-signed cert). Accept it, confirm the Nextcloud login loads. Admin: `admin` / `admin12345`.

## 3. Test on the phone

1. Same WiFi. Install the debug/alpha build.
2. Server URL: `https://<SERVER_IP>`  → login `admin` / `admin12345`.
3. **Expected:** cert4android shows a certificate-accept dialog (fingerprint) on first contact.
   - Accept → login proceeds, sync works, cert is pinned (dialog won't reappear).
   - Also worth checking: reject → login fails cleanly (no crash, see #63).
4. Sync test: create a task list + task in the app; verify it appears in the NC web UI
   (Calendar/Tasks) and survives a pull-to-refresh.

## 4. Firewall (Fedora)

If the phone can't reach port 443:

```bash
sudo firewall-cmd --add-port=443/tcp        # this session only
```

## 5. Tear down

```bash
docker compose down -v   # -v also drops the data volumes
```

## Covers which #61 sub-cases?

- **Self-signed leaf** (main case) — yes, directly.
- Enterprise/user-CA and missing-intermediate variants would need a different cert setup; this
  proves the core TOFU accept + pinning path, which is the shared code for all three.
