$loginBody = '{"email":"test@test.com","password":"test123"}'
try {
    $loginRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/users/login' -Method POST -Body $loginBody -ContentType 'application/json' -TimeoutSec 10 -UseBasicParsing
    $token = $loginRes.token
    Write-Output "LOGIN SUCCESS"
    Write-Output "Email: $($loginRes.email)"
    Write-Output "Token (first 40 chars): $($token.Substring(0, [Math]::Min(40, $token.Length)))..."
    
    # Now test wallet endpoint
    $headers = @{ Authorization = "Bearer $token" }
    $walletRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/wallet' -Headers $headers -TimeoutSec 10 -UseBasicParsing
    Write-Output "WALLET RESPONSE:"
    $walletRes | ConvertTo-Json
    
    # Test portfolio endpoint
    $portfolioRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/portfolio' -Headers $headers -TimeoutSec 10 -UseBasicParsing
    Write-Output "PORTFOLIO RESPONSE:"
    $portfolioRes | ConvertTo-Json -Depth 3
} catch {
    Write-Output "ERROR: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        Write-Output "Response body: $($reader.ReadToEnd())"
    }
}
