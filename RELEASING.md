# Releasing & test tracks

The app publishes through [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher)
(the `com.github.triplet.play` plugin) and Fastlane. Which Play track an upload goes to is
controlled by the `PLAY_TRACK` environment variable (default: `production`) — see the `play { }`
block in `app/build.gradle.kts`.

## Play tracks

| Track        | Purpose                                  | Fastlane lane         |
|--------------|------------------------------------------|-----------------------|
| `internal`   | Quick internal QA (up to 100 testers)    | `fastlane internal`   |
| `alpha`      | Closed testing (invited testers)         | `fastlane alpha`      |
| `beta`       | Open/closed testing                      | `fastlane beta`       |
| `production` | Public release                           | `fastlane production` |

Equivalent without Fastlane:

```bash
PLAY_TRACK=alpha ./gradlew bundleRelease publishReleaseBundle
```

Publishing requires the signing secrets (see README) and `PLAY_SERVICE_ACCOUNT_JSON`
pointing at the Play service-account credentials.

## Automated releases (tags → tracks)

`.github/workflows/release.yml` builds, signs and publishes on every `v*.*.*` tag. The Play
track is derived from the tag's pre-release suffix, so you never hand-set `PLAY_TRACK` in CI:

| Tag pushed        | Play track   |
|-------------------|--------------|
| `v1.2.3`          | `production` |
| `v1.2.3-alpha.1`  | `alpha`      |
| `v1.2.3-beta.1`   | `beta`       |
| `v1.2.3-rc.1`     | `internal`   |

```bash
git tag v1.2.3-alpha.1 && git push origin v1.2.3-alpha.1   # → publishes to the alpha track
```

**versionCode** is set by CI to `100000 + <run number>`, not derived from the version name.
Play requires versionCode to strictly increase across *all* tracks, so two `-alpha.N` builds of
the same version must not share a code. Local `bundleRelease` (no `VERSION_CODE` env) still uses
the computed `MAJOR*10000+MINOR*100+PATCH` for dev builds — those are never published.

## Play tracks

The `internal`, `alpha` (closed testing), `beta` (open testing) and `production` tracks are
configured in the Play Console; their internal names match the tag routing and Fastlane lanes
above. Releasing to any track is a tag push (`v1.2.3-alpha.N`, `v1.2.3-beta.N`, …) or the
matching `fastlane` lane. The first build on a newly created track may require a one-time
review in the Console.
