param(
    [Parameter(Mandatory=$true)]
    [ValidatePattern('^([A-Fa-f0-9]{2}:){31}[A-Fa-f0-9]{2}$')]
    [string]$CertificateSha256,
    [Parameter(Mandatory=$true)]
    [string]$OutputPath
)
$ErrorActionPreference = 'Stop'
$resolvedOutput = [IO.Path]::GetFullPath($OutputPath)
if (Test-Path -LiteralPath $resolvedOutput) {
    throw 'O arquivo já existe. Preserve as associações atuais e revise a inclusão manualmente.'
}
$statement = @(
    @{
        relation = @('delegate_permission/common.handle_all_urls')
        target = @{
            namespace = 'android_app'
            package_name = 'com.gastointeligente.app'
            sha256_cert_fingerprints = @($CertificateSha256.ToUpperInvariant())
        }
    }
)
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($resolvedOutput)) | Out-Null
$json = ConvertTo-Json -InputObject $statement -Depth 8
[IO.File]::WriteAllText($resolvedOutput, $json + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
Write-Output "Associação gerada em $resolvedOutput. Publique somente após conferir o certificado do APK distribuído."
