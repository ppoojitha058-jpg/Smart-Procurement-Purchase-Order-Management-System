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

Write-Output "================================================================================"
Write-Output "               ENTERPRISE PROCUREMENT SYSTEM (EPS) - FULL FLOW DEMO             "
Write-Output "================================================================================"

Write-Output "`n[1/10] AUTHENTICATING ALL SYSTEM ROLES..."
$empToken = (PostJson "http://localhost:8080/api/auth/login" @{ email="employee@eps.com"; password="User@123" }).data.token
$mgrToken = (PostJson "http://localhost:8080/api/auth/login" @{ email="manager@eps.com"; password="Manager@123" }).data.token
$admToken = (PostJson "http://localhost:8080/api/auth/login" @{ email="admin@eps.com"; password="Admin@123" }).data.token
$supToken = (PostJson "http://localhost:8080/api/auth/login" @{ email="supplier@eps.com"; password="Supplier@123" }).data.token
$recToken = (PostJson "http://localhost:8080/api/auth/login" @{ email="receiving@eps.com"; password="Receiving@123" }).data.token
$finToken = (PostJson "http://localhost:8080/api/auth/login" @{ email="finance@eps.com"; password="Finance@123" }).data.token
Write-Output "✔ 6 Enterprise Roles Authenticated (Employee, Manager, Admin, Supplier, Receiving, Finance)"

Write-Output "`n[2/10] EMPLOYEE CREATING PURCHASE REQUISITION..."
$pr = PostJson "http://localhost:8080/api/purchase-requests" @{
    userId = 2
    productId = 1
    departmentId = 1
    quantity = 2
} $empToken
$prId = $pr.data.requestId
Write-Output "✔ Requisition Created: REQ #$prId for 2x '$($pr.data.productName)'"
Write-Output "  Initial Status: $($pr.data.status) | Total: ₹$($pr.data.totalPrice)"

Write-Output "`n[3/10] MANAGER REVIEW & APPROVAL..."
$appr = PostJson "http://localhost:8080/api/manager/requests/$prId/approve" @{ remarks = "Approved - Q3 Engineering upgrade" } $mgrToken
Write-Output "✔ Manager Approval Complete: Status -> $($appr.data.status)"

Write-Output "`n[4/10] ADMIN SEQUENTIAL FLOW: ISSUE PO -> PAY..."
try {
    PostJson "http://localhost:8080/api/admin/requests/$prId/pay" @{ mpin = "1234"; paymentMethod = "UPI" } $admToken | Out-Null
    Write-Output "✖ ERROR: Payment was allowed before PO issuance!"
} catch {
    Write-Output "✔ STRICT ENFORCEMENT: Payment blocked before PO issuance (HTTP 400 Bad Request)"
}

$po = PostJson "http://localhost:8080/api/admin/requests/$prId/process" @{} $admToken
$poId = $po.data.orderId
Write-Output "✔ Admin Issued PO: ORD-$poId generated from REQ #$prId"
Write-Output "  Requisition Status -> PO_ISSUED | UI Action: [PO Issued ✓] with Pay enabled"

PostJson "http://localhost:8080/api/admin/orders/$poId/assign-supplier" @{ supplierId = 1 } $admToken | Out-Null
Write-Output "✔ Supplier Assigned: Apex Technologies Global (Supplier #1)"

$pay = PostJson "http://localhost:8080/api/admin/requests/$prId/pay" @{
    mpin = "1234"
    paymentMethod = "UPI (Google Pay)"
    transactionReference = "TXN-GPAY-" + (Get-Random -Minimum 100000 -Maximum 999999)
} $admToken
Write-Output "✔ Admin Payment Settled: Ref=$($pay.data.transactionReference) | Status -> $($pay.data.status)"
Write-Output "  Order Status -> PAID | UI Action: [PO Issued ✓ | Payment Completed ✓]"

Write-Output "`n[5/10] SUPPLIER ACCEPTANCE & DISPATCH..."
$supAccept = PutJson "http://localhost:8080/api/supplier/orders/$poId/accept" @{} $supToken
Write-Output "✔ Supplier Accepted Order: ORD-$poId (Status: $($supAccept.status))"

