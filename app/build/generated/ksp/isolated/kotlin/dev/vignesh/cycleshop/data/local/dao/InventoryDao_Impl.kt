package dev.vignesh.cycleshop.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import dev.vignesh.cycleshop.`data`.local.Converters
import dev.vignesh.cycleshop.`data`.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.`data`.local.entity.StockMovementEntity
import dev.vignesh.cycleshop.`data`.local.model.MovementType
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class InventoryDao_Impl(
  __db: RoomDatabase,
) : InventoryDao() {
  private val __db: RoomDatabase

  private val __insertAdapterOfStockMovementEntity: EntityInsertAdapter<StockMovementEntity>

  private val __converters: Converters = Converters()
  init {
    this.__db = __db
    this.__insertAdapterOfStockMovementEntity = object : EntityInsertAdapter<StockMovementEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `stock_movements` (`id`,`variant_id`,`delta`,`movement_type`,`unit_price_paise`,`reference_id`,`notes`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StockMovementEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.variantId)
        statement.bindLong(3, entity.delta.toLong())
        val _tmp: String? = __converters.fromMovementType(entity.movementType)
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmp)
        }
        val _tmpUnitPricePaise: Long? = entity.unitPricePaise
        if (_tmpUnitPricePaise == null) {
          statement.bindNull(5)
        } else {
          statement.bindLong(5, _tmpUnitPricePaise)
        }
        val _tmpReferenceId: String? = entity.referenceId
        if (_tmpReferenceId == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpReferenceId)
        }
        val _tmpNotes: String? = entity.notes
        if (_tmpNotes == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpNotes)
        }
        statement.bindLong(8, entity.createdAt)
      }
    }
  }

  public override suspend fun insertMovement(movement: StockMovementEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfStockMovementEntity.insertAndReturnId(_connection, movement)
    _result
  }

  public override suspend fun recordStockMovement(
    variantId: Long,
    delta: Int,
    movementType: MovementType,
    unitPricePaise: Long?,
    referenceId: String?,
    notes: String?,
    timestamp: Long,
  ): Long = performInTransactionSuspending(__db) {
    super@InventoryDao_Impl.recordStockMovement(variantId, delta, movementType, unitPricePaise, referenceId, notes, timestamp)
  }

  public override suspend fun getVariantById(variantId: Long): ProductVariantEntity? {
    val _sql: String = "SELECT * FROM product_variants WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, variantId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfProductId: Int = getColumnIndexOrThrow(_stmt, "product_id")
        val _columnIndexOfSku: Int = getColumnIndexOrThrow(_stmt, "sku")
        val _columnIndexOfSupplierCode: Int = getColumnIndexOrThrow(_stmt, "supplier_code")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfMrpPaise: Int = getColumnIndexOrThrow(_stmt, "mrp_paise")
        val _columnIndexOfCostPricePaise: Int = getColumnIndexOrThrow(_stmt, "cost_price_paise")
        val _columnIndexOfSellingPricePaise: Int = getColumnIndexOrThrow(_stmt, "selling_price_paise")
        val _columnIndexOfStockQuantity: Int = getColumnIndexOrThrow(_stmt, "stock_quantity")
        val _columnIndexOfSpecsJson: Int = getColumnIndexOrThrow(_stmt, "specs_json")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: ProductVariantEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpProductId: Long
          _tmpProductId = _stmt.getLong(_columnIndexOfProductId)
          val _tmpSku: String
          _tmpSku = _stmt.getText(_columnIndexOfSku)
          val _tmpSupplierCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierCode)) {
            _tmpSupplierCode = null
          } else {
            _tmpSupplierCode = _stmt.getText(_columnIndexOfSupplierCode)
          }
          val _tmpSize: String?
          if (_stmt.isNull(_columnIndexOfSize)) {
            _tmpSize = null
          } else {
            _tmpSize = _stmt.getText(_columnIndexOfSize)
          }
          val _tmpColor: String?
          if (_stmt.isNull(_columnIndexOfColor)) {
            _tmpColor = null
          } else {
            _tmpColor = _stmt.getText(_columnIndexOfColor)
          }
          val _tmpMrpPaise: Long
          _tmpMrpPaise = _stmt.getLong(_columnIndexOfMrpPaise)
          val _tmpCostPricePaise: Long
          _tmpCostPricePaise = _stmt.getLong(_columnIndexOfCostPricePaise)
          val _tmpSellingPricePaise: Long
          _tmpSellingPricePaise = _stmt.getLong(_columnIndexOfSellingPricePaise)
          val _tmpStockQuantity: Int
          _tmpStockQuantity = _stmt.getLong(_columnIndexOfStockQuantity).toInt()
          val _tmpSpecsJson: String?
          if (_stmt.isNull(_columnIndexOfSpecsJson)) {
            _tmpSpecsJson = null
          } else {
            _tmpSpecsJson = _stmt.getText(_columnIndexOfSpecsJson)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = ProductVariantEntity(_tmpId,_tmpProductId,_tmpSku,_tmpSupplierCode,_tmpSize,_tmpColor,_tmpMrpPaise,_tmpCostPricePaise,_tmpSellingPricePaise,_tmpStockQuantity,_tmpSpecsJson,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getStockQuantity(variantId: Long): Int? {
    val _sql: String = "SELECT stock_quantity FROM product_variants WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, variantId)
        val _result: Int?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getLong(0).toInt()
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getMovementsForVariant(variantId: Long): Flow<List<StockMovementEntity>> {
    val _sql: String = "SELECT * FROM stock_movements WHERE variant_id = ? ORDER BY created_at DESC"
    return createFlow(__db, false, arrayOf("stock_movements")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, variantId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfVariantId: Int = getColumnIndexOrThrow(_stmt, "variant_id")
        val _columnIndexOfDelta: Int = getColumnIndexOrThrow(_stmt, "delta")
        val _columnIndexOfMovementType: Int = getColumnIndexOrThrow(_stmt, "movement_type")
        val _columnIndexOfUnitPricePaise: Int = getColumnIndexOrThrow(_stmt, "unit_price_paise")
        val _columnIndexOfReferenceId: Int = getColumnIndexOrThrow(_stmt, "reference_id")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<StockMovementEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: StockMovementEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpVariantId: Long
          _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          val _tmpDelta: Int
          _tmpDelta = _stmt.getLong(_columnIndexOfDelta).toInt()
          val _tmpMovementType: MovementType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfMovementType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfMovementType)
          }
          val _tmp_1: MovementType? = __converters.toMovementType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.MovementType', but it was NULL.")
          } else {
            _tmpMovementType = _tmp_1
          }
          val _tmpUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfUnitPricePaise)) {
            _tmpUnitPricePaise = null
          } else {
            _tmpUnitPricePaise = _stmt.getLong(_columnIndexOfUnitPricePaise)
          }
          val _tmpReferenceId: String?
          if (_stmt.isNull(_columnIndexOfReferenceId)) {
            _tmpReferenceId = null
          } else {
            _tmpReferenceId = _stmt.getText(_columnIndexOfReferenceId)
          }
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = StockMovementEntity(_tmpId,_tmpVariantId,_tmpDelta,_tmpMovementType,_tmpUnitPricePaise,_tmpReferenceId,_tmpNotes,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getRecentMovements(limit: Int): Flow<List<StockMovementEntity>> {
    val _sql: String = "SELECT * FROM stock_movements ORDER BY created_at DESC LIMIT ?"
    return createFlow(__db, false, arrayOf("stock_movements")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfVariantId: Int = getColumnIndexOrThrow(_stmt, "variant_id")
        val _columnIndexOfDelta: Int = getColumnIndexOrThrow(_stmt, "delta")
        val _columnIndexOfMovementType: Int = getColumnIndexOrThrow(_stmt, "movement_type")
        val _columnIndexOfUnitPricePaise: Int = getColumnIndexOrThrow(_stmt, "unit_price_paise")
        val _columnIndexOfReferenceId: Int = getColumnIndexOrThrow(_stmt, "reference_id")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<StockMovementEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: StockMovementEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpVariantId: Long
          _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          val _tmpDelta: Int
          _tmpDelta = _stmt.getLong(_columnIndexOfDelta).toInt()
          val _tmpMovementType: MovementType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfMovementType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfMovementType)
          }
          val _tmp_1: MovementType? = __converters.toMovementType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.MovementType', but it was NULL.")
          } else {
            _tmpMovementType = _tmp_1
          }
          val _tmpUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfUnitPricePaise)) {
            _tmpUnitPricePaise = null
          } else {
            _tmpUnitPricePaise = _stmt.getLong(_columnIndexOfUnitPricePaise)
          }
          val _tmpReferenceId: String?
          if (_stmt.isNull(_columnIndexOfReferenceId)) {
            _tmpReferenceId = null
          } else {
            _tmpReferenceId = _stmt.getText(_columnIndexOfReferenceId)
          }
          val _tmpNotes: String?
          if (_stmt.isNull(_columnIndexOfNotes)) {
            _tmpNotes = null
          } else {
            _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = StockMovementEntity(_tmpId,_tmpVariantId,_tmpDelta,_tmpMovementType,_tmpUnitPricePaise,_tmpReferenceId,_tmpNotes,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalStockCount(): Flow<Int?> {
    val _sql: String = "SELECT SUM(stock_quantity) FROM product_variants"
    return createFlow(__db, false, arrayOf("product_variants")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int?
        if (_stmt.step()) {
          val _tmp: Int?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0).toInt()
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getLowStockVariants(threshold: Int): Flow<List<ProductVariantEntity>> {
    val _sql: String = "SELECT * FROM product_variants WHERE stock_quantity <= ? ORDER BY stock_quantity ASC"
    return createFlow(__db, false, arrayOf("product_variants")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, threshold.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfProductId: Int = getColumnIndexOrThrow(_stmt, "product_id")
        val _columnIndexOfSku: Int = getColumnIndexOrThrow(_stmt, "sku")
        val _columnIndexOfSupplierCode: Int = getColumnIndexOrThrow(_stmt, "supplier_code")
        val _columnIndexOfSize: Int = getColumnIndexOrThrow(_stmt, "size")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfMrpPaise: Int = getColumnIndexOrThrow(_stmt, "mrp_paise")
        val _columnIndexOfCostPricePaise: Int = getColumnIndexOrThrow(_stmt, "cost_price_paise")
        val _columnIndexOfSellingPricePaise: Int = getColumnIndexOrThrow(_stmt, "selling_price_paise")
        val _columnIndexOfStockQuantity: Int = getColumnIndexOrThrow(_stmt, "stock_quantity")
        val _columnIndexOfSpecsJson: Int = getColumnIndexOrThrow(_stmt, "specs_json")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<ProductVariantEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProductVariantEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpProductId: Long
          _tmpProductId = _stmt.getLong(_columnIndexOfProductId)
          val _tmpSku: String
          _tmpSku = _stmt.getText(_columnIndexOfSku)
          val _tmpSupplierCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierCode)) {
            _tmpSupplierCode = null
          } else {
            _tmpSupplierCode = _stmt.getText(_columnIndexOfSupplierCode)
          }
          val _tmpSize: String?
          if (_stmt.isNull(_columnIndexOfSize)) {
            _tmpSize = null
          } else {
            _tmpSize = _stmt.getText(_columnIndexOfSize)
          }
          val _tmpColor: String?
          if (_stmt.isNull(_columnIndexOfColor)) {
            _tmpColor = null
          } else {
            _tmpColor = _stmt.getText(_columnIndexOfColor)
          }
          val _tmpMrpPaise: Long
          _tmpMrpPaise = _stmt.getLong(_columnIndexOfMrpPaise)
          val _tmpCostPricePaise: Long
          _tmpCostPricePaise = _stmt.getLong(_columnIndexOfCostPricePaise)
          val _tmpSellingPricePaise: Long
          _tmpSellingPricePaise = _stmt.getLong(_columnIndexOfSellingPricePaise)
          val _tmpStockQuantity: Int
          _tmpStockQuantity = _stmt.getLong(_columnIndexOfStockQuantity).toInt()
          val _tmpSpecsJson: String?
          if (_stmt.isNull(_columnIndexOfSpecsJson)) {
            _tmpSpecsJson = null
          } else {
            _tmpSpecsJson = _stmt.getText(_columnIndexOfSpecsJson)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = ProductVariantEntity(_tmpId,_tmpProductId,_tmpSku,_tmpSupplierCode,_tmpSize,_tmpColor,_tmpMrpPaise,_tmpCostPricePaise,_tmpSellingPricePaise,_tmpStockQuantity,_tmpSpecsJson,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateStockQuantityAtomic(variantId: Long, delta: Int): Int {
    val _sql: String = """
        |
        |        UPDATE product_variants 
        |        SET stock_quantity = stock_quantity + ? 
        |        WHERE id = ? AND (stock_quantity + ?) >= 0
        |    
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, delta.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, variantId)
        _argIndex = 3
        _stmt.bindLong(_argIndex, delta.toLong())
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
