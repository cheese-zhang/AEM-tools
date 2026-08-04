# AEM Toolkit

AEM Toolkit is an IntelliJ IDEA plugin for Adobe Experience Manager development.
It adds navigation, completion, inspections, previews, repository tools, and AEM
Author integration for FileVault projects.

## Requirements

- IntelliJ IDEA 2025.1 or newer
- Java 21
- An AEM project using a Maven-style `src/main/content/jcr_root` structure
- Optional: access to an AEM Author instance for server features

## Features

### AEM XML and components

- Detects and parses `.content.xml` files using the IntelliJ XML PSI API.
- Resolves `sling:resourceType`, `cq:template`, policies, dialogs, HTL files,
  client libraries, and Sling Models.
- Provides completion, hover documentation, Ctrl+Click navigation, gutter
  actions, and unresolved-component inspections.
- Recognizes AEM platform components below `/libs` without reporting false
  unresolved errors. If matching `jcr_root/libs` source is present locally,
  navigation opens the real source directory.

### HTL and Sling Models

- HTL expression completion, validation, documentation, and references.
- Navigation from HTL model properties to Java getters.
- Reverse gutter navigation from Java getters to HTL usages.
- Local template and file references plus HTL intention actions.

### AEM Toolkit tool window

- **Content**: hierarchy for the focused `.content.xml`.
- **Dialog**: Granite UI dialog structure with inherited dialog merging.
- **Preview**: visual preview for common Granite UI controls and tabs.
- **Author**: current JCR path, status, CRXDE navigation, copy, and pull actions.
- **Bundles**: search and filter OSGi bundles, then start, stop, or refresh them.
- **Explorer**: components, templates, policies, client libraries, dialogs,
  HTL files, models, JavaScript, and SCSS.

### AEM server tools

- Configurable AEM Author URL and credentials.
- Open CRXDE Lite, Package Manager, and Web Console.
- Upload and install FileVault packages.
- Pull repository content as a FileVault package.
- Right-click files or directories below `jcr_root` to upload or download them.
  Ordinary files use WebDAV. `.content.xml` uploads are parsed and applied as
  JCR node/property updates through Sling POST.
- Retrieve Felix bundle JARs and create a remote debug configuration.

## Installation

1. Build the plugin:

   ```shell
   ./gradlew buildPlugin
   ```

   On Windows:

   ```powershell
   .\gradlew.bat buildPlugin
   ```

2. In IntelliJ IDEA, open **Settings | Plugins**.
3. Select the gear menu and choose **Install Plugin from Disk**.
4. Select the ZIP generated under `build/distributions`.
5. Restart IntelliJ IDEA.

## Configure AEM Author

1. Open **Settings | Tools | AEM Toolkit**.
2. Enable server features.
3. Enter the Author base URL, username, and password.
4. Test the connection and apply the settings.

Credentials are stored through IntelliJ's password-safe APIs rather than in the
project files.

## Content synchronization

Select a file or directory below `jcr_root`, then use the **AEM** submenu in the
Project View or editor context menu:

- **Download from AEM** overwrites matching local files while retaining
  local-only files.
- **Upload to AEM** synchronizes the selected resource to the configured Author.

Upload and lifecycle actions modify the configured AEM instance. Use an account
with only the repository and Web Console permissions required for your workflow.
Review the target path before confirming an operation.

## Development

Run all tests:

```shell
./gradlew test
```

Start an IntelliJ development sandbox:

```shell
./gradlew runIde
```

Build the installable ZIP:

```shell
./gradlew buildPlugin
```

The project uses Kotlin, Gradle Kotlin DSL, IntelliJ Platform SDK 2025.1, and
the XML PSI API. Production code is under
`src/main/kotlin/com/github/aemtoolkit`; extension registrations are in
`src/main/resources/META-INF/plugin.xml`.

## Current version

`0.7.7`

## Author

Created by [cheese-zhang](https://github.com/cheese-zhang). Source code and issue
tracking are available at
[cheese-zhang/AEM-tools](https://github.com/cheese-zhang/AEM-tools).

## Notes

- AEM server compatibility depends on the target AEM/Felix version and enabled
  endpoints.
- Dialog inheritance can resolve sources available in the project. Inherited
  sources that exist only in remote packages are not indexed locally.
- Direct server operations require suitable repository, Package Manager, or
  Web Console permissions.
