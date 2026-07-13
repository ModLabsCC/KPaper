# Jenkins migration

The GitHub Actions workflows currently cover three independent jobs:

| Pipeline | Trigger | Equivalent work |
| --- | --- | --- |
| `Jenkinsfile` | PRs targeting `main` (Multibranch Pipeline) | `apiCheck`, then `build` |
| `Jenkinsfile.publish` | Push to `main` or manual run | CalVer version, Maven publish, GitHub release, optional downstream dispatch |
| `Jenkinsfile.sonar` | Push to `main` | `build sonar` with a full checkout |

Create a Jenkins agent with label `kpaper-jdk25`. It must provide JDK 25, Git, `curl`, and a POSIX shell. The Gradle wrapper downloads the remaining build dependencies. Do not use a Java 17-only agent: the build compiles for JVM 25.

Configure these Jenkins credentials:

- `kpaper-nexus`: Username/password for `NEXUS_USER` and `REPO_TOKEN`.
- `kpaper-github-token`: Secret text with permission to create releases in `ModLabsCC/KPaper`.
- `kpaper-downstream-pat`: Secret text with permission to dispatch the downstream workflow.
- `kpaper-sonar-token`: Secret text for SonarQube.

Also configure a Jenkins SonarQube installation named `ModLabs SonarQube`. Create the jobs as Multibranch Pipelines so `checkout scm` follows the discovered branch/PR revision. For publishing, set the repository URL and credentials in the job SCM configuration; the optional `TARGET_REPO` parameter replaces the old GitHub secret and can be left blank to skip downstream dispatch.

The publish pipeline intentionally creates the release through the GitHub API rather than requiring the GitHub CLI on the agent. The old Actions workflow used `GITHUB_TOKEN`; Jenkins therefore needs an equivalent repository-scoped token.