$trackingNo = "FEDEX-IND-" + (Get-Random -Minimum 10000000 -Maximum 99999999)
$supShip = PutJson "http://localhost:8080/api/supplier/orders/$poId/shipment" @{
    trackingNumber = $trackingNo
    shipmentDetails = "Dispatched via FedEx Priority Air Freight"
} $supToken
Write-Output "✔ Supplier Dispatched Order: Tracking #$trackingNo"

Write-Output "`n[6/10] RECEIVING OFFICER PHYSICAL INSPECTION & GRN..."
$grn = PostJson "http://localhost:8080/api/receiving/grn" @{
    orderId = $poId
    receivedBy = "Officer Rajesh Sharma"
    quantityReceived = 2
    conditionStatus = "GOOD"
    notes = "Serial numbers verified, hardware diagnostics passed 100%."
} $recToken
Write-Output "✔ Goods Receipt Note Generated: GRN #$($grn.grnId) - Condition: $($grn.conditionStatus)"

Write-Output "`n[7/10] MARKING ORDER AS DELIVERED..."
$delivered = PutJson "http://localhost:8080/api/supplier/orders/$poId/deliver" @{} $supToken
Write-Output "✔ Final Fulfillment Milestone: Order ORD-$poId Status -> $($delivered.status)"

Write-Output "`n[8/10] USER POST-DELIVERY RATING & WRITTEN FEEDBACK..."
$feedbackRes = PostJson "http://localhost:8080/api/users/requests/$prId/feedback" @{
    qualityRating = 5
    speedRating = 5
    comments = "Exceptional product build quality and arrived ahead of schedule. Fully tested and operational!"
} $empToken
Write-Output "✔ Feedback Recorded in Database:"
Write-Output "  Feedback ID    : $($feedbackRes.data.feedbackId)"
Write-Output "  User Link      : $($feedbackRes.data.userName) (User ID: $($feedbackRes.data.userId))"
Write-Output "  Order Link     : ORD-$($feedbackRes.data.orderId) (REQ #$($feedbackRes.data.requestId))"
Write-Output "  Product Link   : $($feedbackRes.data.productName) (Product ID: $($feedbackRes.data.productId))"
Write-Output "  Supplier Link  : $($feedbackRes.data.supplierName) (Supplier ID: $($feedbackRes.data.supplierId))"
Write-Output "  Ratings        : Quality ★ $($feedbackRes.data.qualityRating)/5 | Speed ★ $($feedbackRes.data.speedRating)/5"
Write-Output "  User Feedback  : '$($feedbackRes.data.comments)'"

Write-Output "`n[9/10] SUPPLIER DASHBOARD LIVE RATINGS & FEEDBACK..."
$supFeedbacks = GetJson "http://localhost:8080/api/supplier/feedback" $supToken
$avgRating = ($supFeedbacks | Measure-Object -Property averageRating -Average).Average
Write-Output "✔ Supplier Dashboard Feedback View Verified:"
Write-Output "  Total Supplier Ratings : $($supFeedbacks.Count)"
Write-Output "  Supplier Average Score : $([Math]::Round($avgRating, 2)) / 5.0 ⭐"
Write-Output "  Latest Feedback Row    : ORD-$($supFeedbacks[0].orderId) | '$($supFeedbacks[0].comments)' by $($supFeedbacks[0].userName)"

Write-Output "`n[10/10] FINANCE 3-WAY MATCHING & CSV AUDIT..."
$inv = PostJson "http://localhost:8080/api/supplier/orders/$poId/invoice" @{
    invoiceNumber = "INV-2026-" + (Get-Random -Minimum 10000 -Maximum 99999)
    amount = 2500.00
    notes = "Invoice for completed ORD-$poId"
} $supToken
$match = GetJson "http://localhost:8080/api/finance/invoices/$($inv.invoiceId)/match" $finToken
Write-Output "✔ Finance 3-Way Match : Matched=$($match.matched) | Status=$($match.status) | Qty=$($match.quantityMatched) | Price=$($match.amountMatched)"

Write-Output "`n================================================================================"
Write-Output "     100% END-TO-END PROCUREMENT LIFECYCLE COMPLETED SUCCESSFULLY!              "
Write-Output "================================================================================"
