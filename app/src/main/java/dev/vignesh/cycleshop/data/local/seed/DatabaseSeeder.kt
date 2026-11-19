package dev.vignesh.cycleshop.data.local.seed

import androidx.room.withTransaction
import dev.vignesh.cycleshop.data.local.VglDatabase
import dev.vignesh.cycleshop.data.local.entity.BrandEntity
import dev.vignesh.cycleshop.data.local.entity.ProductEntity
import dev.vignesh.cycleshop.data.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.data.local.model.MovementType
import dev.vignesh.cycleshop.data.local.model.ProductSpecs
import dev.vignesh.cycleshop.data.local.model.ProductType
import kotlinx.coroutines.flow.first

/**
 * Seeds realistic local demo catalog and opening inventory movements for Pollachi showroom demo.
 *
 * Guaranteed invariants:
 * 1. Executes ONCE on fresh database initialization; does not overwrite existing client data.
 * 2. Every initial stock quantity is recorded via [InventoryDao.recordStockMovement] with [MovementType.OPENING].
 * 3. Exact 1:1 ledger reconciliation from inception.
 */
object DatabaseSeeder {

    suspend fun seedIfEmpty(database: VglDatabase) {
        val productDao = database.productDao()
        val inventoryDao = database.inventoryDao()

        // Check if database already has brands/products
        val existingBrands = productDao.getAllBrands().first()
        if (existingBrands.isNotEmpty()) {
            return // Database already seeded; preserve all user changes!
        }

        database.withTransaction {
            // --- 1. Brands ---
            val heroId = productDao.insertBrand(BrandEntity(name = "Hero"))
            val firefoxId = productDao.insertBrand(BrandEntity(name = "Firefox"))
            val montraId = productDao.insertBrand(BrandEntity(name = "Montra"))
            val herculesId = productDao.insertBrand(BrandEntity(name = "Hercules"))
            val bsaId = productDao.insertBrand(BrandEntity(name = "BSA"))
            val shimanoId = productDao.insertBrand(BrandEntity(name = "Shimano"))

            // --- 2. Bicycles ---

            // Hero Sprint Pro 27.5T
            val heroBikeId = productDao.insertProduct(
                ProductEntity(
                    brandId = heroId,
                    name = "Hero Sprint Pro 27.5T",
                    category = "MTB",
                    type = ProductType.BICYCLE,
                    isActive = true
                )
            )
            val heroSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "frame" to "Alloy 6061 (17 inch)",
                    "gears" to "21-Speed Shimano Tourney",
                    "brakes" to "Dual Mechanical Disc",
                    "wheel" to "27.5 x 2.10 Nylon Tires",
                    "suspension" to "80mm Travel Front Fork"
                )
            )
            val vHero1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = heroBikeId,
                    sku = "VGL-HERO-SP275-BLK",
                    supplierCode = "SUP-HERO-101",
                    size = "17 Frame / 27.5T",
                    color = "Matte Black / Neon",
                    mrpPaise = 1650000L,
                    costPricePaise = 1200000L,
                    sellingPricePaise = 1499900L,
                    stockQuantity = 0,
                    specsJson = heroSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vHero1,
                delta = 5,
                movementType = MovementType.OPENING,
                unitPricePaise = 1200000L,
                referenceId = "OPENING-2026",
                notes = "Showroom display opening stock"
            )

            val vHero2 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = heroBikeId,
                    sku = "VGL-HERO-SP275-BLU",
                    supplierCode = "SUP-HERO-102",
                    size = "18 Frame / 27.5T",
                    color = "Cobalt Blue",
                    mrpPaise = 1650000L,
                    costPricePaise = 1200000L,
                    sellingPricePaise = 1499900L,
                    stockQuantity = 0,
                    specsJson = heroSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vHero2,
                delta = 3,
                movementType = MovementType.OPENING,
                unitPricePaise = 1200000L,
                referenceId = "OPENING-2026",
                notes = "Showroom boxed opening stock"
            )

            // Firefox Target 29
            val firefoxBikeId = productDao.insertProduct(
                ProductEntity(
                    brandId = firefoxId,
                    name = "Firefox Target 29",
                    category = "MTB",
                    type = ProductType.BICYCLE,
                    isActive = true
                )
            )
            val ffSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "frame" to "Hydroformed Alloy (18.5 inch)",
                    "gears" to "21-Speed Shimano Altus",
                    "brakes" to "Tektro Mechanical Disc",
                    "wheel" to "29 x 2.25 Wanda Tires",
                    "suspension" to "Zoom 100mm with Hydraulic Lockout"
                )
            )
            val vFf1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = firefoxBikeId,
                    sku = "VGL-FF-TGT29-ORG",
                    supplierCode = "SUP-FFX-201",
                    size = "18.5 Frame / 29T",
                    color = "Blaze Orange / Charcoal",
                    mrpPaise = 2250000L,
                    costPricePaise = 1700000L,
                    sellingPricePaise = 2049900L,
                    stockQuantity = 0,
                    specsJson = ffSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vFf1,
                delta = 4,
                movementType = MovementType.OPENING,
                unitPricePaise = 1700000L,
                referenceId = "OPENING-2026",
                notes = "Showroom display opening stock"
            )

            // Montra Helicon Disc
            val montraBikeId = productDao.insertProduct(
                ProductEntity(
                    brandId = montraId,
                    name = "Montra Helicon Disc",
                    category = "Hybrid",
                    type = ProductType.BICYCLE,
                    isActive = true
                )
            )
            val montraSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "frame" to "6061 Aluminum Alloy",
                    "gears" to "21-Speed Shimano Tourney TX",
                    "brakes" to "Promax Mechanical Disc",
                    "wheel" to "700 x 35C Fast Rolling",
                    "fork" to "Rigid Steel Aero Fork"
                )
            )
            val vMontra1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = montraBikeId,
                    sku = "VGL-MON-HELI-M",
                    supplierCode = "SUP-TI-301",
                    size = "Medium (19 inch)",
                    color = "Gunmetal Grey",
                    mrpPaise = 2400000L,
                    costPricePaise = 1850000L,
                    sellingPricePaise = 2199900L,
                    stockQuantity = 0,
                    specsJson = montraSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vMontra1,
                delta = 6,
                movementType = MovementType.OPENING,
                unitPricePaise = 1850000L,
                referenceId = "OPENING-2026",
                notes = "Showroom display opening stock"
            )

            // Hercules Roadeo Warpath
            val hercBikeId = productDao.insertProduct(
                ProductEntity(
                    brandId = herculesId,
                    name = "Hercules Roadeo Warpath",
                    category = "Youth",
                    type = ProductType.BICYCLE,
                    isActive = true
                )
            )
            val hercSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "frame" to "High-Tensile Steel",
                    "gears" to "21-Speed Ezy Fire Shifters",
                    "brakes" to "Front Disc / Rear V-Brake",
                    "wheel" to "26 x 2.10 MTB Tread"
                )
            )
            val vHerc1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = hercBikeId,
                    sku = "VGL-HERC-ROADEO-YEL",
                    supplierCode = "SUP-TI-401",
                    size = "26T",
                    color = "Neon Yellow / Black",
                    mrpPaise = 1350000L,
                    costPricePaise = 1000000L,
                    sellingPricePaise = 1220000L,
                    stockQuantity = 0,
                    specsJson = hercSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vHerc1,
                delta = 4,
                movementType = MovementType.OPENING,
                unitPricePaise = 1000000L,
                referenceId = "OPENING-2026",
                notes = "Showroom display opening stock"
            )

            // BSA Ladybird Shine 26T
            val bsaBikeId = productDao.insertProduct(
                ProductEntity(
                    brandId = bsaId,
                    name = "BSA Ladybird Shine 26T",
                    category = "City",
                    type = ProductType.BICYCLE,
                    isActive = true
                )
            )
            val bsaSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "frame" to "Low-Step Through Steel Frame",
                    "gears" to "Single Speed",
                    "brakes" to "Caliper Brakes with Soft-Grip Levers",
                    "wheel" to "26 x 1.375 City Profile",
                    "accessories" to "Integrated Dress Guard, Wire Basket & Carrier"
                )
            )
            val vBsa1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = bsaBikeId,
                    sku = "VGL-BSA-LADYBIRD-PNK",
                    supplierCode = "SUP-TI-501",
                    size = "26T Low-Step",
                    color = "Candy Pink",
                    mrpPaise = 890000L,
                    costPricePaise = 650000L,
                    sellingPricePaise = 799900L,
                    stockQuantity = 0,
                    specsJson = bsaSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vBsa1,
                delta = 5,
                movementType = MovementType.OPENING,
                unitPricePaise = 650000L,
                referenceId = "OPENING-2026",
                notes = "Showroom display opening stock"
            )

            // --- 3. Spare Parts ---

            // Shimano Tourney TX Rear Derailleur
            val shimanoRdId = productDao.insertProduct(
                ProductEntity(
                    brandId = shimanoId,
                    name = "Shimano Tourney TX Rear Derailleur (RD-TX800)",
                    category = "Gearing",
                    type = ProductType.SPARE_PART,
                    isActive = true
                )
            )
            val rdSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "compatibility" to "7/8 Speed Shimano SIS",
                    "mount_type" to "Direct Mount (Hanger)",
                    "pulley" to "13T/13T Smart Cage"
                )
            )
            val vRd1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = shimanoRdId,
                    sku = "VGL-SHIM-RDTX800",
                    supplierCode = "SUP-SHI-800",
                    size = "7/8 Speed SGS Long Cage",
                    color = "Black",
                    mrpPaise = 125000L,
                    costPricePaise = 85000L,
                    sellingPricePaise = 110000L,
                    stockQuantity = 0,
                    specsJson = rdSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vRd1,
                delta = 8,
                movementType = MovementType.OPENING,
                unitPricePaise = 85000L,
                referenceId = "OPENING-2026",
                notes = "Workshop opening spare parts"
            )

            // Shimano V-Brake Pads
            val shimanoPadsId = productDao.insertProduct(
                ProductEntity(
                    brandId = shimanoId,
                    name = "Shimano V-Brake Shoe & Pads (Pair)",
                    category = "Braking",
                    type = ProductType.SPARE_PART,
                    isActive = true
                )
            )
            val padSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "length" to "70mm Contoured",
                    "compatibility" to "All Linear Pull V-Brake Calipers",
                    "compound" to "All-Weather Rubber"
                )
            )
            val vPads1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = shimanoPadsId,
                    sku = "VGL-SHIM-VB-PADS",
                    supplierCode = "SUP-SHI-VB1",
                    size = "Standard 70mm",
                    color = "Black",
                    mrpPaise = 45000L,
                    costPricePaise = 25000L,
                    sellingPricePaise = 38000L,
                    stockQuantity = 0,
                    specsJson = padSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vPads1,
                delta = 20,
                movementType = MovementType.OPENING,
                unitPricePaise = 25000L,
                referenceId = "OPENING-2026",
                notes = "Workshop opening spare parts"
            )

            // KMC Z7 Drivetrain Chain
            val chainId = productDao.insertProduct(
                ProductEntity(
                    brandId = shimanoId,
                    name = "KMC Z7 7-Speed Bicycle Chain",
                    category = "Drivetrain",
                    type = ProductType.SPARE_PART,
                    isActive = true
                )
            )
            val chainSpecs = ProductSpecs(
                schemaVersion = 1,
                attributes = mapOf(
                    "links" to "116 Links with MissingLink",
                    "size" to "1/2 x 3/32 inch",
                    "compatibility" to "6/7/8 Speed Systems"
                )
            )
            val vChain1 = productDao.insertVariant(
                ProductVariantEntity(
                    productId = chainId,
                    sku = "VGL-KMC-Z7-116",
                    supplierCode = "SUP-KMC-07",
                    size = "116 Links",
                    color = "Grey / Brown",
                    mrpPaise = 65000L,
                    costPricePaise = 40000L,
                    sellingPricePaise = 55000L,
                    stockQuantity = 0,
                    specsJson = chainSpecs.toJson()
                )
            )
            inventoryDao.recordStockMovement(
                variantId = vChain1,
                delta = 12,
                movementType = MovementType.OPENING,
                unitPricePaise = 40000L,
                referenceId = "OPENING-2026",
                notes = "Workshop opening spare parts"
            )
        }
    }
}
