[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$Device,

    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = 'Stop'
$targetPackage = 'dev.vignesh.cycleshop.isolated'
$testPackage = "$targetPackage.test"
$sdk = Split-Path (Split-Path $Adb -Parent) -Parent
$aapt = Get-ChildItem (Join-Path $sdk 'build-tools') -Recurse -Filter 'aapt.exe' |
    Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
if (-not $aapt) { throw 'Install Android SDK Build Tools to verify the APK package IDs.' }

function Invoke-Checked([string]$Program, [string[]]$Arguments) {
    $result = & $Program @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Program exited with code $LASTEXITCODE" }
    $result
}

function Assert-Package([string]$Apk, [string]$PackageName) {
    $badging = Invoke-Checked $aapt @('dump', 'badging', $Apk)
    if (-not ($badging -match "^package: name='$([regex]::Escape($PackageName))' ")) {
        throw "Expected $PackageName in $Apk. Installation refused."
    }
}

Push-Location $PSScriptRoot
try {
    Invoke-Checked '.\gradlew.bat' @(':app:assembleIsolated', ':app:assembleIsolatedAndroidTest')
    $appApk = 'app/build/outputs/apk/isolated/app-isolated.apk'
    $testApk = 'app/build/outputs/apk/androidTest/isolated/app-isolated-androidTest.apk'
    Assert-Package $appApk $targetPackage
    Assert-Package $testApk $testPackage
    $manifest = Invoke-Checked $aapt @('dump', 'xmltree', $testApk, 'AndroidManifest.xml')
    if (-not ($manifest -match "targetPackage.*`"$([regex]::Escape($targetPackage))`"")) {
        throw 'Instrumentation manifest does not target the isolated app.'
    }
    Invoke-Checked $Adb @('-s', $Device, 'install', '-r', '-t', $appApk)
    Invoke-Checked $Adb @('-s', $Device, 'install', '-r', '-t', $testApk)
    $report = Invoke-Checked $Adb @(
        '-s', $Device, 'shell', 'am', 'instrument', '-w', '-r',
        "$testPackage/dev.vignesh.cycleshop.IsolatedTestRunner"
    )
    $report
    if (-not ($report -match '^OK \([1-9]\d* tests?\)$') -or
        -not ($report -match '^INSTRUMENTATION_CODE: -1$')) {
        throw 'Instrumentation did not report a successful test run.'
    }
}
finally {
    Pop-Location
}
