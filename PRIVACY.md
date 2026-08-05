# Privacy Policy

Last updated: August 5, 2026

AEM Toolkit is an IntelliJ IDEA plugin developed by Cheese. This policy
describes how the plugin handles data.

## Data collection

AEM Toolkit does not include analytics, advertising, telemetry, or crash
reporting, and it does not send project files or usage data to the plugin
author.

## AEM connections

Server features connect only to the AEM Author base URL configured by the
user. When the user invokes a server action, the plugin may send credentials,
repository paths, content, packages, or bundle commands directly to that AEM
instance. The operator of that instance may log or process those requests
under its own policies.

The plugin does not proxy AEM requests through infrastructure controlled by
the plugin author.

## Credentials

AEM usernames and passwords are stored through the IntelliJ Platform
Password Safe. Their protection and synchronization behavior depends on the
Password Safe option selected in the IDE. Credentials are read only when
needed for a user-requested AEM operation.

## Local processing

Project indexing, code completion, navigation, inspections, content-tree
rendering, and configuration analysis are performed locally by the IDE. The
plugin may write normal IntelliJ settings and cache data managed by the IDE.

## External links

Actions that open CRXDE Lite, Package Manager, Web Console, documentation, or
the project website use the system browser. Those sites have their own
privacy policies.

## Data retention and deletion

The plugin author does not receive or retain personal data through the
plugin. Users can remove stored AEM credentials from AEM Toolkit settings or
their IDE Password Safe. Uninstalling the plugin removes its executable code;
the IDE controls removal of settings and caches.

## Changes

Material changes to this policy will be published in this repository and, when
appropriate, noted in the plugin release notes.

## Contact

Questions and privacy requests can be submitted through the
[AEM Toolkit issue tracker](https://github.com/cheese-zhang/AEM-tools/issues).
