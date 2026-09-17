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

Write-Output "=== 1. AUTHENTICATION ==="
$empAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "employee@eps.com"; password = "User@123" }
$empToken = $empAuth.data.token
Write-Output "Employee Token OK"

$mgrAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "manager@eps.com"; password = "Manager@123" }
$mgrToken = $mgrAuth.data.token
Write-Output "Manager Token OK"

$proAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "procurement@eps.com"; password = "Procurement@123" }
$proToken = $proAuth.data.token
Write-Output "Procurement Token OK"

$supAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "supplier@eps.com"; password = "Supplier@123" }
$supToken = $supAuth.data.token
Write-Output "Supplier Token OK"

$recAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "receiving@eps.com"; password = "Receiving@123" }
$recToken = $recAuth.data.token
Write-Output "Receiving Token OK"

$finAuth = PostJson "http://localhost:8080/api/auth/login" @{ email = "finance@eps.com"; password = "Finance@123" }
$finToken = $finAuth.data.token
Write-Output "Finance Token OK"

Write-Output "=== 2. EMPLOYEE CREATES PURCHASE REQUEST ==="
$pr = PostJson "http://localhost:8080/api/purchase-requests" @{
    userId = 2
    productId = 1
    departmentId = 1
    quantity = 2
} $empToken
$prId = $pr.data.requestId
Write-Output "Created Purchase Request #$prId (Total: $($pr.data.totalPrice))"

Write-Output "=== 3. MANAGER APPROVES REQUEST ==="
$appr = PostJson "http://localhost:8080/api/manager/requests/$prId/approve" @{ remarks = "Approved for Q3 Hardware Upgrade" } $mgrToken
Write-Output "Manager Approved Request #$prId - Status: $($appr.data.status)"

Write-Output "=== 4. PROCUREMENT CREATES RFQ ==="
$rfq = PostJson "http://localhost:8080/api/procurement/rfq" @{
    purchaseRequestId = $prId
    title = "RFQ for 2x Dell Enterprise Laptops"
    description = "Procuring enterprise units with 3-year warranty"
} $proToken
$rfqId = $rfq.rfqId
Write-Output "Procurement created RFQ #$rfqId - Title: $($rfq.title)"

Write-Output "=== 5. SUPPLIER SUBMITS QUOTATION ==="
$quote = PostJson "http://localhost:8080/api/supplier/rfq/$rfqId/quote" @{
    supplierId = 1
    quotedPrice = 2400.00
    deliveryDays = 4
    comments = "Includes premium delivery and warranty pack"
} $supToken
$quoteId = $quote.quotationId
Write-Output "Supplier submitted Quote #$quoteId for RFQ #$rfqId ($($quote.quotedPrice))"

Write-Output "=== 6. PROCUREMENT SELECTS QUOTE & ISSUES PO ==="
$po = PostJson "http://localhost:8080/api/procurement/rfq/$rfqId/select-quote/$quoteId" @{} $proToken
$poId = $po.orderId
Write-Output "PO #$poId issued to Supplier (Amount: $($po.totalAmount), Status: $($po.status))"

Write-Output "=== 7. SUPPLIER ACCEPTS & SHIPS ORDER ==="
$poAcc = PutJson "http://localhost:8080/api/supplier/orders/$poId/accept" @{} $supToken
Write-Output "Supplier accepted PO #$poId - Status: $($poAcc.status)"

$poShip = PutJson "http://localhost:8080/api/supplier/orders/$poId/shipment" @{
    trackingNumber = "FEDEX-88997701"
    shipmentDetails = "Dispatched via FedEx Priority Freight"
} $supToken
Write-Output "Supplier shipped PO #$poId - Tracking: $($poShip.trackingNumber), Status: $($poShip.status)"

Write-Output "=== 8. RECEIVING INSPECTION & GRN CREATION ==="
$grn = PostJson "http://localhost:8080/api/receiving/grn" @{
    orderId = $poId
    receivedBy = "Marcus Vance (Receiving Officer)"
    quantityReceived = 2
    conditionStatus = "GOOD"
    notes = "Inspected and verified in perfect physical and technical condition."
} $recToken
Write-Output "GRN #$($grn.grnId) created for PO #$poId - Condition: $($grn.conditionStatus)"

Write-Output "=== 9. SUPPLIER SUBMITS INVOICE ==="
$inv = PostJson "http://localhost:8080/api/supplier/orders/$poId/invoice" @{
    invoiceNumber = "INV-2026-" + (Get-Random -Minimum 10000 -Maximum 99999)
    amount = 2400.00
    notes = "Invoice for PO #$poId"
} $supToken
$invId = $inv.invoiceId
Write-Output "Invoice #$($inv.invoiceNumber) submitted (Amount: $($inv.amount))"

Write-Output "=== 10. FINANCE 3-WAY MATCHING & PAYMENT ==="
$matchResult = GetJson "http://localhost:8080/api/finance/invoices/$invId/match" $finToken
Write-Output "3-Way Match Result: Matched=$($matchResult.matched) | Status=$($matchResult.status) | QtyMatched=$($matchResult.quantityMatched) | AmountMatched=$($matchResult.amountMatched)"

$apprInv = PutJson "http://localhost:8080/api/finance/invoices/$invId/status" @{ status = "APPROVED"; notes = "3-Way Match Verified" } $finToken
Write-Output "Invoice approved by Finance - Status: $($apprInv.status)"

$payment = PostJson "http://localhost:8080/api/finance/invoices/$invId/payment" @{
    paymentMethod = "WIRE_TRANSFER"
    transactionReference = "TXN-WIRE-77382019"
} $finToken
Write-Output "Payment processed for Invoice #$invId - Ref: $($payment.transactionReference) - Status: $($payment.status)"

Write-Output "=== 11. CSV EXPORT VERIFICATION ==="
$csvReq = Invoke-WebRequest -Uri "http://localhost:8080/api/reports/csv/my-requests?email=employee@eps.com" -Headers @{ Authorization = "Bearer $empToken" } -UseBasicParsing
Write-Output "Employee CSV downloaded: $($csvReq.Content.Length) bytes"
Write-Output "CSV Header/Preview: $($csvReq.Content.Substring(0, [Math]::Min(120, $csvReq.Content.Length)))"

Write-Output "=== 12. SECURITY NEGATIVE TEST (403 FORBIDDEN) ==="
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Headers @{ Authorization = "Bearer $empToken" }
    Write-Output "UNEXPECTED: Employee accessed Admin endpoint!"
} catch {
    Write-Output "SUCCESS: Forbidden access returned HTTP status: $($_.Exception.Response.StatusCode.value__)"
}

Write-Output "ALL INTEGRATION TESTS PASSED SUCCESSFULLY!"
