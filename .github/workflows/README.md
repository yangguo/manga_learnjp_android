# GitHub Actions Workflows for Android APK Build

This directory contains GitHub Actions workflows to automatically build Android APK files.

## Workflows

### 1. `build-apk.yml` - Basic APK Build

**Triggers:**
- Push to `main`, `master`, or `develop` branches
- Pull requests to `main`, `master`, or `develop` branches
- Manual trigger via GitHub Actions UI

**What it does:**
- Sets up Android build environment
- Builds both debug and release APKs
- Uploads APKs as artifacts
- Creates a GitHub release (only on main branch pushes)

### 2. `build-signed-apk.yml` - Signed APK Build

**Triggers:**
- Push to tags starting with `v` (e.g., `v1.0.0`)
- Manual trigger with version input

**What it does:**
- Builds a signed release APK (if keystore secrets are configured)
- Falls back to unsigned APK if no keystore is available
- Creates a GitHub release with the APK

## Setup Instructions

### Basic Setup (No Signing)

1. Push these workflow files to your repository
2. The workflows will automatically run on the specified triggers
3. APKs will be available as artifacts in the Actions tab

### Advanced Setup (With APK Signing)

To enable APK signing for production releases:

1. **Generate a keystore** (if you don't have one):
   ```bash
   keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
   ```

2. **Add secrets to your GitHub repository**:
   - Go to Settings → Secrets and variables → Actions
   - Add the following secrets:
     - `KEYSTORE_BASE64`: Base64 encoded keystore file
       ```bash
       base64 -i my-release-key.jks | pbcopy  # macOS
       base64 -w 0 my-release-key.jks         # Linux
       ```
     - `KEYSTORE_PASSWORD`: Password for the keystore
     - `KEY_ALIAS`: Alias of the key in the keystore
     - `KEY_PASSWORD`: Password for the key

3. **Create a release**:
   - Create and push a tag: `git tag v1.0.0 && git push origin v1.0.0`
   - Or use the manual trigger in GitHub Actions

## Artifacts and Releases

- **Artifacts**: Available for 90 days in the Actions tab
- **Releases**: Permanent releases with APK files attached
- **Debug APKs**: Built on every push/PR for testing
- **Release APKs**: Optimized builds for distribution

## Customization

You can customize these workflows by:

- Changing trigger branches
- Modifying build variants
- Adding additional build steps (testing, linting, etc.)
- Configuring different signing configurations
- Adding notification steps (Slack, Discord, etc.)

## Troubleshooting

### Common Issues:

1. **Build fails with "Permission denied"**:
   - The workflow automatically grants execute permission to `gradlew`

2. **Keystore not found**:
   - Ensure `KEYSTORE_BASE64` secret is properly set
   - Check that the base64 encoding is correct

3. **Gradle build fails**:
   - Check that your local build works: `./gradlew assembleRelease`
   - Ensure all dependencies are properly declared

4. **Out of memory errors**:
   - Add `org.gradle.jvmargs=-Xmx2048m` to `gradle.properties`

### Getting Help

- Check the Actions tab for detailed logs
- Ensure your project builds locally before pushing
- Verify all required secrets are configured correctly