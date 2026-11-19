$adb = "C:\Users\ramas\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$device = "192.168.0.9:37157"

Write-Host "Connecting to OnePlus 9R at $device..."
& $adb connect $device

Write-Host "Installing app-debug.apk..."
& $adb -s $device install -r -t "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "Installing app-debug-androidTest.apk..."
& $adb -s $device install -r -t "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"

Write-Host "Executing on-device instrumentation tests (VglDatabaseTest)..."
& $adb -s $device shell am instrument -w -r -e class dev.vignesh.cycleshop.data.local.VglDatabaseTest dev.vignesh.cycleshop.test/androidx.test.runner.AndroidJUnitRunner
