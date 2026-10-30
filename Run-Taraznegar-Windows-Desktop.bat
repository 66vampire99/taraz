@echo off
title Taraznegar Desktop App
start msedge --app="%~dp0app\src\main\assets\index.html" --window-size=450,860 2>nul || start chrome --app="%~dp0app\src\main\assets\index.html" --window-size=450,860
