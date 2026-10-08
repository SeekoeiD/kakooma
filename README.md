# Kakooma

A Kakooma math puzzle game for Android.

Every puzzle is a set of "flowers": rings of numbers. In each flower exactly one
number is special: it is the sum (or product) of two other numbers in the ring.
Tap it. The special numbers you find form the **final flower** at the bottom;
solve that to finish the puzzle.

## Modes

| Mode | Find the number that… |
| --- | --- |
| **+ Addition** | is the sum of two others |
| **− Subtraction** | you can subtract another number from to get a third |
| **× Multiplication** | is the product of two others |
| **÷ Division** | you can divide by another number to get a third |
| **Mixed** | each flower uses its own operation (shown in its centre) |

## Levels

| Level | Flowers | Numbers per flower | + / − numbers up to | × / ÷ factors up to |
| --- | --- | --- | --- | --- |
| Beginner | 3 | 4 | 10 | 5 (no final flower) |
| Easy | 3 | 4 | 20 | 10 |
| Medium | 4 | 5 | 50 | 12 |
| Hard | 4 | 6 | 100 | 15 |
| Expert | 5 | 6 | 250 | 20 |

Wrong taps add 5 seconds, hints add 15 seconds. Best times are saved per mode and level.

## Install on your phone

Open the [latest release](https://github.com/SeekoeiD/kakooma/releases/latest) on your
phone, download the `.apk` file and open it. Android will ask you to allow installs
from your browser or file manager the first time.

## Building

Every push runs `.github/workflows/release.yml`, which runs the unit tests,
builds a signed release APK and publishes it as a GitHub release.

Locally (needs the Android SDK):

```sh
./gradlew testDebugUnitTest assembleRelease
```

The release signing key `app/kakooma-release.jks` is committed on purpose so that
every build is signed the same way and new versions install over old ones. Because
it is public, anyone could sign an APK with it, so only install APKs from this
repository's releases. To use a private key instead, set `KAKOOMA_KEYSTORE`,
`KAKOOMA_KEYSTORE_PASSWORD`, `KAKOOMA_KEY_ALIAS` and `KAKOOMA_KEY_PASSWORD`.
