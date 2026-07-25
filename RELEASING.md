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

## Setting up the alpha (closed testing) track — one-time

The closed testing (**alpha**) track is set up in the Play Console (track name `alpha`,
testers added). For reference, the one-time steps were:

1. Play Console → your app → **Test and release → Testing → Closed testing**.
2. Create a closed testing track (or use the default "Alpha"). The track's internal
   name must be `alpha` to match the tag routing and Fastlane lane (or adjust `PLAY_TRACK`).
3. Under **Testers**, create an email list (or Google Group) and add the testers — e.g.
   the users who reported certificate/sync issues, so they can validate fixes against
   their real servers.
4. Save and copy the **opt-in URL**; share it with testers so they can join.

Day-to-day alpha releases are then just a tag push (`v1.2.3-alpha.N`) or `fastlane alpha`.
The first build on a new track may need a one-time promote/review in the Console.
