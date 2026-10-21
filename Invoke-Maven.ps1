# Select this project's JDK only for the Maven process, then restore the session.
# Set CA1_JAVA_HOME / CA1_MAVEN_HOME if your installations are elsewhere.
$ErrorActionPreference = 'Stop'
$projectPath = Join-Path $PSScriptRoot 'CA1\autopilot\pom.xml'
$jdkPath = $env:CA1_JAVA_HOME
if (-not $jdkPath) {
    $jdkPath = Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium\jdk-26.0.2.1+1'
}
if (-not (Test-Path (Join-Path $jdkPath 'bin\javac.exe'))) {
    throw 'Set CA1_JAVA_HOME to your Temurin 26 JDK installation.'
}
$mavenPath = $null
if ($env:CA1_MAVEN_HOME) {
    $mavenPath = Join-Path $env:CA1_MAVEN_HOME 'bin\mvn.cmd'
} else {
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { $mavenPath = $command.Source }
    else {
        $candidates = @(Get-ChildItem 'C:\Program Files\JetBrains\IntelliJ IDEA *\plugins\maven-plugin\lib\maven3\bin\mvn.cmd' -ErrorAction SilentlyContinue)
        if ($candidates.Count -gt 0) { $mavenPath = $candidates[-1].FullName }
    }
}
if (-not $mavenPath -or -not (Test-Path $mavenPath)) {
    throw 'Set CA1_MAVEN_HOME to your Maven installation.'
}
$previousJavaHome = $env:JAVA_HOME
try {
    $env:JAVA_HOME = $jdkPath
    & $mavenPath -f $projectPath @args
    $mavenExitCode = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJavaHome
}
exit $mavenExitCode
