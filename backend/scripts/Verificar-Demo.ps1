param([int]$Port = 18082, [string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw 'Configura JAVA_HOME con un JDK 25 o superior.' }
$backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$target = Join-Path $backend 'target'
$jar = Join-Path $target 'umbral-backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Ejecuta primero mvnw.cmd -B -Pdemo package.' }
$base = "http://127.0.0.1:$Port"
$evidencia = [Collections.Generic.List[object]]::new()
$previousSecret = $env:UMBRAL_JWT_SECRET
$random = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($random)
$rng.Dispose()
$env:UMBRAL_JWT_SECRET = [Convert]::ToBase64String($random)

function Base64Url([byte[]]$bytes) {
    [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
}
function Token([long]$UserId) {
    $header = Base64Url ([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}'))
    $claims = @{ user_id="$UserId";token_type='access';exp=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()+600;jti=[Guid]::NewGuid().ToString() } | ConvertTo-Json -Compress
    $payload = Base64Url ([Text.Encoding]::UTF8.GetBytes($claims))
    $hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($env:UMBRAL_JWT_SECRET))
    try { $signature = Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes("$header.$payload"))) }
    finally { $hmac.Dispose() }
    "$header.$payload.$signature"
}
function Comprobar([string]$Method,[string]$Path,[int]$Expected,[long]$UserId=0,$Body=$null) {
    $argsHttp = @{ Uri="$base$Path";Method=$Method;UseBasicParsing=$true;TimeoutSec=15 }
    if ($UserId -gt 0) { $argsHttp.Headers=@{Authorization="Bearer $(Token $UserId)"} }
    if ($null -ne $Body) { $argsHttp.ContentType='application/json; charset=utf-8';$argsHttp.Body=($Body | ConvertTo-Json -Compress) }
    $response = $null
    try { $response=Invoke-WebRequest @argsHttp;$status=[int]$response.StatusCode }
    catch { if ($null -eq $_.Exception.Response) { throw };$status=[int]$_.Exception.Response.StatusCode }
    $evidencia.Add([pscustomobject]@{method=$Method;path=$Path;status=$status;expected=$Expected})
    if ($status -ne $Expected) { throw "$Method $Path devolvio $status; se esperaba $Expected" }
    if ($null -ne $response -and $response.Content) { $response.Content | ConvertFrom-Json }
}

$demo = $null
try {
    # Solo este proceso recibe la clave efimera. No se escribe en archivos ni en Git.
    $demo = Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -WindowStyle Hidden -PassThru `
        -ArgumentList @('-jar',('"'+$jar+'"'),'--spring.profiles.active=demo',"--server.port=$Port",'--umbral.resumen.reintento-ms=1000') `
        -RedirectStandardOutput (Join-Path $target 'smoke-demo-stdout.log') -RedirectStandardError (Join-Path $target 'smoke-demo-stderr.log')
    $ready = $false
    for ($i=0;$i -lt 45;$i++) {
        if ($demo.HasExited) { throw 'El demo se detuvo; revisa target/smoke-demo-stdout.log.' }
        try { $null=Invoke-WebRequest -UseBasicParsing -Uri "$base/api/practicas/preguntas?tema=Algebra&dificultad=FACIL" -TimeoutSec 2;$ready=$true;break }
        catch { Start-Sleep -Seconds 1 }
    }
    if (-not $ready) { throw 'El demo no inicio a tiempo.' }
    $preguntas=Comprobar GET '/api/practicas/preguntas?tema=Algebra&dificultad=FACIL' 200
    $pregunta=@($preguntas)[0].id
    $null=Comprobar GET "/api/practicas/preguntas/$pregunta/solucionario" 403 2
    $null=Comprobar POST '/api/practicas/respuestas' 201 2 @{preguntaId=$pregunta;respuesta='4'}
    $solucion=Comprobar POST "/api/docente/preguntas/$pregunta/solucionario" 200 1
    $null=Comprobar PUT "/api/docente/solucionarios/$($solucion.id)/aprobar" 200 1
    $null=Comprobar GET "/api/practicas/preguntas/$pregunta/solucionario" 200 2
    $duda=Comprobar POST '/api/alumno/dudas' 201 2 @{docenteId=1;seccionId=1;preguntaId=$pregunta;texto='No entiendo el despeje'}
    $null=Comprobar GET '/api/docente/dudas/grupos' 200 1
    $null=Comprobar POST "/api/docente/dudas/grupos/$($duda.grupoId)/respuestas" 201 1 @{texto='Restar seis y dividir entre dos.'}
    $null=Comprobar GET "/api/alumno/dudas/$($duda.id)/respuesta" 200 2
    $null=Comprobar PUT "/api/alumno/dudas/$($duda.id)/resolver" 200 2
    $null=Comprobar GET "/api/alumno/dudas/$($duda.id)/historial" 200 2
    $null=Comprobar GET '/api/alumno/dudas?estado=RESUELTA' 200 2
    $null=Comprobar GET '/api/apoderado/alumnos/2/simulacros?desde=2026-11-01&hasta=2026-11-03' 200 3
    $null=Comprobar GET '/api/apoderado/alumnos/4/simulacros' 403 3
    $null=Comprobar GET '/api/docente/resumen-semanal' 200 1
    $pdf=Join-Path $target 'smoke-demo-rendimiento.pdf'
    $res=Invoke-WebRequest -UseBasicParsing -Uri "$base/api/docente/secciones/1/rendimiento.pdf" `
        -Headers @{Authorization="Bearer $(Token 1)"} -OutFile $pdf -PassThru -TimeoutSec 15
    if ($res.Headers['Content-Type'] -notmatch 'application/pdf' -or (Get-Item -LiteralPath $pdf).Length -lt 100) { throw 'Descarga PDF invalida' }
    $evidencia.Add([pscustomobject]@{method='GET';path='/api/docente/secciones/1/rendimiento.pdf';status=[int]$res.StatusCode;expected=200})
    $simulado = $false
    for ($i=0;$i -lt 8;$i++) {
        if (Select-String -LiteralPath (Join-Path $target 'smoke-demo-stdout.log') -Pattern 'Correo simulado preparado' -Quiet) { $simulado=$true;break }
        Start-Sleep -Seconds 1
    }
    if (-not $simulado) { throw 'No se verifico ejecucion programada del correo simulado' }
    $evidencia | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $target 'smoke-demo-resultados.json') -Encoding UTF8
    Write-Output "$($evidencia.Count) solicitudes HTTP correctas; PDF descargado y correo programado simulado."
} finally {
    if ($null -ne $demo -and -not $demo.HasExited) { Stop-Process -Id $demo.Id }
    $env:UMBRAL_JWT_SECRET=$previousSecret
}
