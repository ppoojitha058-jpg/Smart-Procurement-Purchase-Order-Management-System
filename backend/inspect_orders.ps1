$sup1 = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"supplier@eps.com","password":"Supplier@123"}'
$t1 = $sup1.data.token
$o1 = Invoke-RestMethod -Uri "http://localhost:8080/api/supplier/orders" -Method Get -Headers @{ Authorization = "Bearer $t1" }
Write-Output "=== SUPPLIER 1 ORDER STATUSES ==="
foreach ($o in $o1) {
    Write-Output "OrderID: $($o.orderId), Status: '$($o.status)'"
}
