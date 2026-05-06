# CI/CD Guide for **tracking_order**

## 📚 Overview
This guide explains how to set up a **GitHub Actions** CI/CD pipeline for the `tracking_order` Java Spring Boot project.
- **Branch strategy:** `develop` → feature work, **PR** → `main` for production.
- **CI:** Build, test, lint.
- **CD:** Build Docker image, push to GHCR, optionally deploy to a VPS.

---

## 1️⃣ Branch Workflow
| Branch | Purpose |
|--------|----------|
| `develop` | Ongoing development, feature branches are merged here. |
| `main` | Production‑ready code. All merges to `main` trigger a full release pipeline. |

**Typical flow:**
1. Create a feature branch from `develop`.
2. Open a Pull Request **targeting `develop`** for code review.
3. Once approved, merge into `develop`.
4. When a release is ready, open a PR **from `develop` to `main`**. Merging triggers the **release pipeline**.

---

## 2️⃣ GitHub Actions Pipeline (`.github/workflows/ci-cd.yml`)
The file we already created contains three jobs:
1. **`build-and-test`** – runs on every PR and push to any branch. Compiles the project with Maven, runs unit/integration tests, and performs a static‑code analysis.
2. **`docker-build-push`** – runs **only** when code lands on `main`. It builds a Docker image (using the existing `Dockerfile`) and pushes it to the **GitHub Container Registry (GHCR)**.
3. **`deploy`** – optional. Deploys the new image to a remote VPS via SSH (requires secrets).

### Key sections to customise
```yaml
# Push image to your own registry (if you prefer Docker Hub, change the registry URL)
- name: Log in to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```
If you use Docker Hub, replace `ghcr.io` and provide `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` secrets.

#### Deploy job notes
- Update the `cd /opt/tracking_order` line to the absolute path where the `docker‑compose.yml` lives on your server.
- Ensure the server can pull from the registry (run the same `docker login` command on the server).
- The job uses the `appleboy/ssh-action`; you must store three secrets in the repo (see next section).

---

## 3️⃣ Adding Secrets to GitHub
Navigate to **Settings → Secrets and variables → Actions → New repository secret** and add:
- `SERVER_IP` – IP address of the VPS.
- `SERVER_USER` – SSH user (e.g., `ubuntu`).
- `SERVER_SSH_KEY` – Private key contents (no passphrase). The public key must be added to `~/.ssh/authorized_keys` on the server.
- (Optional) `DOCKERHUB_USERNAME` & `DOCKERHUB_TOKEN` if you push to Docker Hub.

**Never** commit passwords or private keys to the repository!

---

## 4️⃣ Running the Pipeline Locally (Optional)
You can test the Docker build step locally:
```bash
# From the project root
./mvnw clean package -DskipTests   # Build the jar
docker build -t tracking_order:local .
```
If the image builds successfully, the CI step should also succeed.

---

## 5️⃣ Troubleshooting Common Issues
| Symptom | Likely Cause | Fix |
|----------|--------------|-----|
| CI fails at `./mvnw test` | Missing environment variables (e.g., DB credentials) | Add a `test` profile in `application.properties` that points to an in‑memory DB (H2) for CI. |
| Docker push denied | Wrong registry credentials or GHCR token scope | Ensure `GITHUB_TOKEN` has **write:packages** permission (default for repo). |
| Deploy job can't SSH | Wrong `SERVER_IP`/`SERVER_USER` or key mismatch | Verify that `ssh -i <key> $SERVER_USER@$SERVER_IP` works from your local machine. |
| Image size too big | Unnecessary files copied in Docker context | Add a `.dockerignore` to exclude `target/`, `src/test/`, etc. |

---

## 6️⃣ Next Steps / Extensions
- **Branch protection rules**: Enable required reviews on `main` and status checks.
- **Semantic versioning**: Use a GitHub Action to bump the version tag on each release.
- **Release notes**: Auto‑generate changelogs with `github‑changelog‑action`.
- **Monitoring**: Add a step that runs `docker image ls` on the server and sends a Slack notification on failures.

---

## 📌 Quick Recap
1. Keep development on `develop`.
2. Merge to `main` via PR to trigger a full release.
3. The pipeline builds, tests, Docker‑izes, pushes, and optionally deploys.
4. Store all secrets in GitHub **Settings → Secrets**.
5. Adjust paths/registry names as needed.

Happy CI/CD!
