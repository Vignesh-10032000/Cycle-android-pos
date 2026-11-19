$adb = "C:\Users\ramas\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$device = "192.168.0.9:37157"
$artifactDir = "C:\Users\ramas\.gemini\antigravity\brain\2411819a-a8a4-48d9-a2d7-504856f98161"

Write-Host "Connecting to OnePlus 9R..."
& $adb connect $device

Write-Host "Checking screen resolution..."
& $adb -s $device shell wm size

Write-Host "Starting MainActivity..."
& $adb -s $device shell am start -S -n dev.vignesh.cycleshop/.MainActivity

Start-Sleep -Seconds 3

Write-Host "Capturing Dashboard screenshot..."
& $adb -s $device shell screencap -p /sdcard/dashboard_live.png
& $adb -s $device pull /sdcard/dashboard_live.png "$artifactDir\dashboard_live_oneplus.png"

Write-Host "Dashboard screenshot saved to $artifactDir\dashboard_live_oneplus.png"
