# Kovax FlowOps installer

This installer uses Inno Setup and WinSW. The repository intentionally does not include third-party binaries.

Before compiling, place these files in `installer\prerequisites\`:

- `WinSW-x64.exe`, renamed to `KovaxFlowOps.exe` in the build payload.
- Official PostgreSQL Windows installer, named `postgresql-installer.exe`, so the installer can offer automatic PostgreSQL installation.

The build script creates a private Java 21 runtime with `jlink` from the developer JDK, prepares the JAR and helper scripts, then calls `ISCC.exe`. It does not download Java, PostgreSQL, or WinSW; you must place the approved prerequisite binaries before building. It does not store credentials in installer source.

Runtime binaries install under `C:\Program Files\Kovax FlowOps`. Writable data is kept under `C:\ProgramData\Kovax FlowOps` and is deliberately retained on uninstall. The PostgreSQL administrator password is passed through a temporary setup file, used to create `kovax_flowops` and `kovax_user`, then deleted. Application credentials are stored only in the protected ProgramData config directory.
