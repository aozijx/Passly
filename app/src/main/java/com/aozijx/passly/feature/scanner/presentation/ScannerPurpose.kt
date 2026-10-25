package com.aozijx.passly.feature.scanner.presentation

enum class ScannerPurpose {
    General,
    ApplyToOtpEditor,
    ApplyToWifiEditor,
    ;

    fun resolveImport(result: ScannerResult): ScannerImportResolution = when (result) {
        is ScannerResult.Otp -> if (this == General || this == ApplyToOtpEditor) {
            ScannerImportResolution.Available(ScannerImport.Otp(result.config))
        } else {
            ScannerImportResolution.Unavailable
        }
        is ScannerResult.Wifi -> if (this == General || this == ApplyToWifiEditor) {
            ScannerImportResolution.Available(ScannerImport.Wifi(result.payload))
        } else {
            ScannerImportResolution.Unavailable
        }
        is ScannerResult.WebLink,
        is ScannerResult.PlainText,
        -> ScannerImportResolution.Unavailable
    }
}
