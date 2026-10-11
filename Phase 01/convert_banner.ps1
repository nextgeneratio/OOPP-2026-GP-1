Add-Type -AssemblyName PresentationCore
$webpPath = Resolve-Path "src/main/resources/login_bg.webp"
$pngPath = [System.IO.Path]::Combine((Split-Path $webpPath), "faculty_home_banner.png")
$stream = [System.IO.File]::OpenRead($webpPath)
try {
    $decoder = [System.Windows.Media.Imaging.BitmapDecoder]::Create($stream, [System.Windows.Media.Imaging.BitmapCreateOptions]::None, [System.Windows.Media.Imaging.BitmapCacheOption]::OnLoad)
    $encoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $encoder.Frames.Add($decoder.Frames[0])
    $outStream = [System.IO.File]::OpenWrite($pngPath)
    $encoder.Save($outStream)
    $outStream.Close()
    Write-Host "SUCCESS: Saved to $pngPath"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
} finally {
    $stream.Close()
}
