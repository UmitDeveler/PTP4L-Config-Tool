@echo off
echo Building and packaging PTP4L Configuration Tool for Windows...
echo.

REM Maven'ı çalıştır. Tüm derleme ve paketleme mantığı pom.xml dosyasındadır.
call mvn clean package

REM Hata kontrolü
if %ERRORLEVEL% neq 0 (
    echo.
    echo HATA: Maven derlemesi başarısız oldu! Yukarıdaki hata mesajlarını kontrol edin.
    echo 'target' klasorunun baska bir program (Dosya Gezgini, Terminal, vb.) tarafindan kilitli olmadigindan emin olun.
    pause
    exit /b 1
)

echo.
echo =======================================================
echo  BASARILI: Uygulama paketi olusturuldu!
echo  Konum: target\dist\
echo =======================================================
echo.

pause