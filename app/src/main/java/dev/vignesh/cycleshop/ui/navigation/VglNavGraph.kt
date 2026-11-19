package dev.vignesh.cycleshop.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.vignesh.cycleshop.VglApplication
import dev.vignesh.cycleshop.ui.dashboard.DashboardScreen
import dev.vignesh.cycleshop.ui.dashboard.DashboardViewModel
import dev.vignesh.cycleshop.ui.dashboard.DashboardViewModelFactory
import dev.vignesh.cycleshop.ui.inventory.InventoryScreen
import dev.vignesh.cycleshop.ui.inventory.InventoryViewModel
import dev.vignesh.cycleshop.ui.inventory.InventoryViewModelFactory
import dev.vignesh.cycleshop.ui.invoice.InvoiceScanScreen
import dev.vignesh.cycleshop.ui.placeholders.PlaceholderScreen

@Composable
fun VglNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = context.applicationContext as VglApplication
    val repository = app.repository
    
    NavHost(
        navController = navController,
        startDestination = VglDestination.Dashboard.route
    ) {
        composable(VglDestination.Dashboard.route) {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModelFactory(repository)
            )
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
        
        // Phase 3, Step 1: Real invoice scan screen (CameraX + ML Kit OCR)
        composable(VglDestination.InvoiceScan.route) {
            InvoiceScanScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(VglDestination.BicycleScan.route) {
            PlaceholderScreen(
                title = "Scan Bicycle",
                phaseDescription = "Phase 4: Gemini Multimodal Vision AI for detecting bicycle branding, model, category, and specifications.",
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(VglDestination.SparePartScan.route) {
            PlaceholderScreen(
                title = "Scan Spare Part",
                phaseDescription = "Phase 4: Vision AI for spare part category classification and inventory stock verification.",
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(VglDestination.Inventory.route) {
            val inventoryViewModel: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(repository)
            )
            InventoryScreen(
                viewModel = inventoryViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(VglDestination.Billing.route) {
            PlaceholderScreen(
                title = "Billing & POS",
                phaseDescription = "Phase 5: Product selection, GST calculation, sales receipt preview, and stock reduction.",
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(VglDestination.Service.route) {
            PlaceholderScreen(
                title = "Service Management",
                phaseDescription = "Phase 5: Customer bicycle service intake, repair job cards, labour charges, and parts tracking.",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
