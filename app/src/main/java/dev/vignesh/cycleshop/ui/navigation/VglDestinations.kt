package dev.vignesh.cycleshop.ui.navigation

/**
 * Defines all the destinations in the VGL Cycle Shop app.
 */
sealed class VglDestination(val route: String) {
    object Dashboard : VglDestination("dashboard")
    object InvoiceScan : VglDestination("invoice_scan")
    object BicycleScan : VglDestination("bicycle_scan")
    object SparePartScan : VglDestination("spare_part_scan")
    object Inventory : VglDestination("inventory")
    object Billing : VglDestination("billing")
    object Service : VglDestination("service")
}
