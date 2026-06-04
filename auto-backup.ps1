﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿$ErrorActionPreference = 'Continue'

$BACKUP_DIR = 'D:\web_beifen'
$DB_DIR = 'd:\blog01\backend\data'
$BACKEND_JAR = 'd:\blog01\backend\target\blog-backend-1.0.0.jar'
$KEEP_DAYS = 7

Write-Host ''
Write-Host '========================================' -ForegroundColor Cyan
Write-Host '  博客数据库自动备份' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host ''

if (-not (Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR -Force | Out-Null
    Write-Host "[OK] 创建备份目录" -ForegroundColor Green
}

Write-Host ''
Write-Host '[1/5] 正在关闭后端进程 (端口 8080)...' -ForegroundColor Yellow
$backendStopped = $false
netstat -ano 2>$null | Select-String ':8080\s' | ForEach-Object {
    $parts = $_.Line -split '\s+'
    $pid = $parts[-1]
    if ($pid -match '^\d+$') {
        Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
        Write-Host "  已停止后端进程 (PID: $pid)"
        $backendStopped = $true
    }
}
if (-not $backendStopped) { Write-Host '  后端未运行' }

Write-Host '[1/5] 正在关闭前端进程 (端口 5173)...' -ForegroundColor Yellow
$frontendStopped = $false
netstat -ano 2>$null | Select-String ':5173\s' | ForEach-Object {
    $parts = $_.Line -split '\s+'
    $pid = $parts[-1]
    if ($pid -match '^\d+$') {
        Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
        Write-Host "  已停止前端进程 (PID: $pid)"
        $frontendStopped = $true
    }
}
if (-not $frontendStopped) { Write-Host '  前端未运行' }
Start-Sleep -Seconds 2

Write-Host ''
Write-Host '[2/5] 正在备份数据库...' -ForegroundColor Yellow
if (-not (Test-Path $DB_DIR)) {
    Write-Host "  [错误] 数据库目录不存在" -ForegroundColor Red
}
else {
    $dbFiles = Get-ChildItem -Path $DB_DIR -Include '*.mv.db', '*.trace.db' -ErrorAction SilentlyContinue
    if ($dbFiles) {
        $timestamp = Get-Date -Format 'yyyy-MM-dd_HH-mm'
        $zipFile = "$BACKUP_DIR\blog_backup_${timestamp}.zip"
        foreach ($f in $dbFiles) {
            $kb = [math]::Round($f.Length / 1KB, 1)
            Write-Host "  $($f.Name) (${kb} KB)"
        }
        $tempDir = "$env:TEMP\blog_backup_temp"
        if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
        New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
        Copy-Item "$DB_DIR\*.mv.db" -Destination $tempDir -Force -ErrorAction SilentlyContinue
        Copy-Item "$DB_DIR\*.trace.db" -Destination $tempDir -Force -ErrorAction SilentlyContinue
        Compress-Archive -Path "$tempDir\*" -DestinationPath $zipFile -Force
        Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
        $zipKb = [math]::Round((Get-Item $zipFile).Length / 1KB, 1)
        Write-Host "  [OK] 已备份 (${zipKb} KB)" -ForegroundColor Green
    }
    else {
        Write-Host '  [警告] 未找到数据库文件' -ForegroundColor Yellow
    }
}

Write-Host ''
Write-Host '[3/5] 正在清理旧备份...' -ForegroundColor Yellow
$cutoff = (Get-Date).AddDays(-$KEEP_DAYS)
$oldBackups = Get-ChildItem -Path $BACKUP_DIR -Filter 'blog_backup_*.zip' | Where-Object { $_.LastWriteTime -lt $cutoff }
if ($oldBackups) {
    $oldBackups | Remove-Item -Force
    Write-Host "  已删除 $($oldBackups.Count) 个旧备份" -ForegroundColor Green
}
else {
    Write-Host '  无需清理'
}

Write-Host ''
Write-Host '[4/5] 正在重启后端...' -ForegroundColor Yellow
if ($backendStopped) {
    if (Test-Path $BACKEND_JAR) {
        Start-Process -FilePath 'java' -ArgumentList '-jar', $BACKEND_JAR -WorkingDirectory 'd:\blog01\backend' -WindowStyle Minimized
        Write-Host '  [OK] 后端已重新启动' -ForegroundColor Green
    }
    else {
        Write-Host '  [警告] 找不到 JAR 文件，请手动启动' -ForegroundColor Yellow
    }
}
else {
    Write-Host '  后端此前未运行，跳过重启'
}

Write-Host ''
Write-Host '[5/5] 正在重启前端...' -ForegroundColor Yellow
if ($frontendStopped) {
    if (Test-Path 'd:\blog01\my-blog') {
        Start-Process -FilePath 'npm' -ArgumentList 'run', 'dev' -WorkingDirectory 'd:\blog01\my-blog' -WindowStyle Minimized
        Write-Host '  [OK] 前端已重新启动' -ForegroundColor Green
    }
}
else {
    Write-Host '  前端此前未运行，跳过重启'
}

Write-Host ''
Write-Host '========================================' -ForegroundColor Cyan
Write-Host '  备份完成！' -ForegroundColor Green
Write-Host "  备份目录: $BACKUP_DIR"
Write-Host '========================================' -ForegroundColor Cyan