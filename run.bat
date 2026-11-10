<# : run.bat - Chan truy cap web truc tiep bang IP, chi cho Cloudflare vao port 80/443
@echo off
title CF-Only-Web
setlocal
set "RUN_MODE=%~1"

net session >nul 2>&1
if %errorlevel%==0 goto :RUN
echo Dang xin quyen Administrator...
if /i "%~1"=="undo" (
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -ArgumentList 'undo' -Verb RunAs"
) else (
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
)
exit /b

:RUN
powershell -NoProfile -ExecutionPolicy Bypass -Command "iex ([IO.File]::ReadAllText('%~f0'))"
echo.
pause
exit /b
#>
$ErrorActionPreference = 'Stop'
$ruleName = 'CF-Only-Web'
$isWebPort = {
    $pf = $_ | Get-NetFirewallPortFilter
    $pf.Protocol -eq 'TCP' -and $pf.LocalPort -ne 'Any' -and
    (@($pf.LocalPort) -contains '80' -or @($pf.LocalPort) -contains '443')
}

$extraPorts = @()

[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

if ($env:RUN_MODE -eq 'undo') {
    Write-Host '=== GO BO: mo lai web cho moi IP nhu cu ===' -ForegroundColor Yellow
    try { Remove-NetFirewallRule -DisplayName $ruleName -ErrorAction Stop } catch {}
    # Mo lai 80/443 cho moi IP (vi firewall van dang bat)
    try { Remove-NetFirewallRule -DisplayName 'CF-Undo-OpenWeb' -ErrorAction Stop } catch {}
    New-NetFirewallRule -DisplayName 'CF-Undo-OpenWeb' -Direction Inbound -Action Allow `
        -Protocol TCP -LocalPort 80,443 | Out-Null
    $apacheRules = Get-NetFirewallApplicationFilter |
        Where-Object { $_.Program -like '*httpd.exe*' } | Get-NetFirewallRule
    if ($apacheRules) { $apacheRules | Enable-NetFirewallRule }
    Write-Host 'Da go bo xong. Web (80/443) mo cho moi IP. Firewall van BAT.' -ForegroundColor Green
    Write-Host 'Rule port game (CF-GamePorts) van giu nguyen neu co.'
    exit
}

Write-Host '=== CHAN TRUY CAP TRUC TIEP - CHI CHO CLOUDFLARE VAO PORT 80/443 ===' -ForegroundColor Cyan

Write-Host '[1/5] Tai danh sach dai IP Cloudflare...'
$cf = @()
try {
    $cf += (Invoke-RestMethod 'https://www.cloudflare.com/ips-v4' -TimeoutSec 15) -split '\s+'
    $cf += (Invoke-RestMethod 'https://www.cloudflare.com/ips-v6' -TimeoutSec 15) -split '\s+'
} catch {
    Write-Host '  Khong tai duoc tu cloudflare.com, dung danh sach du phong.' -ForegroundColor Yellow
}
$cf = @($cf | Where-Object { $_ })
if ($cf.Count -eq 0) {
    $cf = @(
        '173.245.48.0/20','103.21.244.0/22','103.22.200.0/22','103.31.4.0/22',
        '141.101.64.0/18','108.162.192.0/18','190.93.240.0/20','188.114.96.0/20',
        '197.234.240.0/22','198.41.128.0/17','162.158.0.0/15','104.16.0.0/13',
        '104.24.0.0/14','172.64.0.0/13','131.0.72.0/22',
        '2400:cb00::/32','2606:4700::/32','2803:f800::/32','2405:b500::/32',
        '2405:8100::/32','2a06:98c0::/29','2c0f:f248::/32'
    )
}
Write-Host ("  -> {0} dai IP Cloudflare" -f $cf.Count)

Write-Host '[2/5] Tat cac rule cu dang mo web cho moi IP...'
$daTat = 0

$old = Get-NetFirewallApplicationFilter |
    Where-Object { $_.Program -like '*httpd.exe*' } | Get-NetFirewallRule |
    Where-Object { $_.Direction -eq 'Inbound' -and $_.Action -eq 'Allow' -and $_.Enabled -eq 'True' }
foreach ($x in $old) {
    Disable-NetFirewallRule -InputObject $x
    Write-Host ('  Da tat (chuong trinh): ' + $x.DisplayName)
    $daTat++
}

$cong = Get-NetFirewallRule -Direction Inbound -Action Allow -Enabled True -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -ne $ruleName } |
    Where-Object $isWebPort
foreach ($x in $cong) {
    Disable-NetFirewallRule -InputObject $x
    Write-Host ('  Da tat (cong 80/443): ' + $x.DisplayName)
    $daTat++
}

if ($daTat -eq 0) { Write-Host '  Khong co rule cu nao dang bat.' }

try { Remove-NetFirewallRule -DisplayName 'CF-Undo-OpenWeb' -ErrorAction Stop } catch {}

Write-Host '[3/5] Tao rule chi cho IP Cloudflare vao port 80/443...'
try { Remove-NetFirewallRule -DisplayName $ruleName -ErrorAction Stop } catch {}
New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Action Allow `
    -Protocol TCP -LocalPort 80,443 -RemoteAddress $cf | Out-Null
Write-Host '  Da tao rule CF-Only-Web.'

Write-Host '[4/5] Mo port game server / dich vu khac...'
try { Remove-NetFirewallRule -DisplayName 'CF-GamePorts' -ErrorAction Stop } catch {}
if ($extraPorts.Count -gt 0) {
    New-NetFirewallRule -DisplayName 'CF-GamePorts' -Direction Inbound -Action Allow `
        -Protocol TCP -LocalPort $extraPorts | Out-Null
    Write-Host ('  Da mo port: ' + ($extraPorts -join ', '))
} else {
    Write-Host '  Khong co port nao duoc khai bao ($extraPorts trong file nay).'
}

Write-Host '[5/5] Bat Windows Firewall (tat ca profile)...'
Set-NetFirewallProfile -Profile Domain,Private,Public -Enabled True
$warn = $false
foreach ($p in Get-NetFirewallProfile) {
    if ($p.Enabled -eq 'False') {
        Write-Host ("  CANH BAO: Firewall van TAT o profile '{0}'!" -f $p.Name) -ForegroundColor Red
        $warn = $true
    } elseif ($p.DefaultInboundAction -eq 'Allow') {
        Write-Host ("  CANH BAO: Profile '{0}' dang mac dinh CHO PHEP ket noi vao -> rule khong co tac dung!" -f $p.Name) -ForegroundColor Red
        $warn = $true
    }
}
if (-not $warn) { Write-Host '  OK: Firewall da BAT va mac dinh chan ket noi tu ngoai vao.' }

Write-Host ''
Write-Host '[6/6] Tu kiem lai...'
$sot = Get-NetFirewallRule -Direction Inbound -Action Allow -Enabled True -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -ne $ruleName -and $_.DisplayName -ne 'CF-GamePorts' } |
    Where-Object $isWebPort
$sotApp = Get-NetFirewallApplicationFilter |
    Where-Object { $_.Program -like '*httpd.exe*' } | Get-NetFirewallRule |
    Where-Object { $_.Direction -eq 'Inbound' -and $_.Action -eq 'Allow' -and $_.Enabled -eq 'True' }

if ($sot -or $sotApp) {
    Write-Host '  VAN CON RULE MO WEB CHO MOI IP:' -ForegroundColor Red
    foreach ($x in @($sot) + @($sotApp)) { Write-Host ('    - ' + $x.DisplayName) -ForegroundColor Red }
    Write-Host '  -> Chan CHUA co tac dung. Tat cac rule tren roi chay lai.' -ForegroundColor Red
} else {
    Write-Host '  OK: khong con rule nao mo 80/443 cho moi IP.' -ForegroundColor Green
}

Write-Host ''
Write-Host 'XONG! Gio chi Cloudflare ket noi duoc vao web (port 80/443).' -ForegroundColor Green
Write-Host ' - Go thang IP tu ben ngoai se bi timeout.'
Write-Host ' - localhost tren chinh may nay van vao binh thuong.'
Write-Host ' - Neu game server chay tren may nay: mo file run.bat, tim dong $extraPorts,'
Write-Host '   dien port vao roi chay lai run.bat.'
Write-Host ' - Muon mo lai nhu cu: chay    run.bat undo'
