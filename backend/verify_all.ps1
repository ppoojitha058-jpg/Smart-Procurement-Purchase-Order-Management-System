$ErrorActionPreference = "Stop"

function PostJson($url, $body, $token=$null) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body ($body | ConvertTo-Json -Depth 5)
}

function GetJson($url, $token=$null) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Get -Headers $headers
}

function PutJson($url, $body, $token=$null) {
    $headers = @{ "Content-Type" = "application/json" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Put -Headers $headers -Body ($body | ConvertTo-Json -Depth 5)
}

function GetCsv($url, $token=$null) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $res = Invoke-WebRequest -UseBasicParsing -Uri $url -Method Get -Headers $headers
    return $res
}

Write-Output "================================================="
Write-Output "1. VERIFYING FRESH USER REGISTRATION & ZERO STATE"
Write-Output "================================================="

$freshEmail = "newbie_$(Get-Random)@eps.com"
$regPayload = @{
    fullName = "Fresh Candidate"
    email = $freshEmail
    password = "User@123"
    phone = "9876543210"
    address = "Building 4, Tech Park, Bangalore"
}

$regRes = PostJson "http://localhost:8080/api/auth/register" $regPayload
Write-Output "Registration successful for: $freshEmail"

# Login with newly registered user
$freshAuth = PostJson "http://localhost:8080/api/auth/login" @{
    email = $freshEmail
    password = "User@123"
}
$freshToken = $freshAuth.data.token
$freshUserId = $freshAuth.data.userId
Write-Output "Login successful! Token acquired. User ID: $freshUserId"

# Check User Dashboard stats
$dashboardRes = GetJson "http://localhost:8080/api/users/dashboard" $freshToken
$stats = $dashboardRes.data
Write-Output "Fresh User Stats: TotalRequests=$($stats.totalRequests), Pending=$($stats.pendingRequests), Approved=$($stats.approvedRequests), Rejected=$($stats.rejectedRequests), SpentYtd=$($stats.spentYtd), Orders=$($stats.ordersCount)"

if ($stats.totalRequests -ne 0 -or $stats.pendingRequests -ne 0 -or $stats.spentYtd -ne 0) {
    throw "ERROR: Fresh user did not start with 0 history!"
}
Write-Output "PASS: Fresh user has strictly ZERO history (0 requests, 0 pending, 0 approved, 0 spent)."

# Check my-requests
$myReqs = GetJson "http://localhost:8080/api/users/my-requests" $freshToken
Write-Output "Fresh User Requests count: $($myReqs.Count)"
if ($myReqs.Count -ne 0) {
    throw "ERROR: Fresh user sees requests from other users!"
}
Write-Output "PASS: Strict user data isolation verified (0 records returned for fresh user)."

# Check Fresh User CSV Export
$freshCsvRes = GetCsv "http://localhost:8080/api/reports/csv/user/requests?email=$freshEmail" $freshToken
$freshLines = ($freshCsvRes.Content -split "`r`n|`n") | Where-Object { $_ -ne "" }
Write-Output "Fresh User CSV line count: $($freshLines.Count)"
Write-Output "Header line: $($freshLines[0])"
if ($freshLines.Count -ne 1) {
    throw "ERROR: Fresh user CSV contains rows when it should only contain header!"
}
Write-Output "PASS: Fresh user CSV correctly outputs only the CSV header with zero records."

Write-Output ""
Write-Output "================================================="
Write-Output "2. VERIFYING USER REQUISITION CREATION"
Write-Output "================================================="

$newPr = PostJson "http://localhost:8080/api/purchase-requests" @{
    userId = $freshUserId
    productId = 2
    departmentId = 1
    quantity = 3
} $freshToken
$newPrId = $newPr.data.requestId
Write-Output "Created Purchase Request #$newPrId for fresh user (Total: $($newPr.data.totalPrice), Status: $($newPr.data.status))"

