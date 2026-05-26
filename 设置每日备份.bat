@echo off
chcp 65001 > nul
echo.
echo ========================================
echo   设置每日中午 12:00 自动备份
echo ========================================
echo.
echo 正在创建计划任务...
echo.
powershell -ExecutionPolicy Bypass -Command "$action=New-ScheduledTaskAction -Execute 'powershell.exe' -Argument '-ExecutionPolicy Bypass -WindowStyle Hidden -File \"d:\blog01\auto-backup.ps1\"'; $trigger=New-ScheduledTaskTrigger -Daily -At '12:00'; $settings=New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable; $principal=New-ScheduledTaskPrincipal -UserId \"$env:USERNAME\" -LogonType Interactive -RunLevel Limited; Unregister-ScheduledTask -TaskName 'BlogDailyBackup' -Confirm:$false -ErrorAction SilentlyContinue; Register-ScheduledTask -TaskName 'BlogDailyBackup' -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Force; Write-Host '[OK] 已完成！每天中午 12:00 自动备份到 D:\web_beifen' -ForegroundColor Green"
echo.
pause