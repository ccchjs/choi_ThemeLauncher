@echo off
echo === Theme Launcher: push sa GitHub ===
where git >nul 2>&1 || (echo Walang Git. I-install muna sa https://git-scm.com & pause & exit /b)
set /p REPO=Ilagay ang GitHub repo URL (hal. https://github.com/username/ThemeLauncher.git): 
git init
git add .
git commit -m "Theme Launcher"
git branch -M main
git remote remove origin >nul 2>&1
git remote add origin %REPO%
git push -u origin main
echo.
echo Tapos na. Buksan ang repo mo sa GitHub > tab na Actions para makita ang build.
pause
