@echo off
chcp 65001 > nul
echo.
echo ========================================
echo   博客数据库 - 手动备份
echo ========================================
echo.
powershell -ExecutionPolicy Bypass -File "%~dp0auto-backup.ps1"
pause