# Security Policy

## Supported Versions

The current version on the `main` branch is supported.

## Reporting a Vulnerability

Do not publish vulnerability details in an open issue.

If GitHub private vulnerability reporting is enabled for this repository, use it. If it is not
available, open an issue without exploit details and ask the maintainer for a private channel to
share technical information.

Include:

- affected version or commit;
- brief risk summary;
- minimal reproduction steps, if they can be shared safely;
- expected impact;
- known workaround, if any.

This project handles a SonarQube user token and exposes source code and quality findings to AI
clients, so reports about token leaks, write operations against the SonarQube web-api, corruption
of the stdio JSON-RPC channel, and unintended exposure of private projects or URLs are especially
important.
