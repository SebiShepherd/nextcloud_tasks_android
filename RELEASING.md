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

## ⚠️ Setting up the alpha (closed testing) track — one-time

A track must exist in the Play Console before the pipeline can upload to it. As of this
writing the **closed testing (alpha) track has not been set up yet** — do this once:

1. Play Console → your app → **Test and release → Testing → Closed testing**.
2. Create a new closed testing track (or use the default "Alpha"). The track's internal
   name must be `alpha` to match the Fastlane lane (or adjust `PLAY_TRACK`).
3. Under **Testers**, create an email list (or Google Group) and add the testers — e.g.
   the users who reported certificate/sync issues, so they can validate fixes against
   their real servers.
4. Save and copy the **opt-in URL**; share it with testers so they can join.
5. First upload: `fastlane alpha` (or the Gradle command above). The first build on a new
   track may need to be promoted/reviewed in the Console.

After that, day-to-day alpha releases are just `fastlane alpha`.