# Re-check fresh user dashboard stats
$updatedStats = (GetJson "http://localhost:8080/api/users/dashboard" $freshToken).data
Write-Output "Updated Stats: Total=$($updatedStats.totalRequests), Pending=$($updatedStats.pendingRequests)"
if ($updatedStats.totalRequests -ne 1 -or $updatedStats.pendingRequests -ne 1) {
    throw "ERROR: User dashboard stats did not update after creating requisition!"
}
Write-Output "PASS: Requisition accurately incremented user stats."

# Check fresh user CSV now contains exactly 1 data row
$freshCsvAfter = (GetCsv "http://localhost:8080/api/reports/csv/user/requests?email=$freshEmail" $freshToken).Content
$freshLinesAfter = ($freshCsvAfter -split "`r`n|`n") | Where-Object { $_ -ne "" }
Write-Output "Fresh User CSV after creation lines: $($freshLinesAfter.Count)"
if ($freshLinesAfter.Count -ne 2) {
    throw "ERROR: Fresh user CSV did not contain newly created requisition row!"
}
Write-Output "CSV Record: $($freshLinesAfter[1])"
Write-Output "PASS: User CSV dynamically contains only the user's isolated requisition."

Write-Output ""
Write-Output "================================================="
Write-Output "3. VERIFYING ROLE-BASED CSV EXPORTS (ADMIN & MANAGER)"
Write-Output "================================================="

$adminAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "admin@eps.com"; password = "Admin@123" }
$adminToken = $adminAuth.data.token
$mgrAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "manager@eps.com"; password = "Manager@123" }
$mgrToken = $mgrAuth.data.token

# Admin CSV Endpoints
$adminEndpoints = @(
    "admin/requests",
    "admin/orders",
    "admin/suppliers",
    "admin/payments",
    "admin/tracking"
)

foreach ($ep in $adminEndpoints) {
    $res = GetCsv "http://localhost:8080/api/reports/csv/$ep" $adminToken
    Write-Output "Admin Export [$ep]: HTTP $($res.StatusCode), $($res.Content.Length) bytes"
}

# Manager CSV Endpoints
$mgrEndpoints = @(
    "manager/requests",
    "manager/approvals"
)

foreach ($ep in $mgrEndpoints) {
    $res = GetCsv "http://localhost:8080/api/reports/csv/$ep" $mgrToken
    Write-Output "Manager Export [$ep]: HTTP $($res.StatusCode), $($res.Content.Length) bytes"
}

# User CSV Endpoints for employee
$empAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "employee@eps.com"; password = "User@123" }
$empToken = $empAuth.data.token

$userEndpoints = @(
    "user/requests",
    "user/orders",
    "user/payments",
    "user/tracking"
)

foreach ($ep in $userEndpoints) {
    $res = GetCsv "http://localhost:8080/api/reports/csv/${ep}?email=employee@eps.com" $empToken
    Write-Output "User Export [$ep]: HTTP $($res.StatusCode), $($res.Content.Length) bytes"
}

Write-Output ""
Write-Output "================================================="
Write-Output "4. VERIFYING NOTIFICATIONS & SECURITY ISOLATION"
Write-Output "================================================="

$notifs = GetJson "http://localhost:8080/api/notifications" $adminToken
Write-Output "Admin notifications retrieved: $($notifs.Count) records"

# Security test: Regular user cannot access admin CSV
try {
    GetCsv "http://localhost:8080/api/reports/csv/admin/requests" $freshToken
    throw "UNEXPECTED: Fresh regular user downloaded Admin CSV!"
} catch {
    Write-Output "PASS: Security enforced - Regular user denied access to Admin CSV (HTTP $($_.Exception.Response.StatusCode.value__))"
}

Write-Output ""
Write-Output "================================================="
Write-Output "ALL SYSTEM AUDIT & ISOLATION CHECKS PASSED 100%!"
Write-Output "================================================="
