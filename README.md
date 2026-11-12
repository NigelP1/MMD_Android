# Make My Day for Android

Kotlin + Jetpack Compose app for phones and tablets, using the Laravel API at
`https://api.makemydaylondon.uk/api/v1` (repo `NigelP1/MMD_API`). Features follow
the SwiftUI iOS app (`~/Documents/MMD_IOS`).

## Requirements

- Android Studio (its bundled JDK is used for Gradle)
- Android SDK Platform 37.2

## Build and test

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug        # build
./gradlew :app:testDebugUnitTest    # unit tests
```

## Layout

| Package | Contents |
|---|---|
| `data/model` | API models (kotlinx.serialization, snake_case on the wire) |
| `data/network` | `MmdApi` (Retrofit), `ApiException` + `apiCall`, `AuthInterceptor` |
| `data/session` | `SessionManager`, token stored in DataStore and encrypted with an Android Keystore key |
| `data/repository` | `AuthRepository`, `VenueRepository`, `MeRepository` |
| `di` | Hilt modules |
| `ui` | Compose screens; `MmdApp` switches between a bottom bar (phone) and a navigation rail (tablet) |

## Keeping in step with the API

`app/src/test/resources/openapi.json` is a copy of `MMD_API/docs/openapi.json`.
`ApiContractTest` fails if the app calls an endpoint the spec doesn't have, if
the spec gains an endpoint the app doesn't cover, or if a model is missing a
required field. After changing the backend:

```bash
scripts/update-api-spec.sh            # copies from ~/Documents/MMD_API
./gradlew :app:testDebugUnitTest
```

`src/test/resources/fixtures` holds real responses from the live API, used by `ModelDecodingTest`.
