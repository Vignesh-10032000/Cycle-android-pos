package dev.vignesh.cycleshop.`data`.local.dao

import androidx.collection.LongSparseArray
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import dev.vignesh.cycleshop.`data`.local.Converters
import dev.vignesh.cycleshop.`data`.local.entity.PurchaseInvoiceEntity
import dev.vignesh.cycleshop.`data`.local.entity.PurchaseLineEntity
import dev.vignesh.cycleshop.`data`.local.entity.SupplierProductMappingEntity
import dev.vignesh.cycleshop.`data`.local.model.InventoryUnit
import dev.vignesh.cycleshop.`data`.local.model.LineReviewStatus
import dev.vignesh.cycleshop.`data`.local.model.PurchaseInvoiceStatus
import dev.vignesh.cycleshop.`data`.local.model.PurchaseInvoiceWithLines
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PurchaseDao_Impl(
  __db: RoomDatabase,
) : PurchaseDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPurchaseInvoiceEntity: EntityInsertAdapter<PurchaseInvoiceEntity>

  private val __converters: Converters = Converters()

  private val __insertAdapterOfPurchaseLineEntity: EntityInsertAdapter<PurchaseLineEntity>

  private val __insertAdapterOfSupplierProductMappingEntity:
      EntityInsertAdapter<SupplierProductMappingEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfPurchaseInvoiceEntity = object : EntityInsertAdapter<PurchaseInvoiceEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `purchase_invoices` (`id`,`supplier_name`,`supplier_gstin`,`invoice_number`,`invoice_date`,`consignee_name`,`consignee_gstin`,`total_amount_paise`,`total_quantity`,`image_uri`,`raw_ocr_text`,`status`,`supporting_doc_ref`,`created_at`,`updated_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PurchaseInvoiceEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.supplierName)
        val _tmpSupplierGstin: String? = entity.supplierGstin
        if (_tmpSupplierGstin == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpSupplierGstin)
        }
        statement.bindText(4, entity.invoiceNumber)
        val _tmpInvoiceDate: String? = entity.invoiceDate
        if (_tmpInvoiceDate == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpInvoiceDate)
        }
        val _tmpConsigneeName: String? = entity.consigneeName
        if (_tmpConsigneeName == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpConsigneeName)
        }
        val _tmpConsigneeGstin: String? = entity.consigneeGstin
        if (_tmpConsigneeGstin == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpConsigneeGstin)
        }
        val _tmpTotalAmountPaise: Long? = entity.totalAmountPaise
        if (_tmpTotalAmountPaise == null) {
          statement.bindNull(8)
        } else {
          statement.bindLong(8, _tmpTotalAmountPaise)
        }
        val _tmpTotalQuantity: Double? = entity.totalQuantity
        if (_tmpTotalQuantity == null) {
          statement.bindNull(9)
        } else {
          statement.bindDouble(9, _tmpTotalQuantity)
        }
        val _tmpImageUri: String? = entity.imageUri
        if (_tmpImageUri == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpImageUri)
        }
        val _tmpRawOcrText: String? = entity.rawOcrText
        if (_tmpRawOcrText == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpRawOcrText)
        }
        val _tmp: String? = __converters.fromPurchaseInvoiceStatus(entity.status)
        if (_tmp == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmp)
        }
        val _tmpSupportingDocRef: String? = entity.supportingDocRef
        if (_tmpSupportingDocRef == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpSupportingDocRef)
        }
        statement.bindLong(14, entity.createdAt)
        statement.bindLong(15, entity.updatedAt)
      }
    }
    this.__insertAdapterOfPurchaseLineEntity = object : EntityInsertAdapter<PurchaseLineEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `purchase_lines` (`id`,`invoice_id`,`line_number`,`variant_id`,`supplier_item_code`,`raw_description`,`corrected_description`,`hsn_code`,`raw_quantity`,`corrected_quantity`,`raw_unit`,`inventory_unit`,`unit_conversion_factor`,`effective_quantity_base`,`raw_unit_price_paise`,`corrected_unit_price_paise`,`raw_line_amount_paise`,`corrected_line_amount_paise`,`gst_rate_percent`,`review_status`,`rejection_reason`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PurchaseLineEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.invoiceId)
        statement.bindLong(3, entity.lineNumber.toLong())
        val _tmpVariantId: Long? = entity.variantId
        if (_tmpVariantId == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpVariantId)
        }
        val _tmpSupplierItemCode: String? = entity.supplierItemCode
        if (_tmpSupplierItemCode == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpSupplierItemCode)
        }
        statement.bindText(6, entity.rawDescription)
        val _tmpCorrectedDescription: String? = entity.correctedDescription
        if (_tmpCorrectedDescription == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpCorrectedDescription)
        }
        val _tmpHsnCode: String? = entity.hsnCode
        if (_tmpHsnCode == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpHsnCode)
        }
        statement.bindDouble(9, entity.rawQuantity)
        val _tmpCorrectedQuantity: Double? = entity.correctedQuantity
        if (_tmpCorrectedQuantity == null) {
          statement.bindNull(10)
        } else {
          statement.bindDouble(10, _tmpCorrectedQuantity)
        }
        statement.bindText(11, entity.rawUnit)
        val _tmp: String? = __converters.fromInventoryUnit(entity.inventoryUnit)
        if (_tmp == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmp)
        }
        statement.bindLong(13, entity.unitConversionFactor.toLong())
        statement.bindLong(14, entity.effectiveQuantityBase.toLong())
        val _tmpRawUnitPricePaise: Long? = entity.rawUnitPricePaise
        if (_tmpRawUnitPricePaise == null) {
          statement.bindNull(15)
        } else {
          statement.bindLong(15, _tmpRawUnitPricePaise)
        }
        val _tmpCorrectedUnitPricePaise: Long? = entity.correctedUnitPricePaise
        if (_tmpCorrectedUnitPricePaise == null) {
          statement.bindNull(16)
        } else {
          statement.bindLong(16, _tmpCorrectedUnitPricePaise)
        }
        val _tmpRawLineAmountPaise: Long? = entity.rawLineAmountPaise
        if (_tmpRawLineAmountPaise == null) {
          statement.bindNull(17)
        } else {
          statement.bindLong(17, _tmpRawLineAmountPaise)
        }
        val _tmpCorrectedLineAmountPaise: Long? = entity.correctedLineAmountPaise
        if (_tmpCorrectedLineAmountPaise == null) {
          statement.bindNull(18)
        } else {
          statement.bindLong(18, _tmpCorrectedLineAmountPaise)
        }
        val _tmpGstRatePercent: Double? = entity.gstRatePercent
        if (_tmpGstRatePercent == null) {
          statement.bindNull(19)
        } else {
          statement.bindDouble(19, _tmpGstRatePercent)
        }
        val _tmp_1: String? = __converters.fromLineReviewStatus(entity.reviewStatus)
        if (_tmp_1 == null) {
          statement.bindNull(20)
        } else {
          statement.bindText(20, _tmp_1)
        }
        val _tmpRejectionReason: String? = entity.rejectionReason
        if (_tmpRejectionReason == null) {
          statement.bindNull(21)
        } else {
          statement.bindText(21, _tmpRejectionReason)
        }
        statement.bindLong(22, entity.createdAt)
      }
    }
    this.__insertAdapterOfSupplierProductMappingEntity = object : EntityInsertAdapter<SupplierProductMappingEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `supplier_product_mappings` (`id`,`supplier_identifier`,`supplier_item_code`,`supplier_desc_pattern`,`variant_id`,`verified_by_owner`,`created_at`,`updated_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SupplierProductMappingEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.supplierIdentifier)
        val _tmpSupplierItemCode: String? = entity.supplierItemCode
        if (_tmpSupplierItemCode == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpSupplierItemCode)
        }
        statement.bindText(4, entity.supplierDescPattern)
        statement.bindLong(5, entity.variantId)
        val _tmp: Int = if (entity.verifiedByOwner) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.updatedAt)
      }
    }
  }

  public override suspend fun insertInvoice(invoice: PurchaseInvoiceEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfPurchaseInvoiceEntity.insertAndReturnId(_connection, invoice)
    _result
  }

  public override suspend fun insertLines(lines: List<PurchaseLineEntity>): List<Long> = performSuspending(__db, false, true) { _connection ->
    val _result: List<Long> = __insertAdapterOfPurchaseLineEntity.insertAndReturnIdsList(_connection, lines)
    _result
  }

  public override suspend fun insertOrUpdateSupplierMapping(mapping: SupplierProductMappingEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfSupplierProductMappingEntity.insertAndReturnId(_connection, mapping)
    _result
  }

  public override suspend fun stageInvoiceWithLines(
    invoice: PurchaseInvoiceEntity,
    lines: List<PurchaseLineEntity>,
    verifiedMappings: List<SupplierProductMappingEntity>,
  ): Long = performInTransactionSuspending(__db) {
    super@PurchaseDao_Impl.stageInvoiceWithLines(invoice, lines, verifiedMappings)
  }

  public override suspend fun findDuplicateInvoice(
    invoiceNumber: String,
    supplierGstin: String?,
    supplierName: String,
  ): PurchaseInvoiceEntity? {
    val _sql: String = """
        |
        |        SELECT * FROM purchase_invoices 
        |        WHERE invoice_number = ? 
        |          AND (
        |            (? IS NOT NULL AND supplier_gstin = ?) 
        |            OR supplier_name = ?
        |          )
        |        LIMIT 1
        |    
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, invoiceNumber)
        _argIndex = 2
        if (supplierGstin == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, supplierGstin)
        }
        _argIndex = 3
        if (supplierGstin == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, supplierGstin)
        }
        _argIndex = 4
        _stmt.bindText(_argIndex, supplierName)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierName: Int = getColumnIndexOrThrow(_stmt, "supplier_name")
        val _columnIndexOfSupplierGstin: Int = getColumnIndexOrThrow(_stmt, "supplier_gstin")
        val _columnIndexOfInvoiceNumber: Int = getColumnIndexOrThrow(_stmt, "invoice_number")
        val _columnIndexOfInvoiceDate: Int = getColumnIndexOrThrow(_stmt, "invoice_date")
        val _columnIndexOfConsigneeName: Int = getColumnIndexOrThrow(_stmt, "consignee_name")
        val _columnIndexOfConsigneeGstin: Int = getColumnIndexOrThrow(_stmt, "consignee_gstin")
        val _columnIndexOfTotalAmountPaise: Int = getColumnIndexOrThrow(_stmt, "total_amount_paise")
        val _columnIndexOfTotalQuantity: Int = getColumnIndexOrThrow(_stmt, "total_quantity")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "image_uri")
        val _columnIndexOfRawOcrText: Int = getColumnIndexOrThrow(_stmt, "raw_ocr_text")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSupportingDocRef: Int = getColumnIndexOrThrow(_stmt, "supporting_doc_ref")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: PurchaseInvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierName: String
          _tmpSupplierName = _stmt.getText(_columnIndexOfSupplierName)
          val _tmpSupplierGstin: String?
          if (_stmt.isNull(_columnIndexOfSupplierGstin)) {
            _tmpSupplierGstin = null
          } else {
            _tmpSupplierGstin = _stmt.getText(_columnIndexOfSupplierGstin)
          }
          val _tmpInvoiceNumber: String
          _tmpInvoiceNumber = _stmt.getText(_columnIndexOfInvoiceNumber)
          val _tmpInvoiceDate: String?
          if (_stmt.isNull(_columnIndexOfInvoiceDate)) {
            _tmpInvoiceDate = null
          } else {
            _tmpInvoiceDate = _stmt.getText(_columnIndexOfInvoiceDate)
          }
          val _tmpConsigneeName: String?
          if (_stmt.isNull(_columnIndexOfConsigneeName)) {
            _tmpConsigneeName = null
          } else {
            _tmpConsigneeName = _stmt.getText(_columnIndexOfConsigneeName)
          }
          val _tmpConsigneeGstin: String?
          if (_stmt.isNull(_columnIndexOfConsigneeGstin)) {
            _tmpConsigneeGstin = null
          } else {
            _tmpConsigneeGstin = _stmt.getText(_columnIndexOfConsigneeGstin)
          }
          val _tmpTotalAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfTotalAmountPaise)) {
            _tmpTotalAmountPaise = null
          } else {
            _tmpTotalAmountPaise = _stmt.getLong(_columnIndexOfTotalAmountPaise)
          }
          val _tmpTotalQuantity: Double?
          if (_stmt.isNull(_columnIndexOfTotalQuantity)) {
            _tmpTotalQuantity = null
          } else {
            _tmpTotalQuantity = _stmt.getDouble(_columnIndexOfTotalQuantity)
          }
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawOcrText: String?
          if (_stmt.isNull(_columnIndexOfRawOcrText)) {
            _tmpRawOcrText = null
          } else {
            _tmpRawOcrText = _stmt.getText(_columnIndexOfRawOcrText)
          }
          val _tmpStatus: PurchaseInvoiceStatus
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfStatus)
          }
          val _tmp_1: PurchaseInvoiceStatus? = __converters.toPurchaseInvoiceStatus(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.PurchaseInvoiceStatus', but it was NULL.")
          } else {
            _tmpStatus = _tmp_1
          }
          val _tmpSupportingDocRef: String?
          if (_stmt.isNull(_columnIndexOfSupportingDocRef)) {
            _tmpSupportingDocRef = null
          } else {
            _tmpSupportingDocRef = _stmt.getText(_columnIndexOfSupportingDocRef)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = PurchaseInvoiceEntity(_tmpId,_tmpSupplierName,_tmpSupplierGstin,_tmpInvoiceNumber,_tmpInvoiceDate,_tmpConsigneeName,_tmpConsigneeGstin,_tmpTotalAmountPaise,_tmpTotalQuantity,_tmpImageUri,_tmpRawOcrText,_tmpStatus,_tmpSupportingDocRef,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getInvoiceById(invoiceId: Long): PurchaseInvoiceEntity? {
    val _sql: String = "SELECT * FROM purchase_invoices WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, invoiceId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierName: Int = getColumnIndexOrThrow(_stmt, "supplier_name")
        val _columnIndexOfSupplierGstin: Int = getColumnIndexOrThrow(_stmt, "supplier_gstin")
        val _columnIndexOfInvoiceNumber: Int = getColumnIndexOrThrow(_stmt, "invoice_number")
        val _columnIndexOfInvoiceDate: Int = getColumnIndexOrThrow(_stmt, "invoice_date")
        val _columnIndexOfConsigneeName: Int = getColumnIndexOrThrow(_stmt, "consignee_name")
        val _columnIndexOfConsigneeGstin: Int = getColumnIndexOrThrow(_stmt, "consignee_gstin")
        val _columnIndexOfTotalAmountPaise: Int = getColumnIndexOrThrow(_stmt, "total_amount_paise")
        val _columnIndexOfTotalQuantity: Int = getColumnIndexOrThrow(_stmt, "total_quantity")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "image_uri")
        val _columnIndexOfRawOcrText: Int = getColumnIndexOrThrow(_stmt, "raw_ocr_text")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSupportingDocRef: Int = getColumnIndexOrThrow(_stmt, "supporting_doc_ref")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: PurchaseInvoiceEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierName: String
          _tmpSupplierName = _stmt.getText(_columnIndexOfSupplierName)
          val _tmpSupplierGstin: String?
          if (_stmt.isNull(_columnIndexOfSupplierGstin)) {
            _tmpSupplierGstin = null
          } else {
            _tmpSupplierGstin = _stmt.getText(_columnIndexOfSupplierGstin)
          }
          val _tmpInvoiceNumber: String
          _tmpInvoiceNumber = _stmt.getText(_columnIndexOfInvoiceNumber)
          val _tmpInvoiceDate: String?
          if (_stmt.isNull(_columnIndexOfInvoiceDate)) {
            _tmpInvoiceDate = null
          } else {
            _tmpInvoiceDate = _stmt.getText(_columnIndexOfInvoiceDate)
          }
          val _tmpConsigneeName: String?
          if (_stmt.isNull(_columnIndexOfConsigneeName)) {
            _tmpConsigneeName = null
          } else {
            _tmpConsigneeName = _stmt.getText(_columnIndexOfConsigneeName)
          }
          val _tmpConsigneeGstin: String?
          if (_stmt.isNull(_columnIndexOfConsigneeGstin)) {
            _tmpConsigneeGstin = null
          } else {
            _tmpConsigneeGstin = _stmt.getText(_columnIndexOfConsigneeGstin)
          }
          val _tmpTotalAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfTotalAmountPaise)) {
            _tmpTotalAmountPaise = null
          } else {
            _tmpTotalAmountPaise = _stmt.getLong(_columnIndexOfTotalAmountPaise)
          }
          val _tmpTotalQuantity: Double?
          if (_stmt.isNull(_columnIndexOfTotalQuantity)) {
            _tmpTotalQuantity = null
          } else {
            _tmpTotalQuantity = _stmt.getDouble(_columnIndexOfTotalQuantity)
          }
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawOcrText: String?
          if (_stmt.isNull(_columnIndexOfRawOcrText)) {
            _tmpRawOcrText = null
          } else {
            _tmpRawOcrText = _stmt.getText(_columnIndexOfRawOcrText)
          }
          val _tmpStatus: PurchaseInvoiceStatus
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfStatus)
          }
          val _tmp_1: PurchaseInvoiceStatus? = __converters.toPurchaseInvoiceStatus(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.PurchaseInvoiceStatus', but it was NULL.")
          } else {
            _tmpStatus = _tmp_1
          }
          val _tmpSupportingDocRef: String?
          if (_stmt.isNull(_columnIndexOfSupportingDocRef)) {
            _tmpSupportingDocRef = null
          } else {
            _tmpSupportingDocRef = _stmt.getText(_columnIndexOfSupportingDocRef)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = PurchaseInvoiceEntity(_tmpId,_tmpSupplierName,_tmpSupplierGstin,_tmpInvoiceNumber,_tmpInvoiceDate,_tmpConsigneeName,_tmpConsigneeGstin,_tmpTotalAmountPaise,_tmpTotalQuantity,_tmpImageUri,_tmpRawOcrText,_tmpStatus,_tmpSupportingDocRef,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getLinesForInvoice(invoiceId: Long): Flow<List<PurchaseLineEntity>> {
    val _sql: String = "SELECT * FROM purchase_lines WHERE invoice_id = ? ORDER BY line_number ASC"
    return createFlow(__db, false, arrayOf("purchase_lines")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, invoiceId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfInvoiceId: Int = getColumnIndexOrThrow(_stmt, "invoice_id")
        val _columnIndexOfLineNumber: Int = getColumnIndexOrThrow(_stmt, "line_number")
        val _columnIndexOfVariantId: Int = getColumnIndexOrThrow(_stmt, "variant_id")
        val _columnIndexOfSupplierItemCode: Int = getColumnIndexOrThrow(_stmt, "supplier_item_code")
        val _columnIndexOfRawDescription: Int = getColumnIndexOrThrow(_stmt, "raw_description")
        val _columnIndexOfCorrectedDescription: Int = getColumnIndexOrThrow(_stmt, "corrected_description")
        val _columnIndexOfHsnCode: Int = getColumnIndexOrThrow(_stmt, "hsn_code")
        val _columnIndexOfRawQuantity: Int = getColumnIndexOrThrow(_stmt, "raw_quantity")
        val _columnIndexOfCorrectedQuantity: Int = getColumnIndexOrThrow(_stmt, "corrected_quantity")
        val _columnIndexOfRawUnit: Int = getColumnIndexOrThrow(_stmt, "raw_unit")
        val _columnIndexOfInventoryUnit: Int = getColumnIndexOrThrow(_stmt, "inventory_unit")
        val _columnIndexOfUnitConversionFactor: Int = getColumnIndexOrThrow(_stmt, "unit_conversion_factor")
        val _columnIndexOfEffectiveQuantityBase: Int = getColumnIndexOrThrow(_stmt, "effective_quantity_base")
        val _columnIndexOfRawUnitPricePaise: Int = getColumnIndexOrThrow(_stmt, "raw_unit_price_paise")
        val _columnIndexOfCorrectedUnitPricePaise: Int = getColumnIndexOrThrow(_stmt, "corrected_unit_price_paise")
        val _columnIndexOfRawLineAmountPaise: Int = getColumnIndexOrThrow(_stmt, "raw_line_amount_paise")
        val _columnIndexOfCorrectedLineAmountPaise: Int = getColumnIndexOrThrow(_stmt, "corrected_line_amount_paise")
        val _columnIndexOfGstRatePercent: Int = getColumnIndexOrThrow(_stmt, "gst_rate_percent")
        val _columnIndexOfReviewStatus: Int = getColumnIndexOrThrow(_stmt, "review_status")
        val _columnIndexOfRejectionReason: Int = getColumnIndexOrThrow(_stmt, "rejection_reason")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<PurchaseLineEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PurchaseLineEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpInvoiceId: Long
          _tmpInvoiceId = _stmt.getLong(_columnIndexOfInvoiceId)
          val _tmpLineNumber: Int
          _tmpLineNumber = _stmt.getLong(_columnIndexOfLineNumber).toInt()
          val _tmpVariantId: Long?
          if (_stmt.isNull(_columnIndexOfVariantId)) {
            _tmpVariantId = null
          } else {
            _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          }
          val _tmpSupplierItemCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierItemCode)) {
            _tmpSupplierItemCode = null
          } else {
            _tmpSupplierItemCode = _stmt.getText(_columnIndexOfSupplierItemCode)
          }
          val _tmpRawDescription: String
          _tmpRawDescription = _stmt.getText(_columnIndexOfRawDescription)
          val _tmpCorrectedDescription: String?
          if (_stmt.isNull(_columnIndexOfCorrectedDescription)) {
            _tmpCorrectedDescription = null
          } else {
            _tmpCorrectedDescription = _stmt.getText(_columnIndexOfCorrectedDescription)
          }
          val _tmpHsnCode: String?
          if (_stmt.isNull(_columnIndexOfHsnCode)) {
            _tmpHsnCode = null
          } else {
            _tmpHsnCode = _stmt.getText(_columnIndexOfHsnCode)
          }
          val _tmpRawQuantity: Double
          _tmpRawQuantity = _stmt.getDouble(_columnIndexOfRawQuantity)
          val _tmpCorrectedQuantity: Double?
          if (_stmt.isNull(_columnIndexOfCorrectedQuantity)) {
            _tmpCorrectedQuantity = null
          } else {
            _tmpCorrectedQuantity = _stmt.getDouble(_columnIndexOfCorrectedQuantity)
          }
          val _tmpRawUnit: String
          _tmpRawUnit = _stmt.getText(_columnIndexOfRawUnit)
          val _tmpInventoryUnit: InventoryUnit
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfInventoryUnit)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfInventoryUnit)
          }
          val _tmp_1: InventoryUnit? = __converters.toInventoryUnit(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.InventoryUnit', but it was NULL.")
          } else {
            _tmpInventoryUnit = _tmp_1
          }
          val _tmpUnitConversionFactor: Int
          _tmpUnitConversionFactor = _stmt.getLong(_columnIndexOfUnitConversionFactor).toInt()
          val _tmpEffectiveQuantityBase: Int
          _tmpEffectiveQuantityBase = _stmt.getLong(_columnIndexOfEffectiveQuantityBase).toInt()
          val _tmpRawUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfRawUnitPricePaise)) {
            _tmpRawUnitPricePaise = null
          } else {
            _tmpRawUnitPricePaise = _stmt.getLong(_columnIndexOfRawUnitPricePaise)
          }
          val _tmpCorrectedUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfCorrectedUnitPricePaise)) {
            _tmpCorrectedUnitPricePaise = null
          } else {
            _tmpCorrectedUnitPricePaise = _stmt.getLong(_columnIndexOfCorrectedUnitPricePaise)
          }
          val _tmpRawLineAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfRawLineAmountPaise)) {
            _tmpRawLineAmountPaise = null
          } else {
            _tmpRawLineAmountPaise = _stmt.getLong(_columnIndexOfRawLineAmountPaise)
          }
          val _tmpCorrectedLineAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfCorrectedLineAmountPaise)) {
            _tmpCorrectedLineAmountPaise = null
          } else {
            _tmpCorrectedLineAmountPaise = _stmt.getLong(_columnIndexOfCorrectedLineAmountPaise)
          }
          val _tmpGstRatePercent: Double?
          if (_stmt.isNull(_columnIndexOfGstRatePercent)) {
            _tmpGstRatePercent = null
          } else {
            _tmpGstRatePercent = _stmt.getDouble(_columnIndexOfGstRatePercent)
          }
          val _tmpReviewStatus: LineReviewStatus
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfReviewStatus)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfReviewStatus)
          }
          val _tmp_3: LineReviewStatus? = __converters.toLineReviewStatus(_tmp_2)
          if (_tmp_3 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.LineReviewStatus', but it was NULL.")
          } else {
            _tmpReviewStatus = _tmp_3
          }
          val _tmpRejectionReason: String?
          if (_stmt.isNull(_columnIndexOfRejectionReason)) {
            _tmpRejectionReason = null
          } else {
            _tmpRejectionReason = _stmt.getText(_columnIndexOfRejectionReason)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = PurchaseLineEntity(_tmpId,_tmpInvoiceId,_tmpLineNumber,_tmpVariantId,_tmpSupplierItemCode,_tmpRawDescription,_tmpCorrectedDescription,_tmpHsnCode,_tmpRawQuantity,_tmpCorrectedQuantity,_tmpRawUnit,_tmpInventoryUnit,_tmpUnitConversionFactor,_tmpEffectiveQuantityBase,_tmpRawUnitPricePaise,_tmpCorrectedUnitPricePaise,_tmpRawLineAmountPaise,_tmpCorrectedLineAmountPaise,_tmpGstRatePercent,_tmpReviewStatus,_tmpRejectionReason,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getLineById(lineId: Long): PurchaseLineEntity? {
    val _sql: String = "SELECT * FROM purchase_lines WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, lineId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfInvoiceId: Int = getColumnIndexOrThrow(_stmt, "invoice_id")
        val _columnIndexOfLineNumber: Int = getColumnIndexOrThrow(_stmt, "line_number")
        val _columnIndexOfVariantId: Int = getColumnIndexOrThrow(_stmt, "variant_id")
        val _columnIndexOfSupplierItemCode: Int = getColumnIndexOrThrow(_stmt, "supplier_item_code")
        val _columnIndexOfRawDescription: Int = getColumnIndexOrThrow(_stmt, "raw_description")
        val _columnIndexOfCorrectedDescription: Int = getColumnIndexOrThrow(_stmt, "corrected_description")
        val _columnIndexOfHsnCode: Int = getColumnIndexOrThrow(_stmt, "hsn_code")
        val _columnIndexOfRawQuantity: Int = getColumnIndexOrThrow(_stmt, "raw_quantity")
        val _columnIndexOfCorrectedQuantity: Int = getColumnIndexOrThrow(_stmt, "corrected_quantity")
        val _columnIndexOfRawUnit: Int = getColumnIndexOrThrow(_stmt, "raw_unit")
        val _columnIndexOfInventoryUnit: Int = getColumnIndexOrThrow(_stmt, "inventory_unit")
        val _columnIndexOfUnitConversionFactor: Int = getColumnIndexOrThrow(_stmt, "unit_conversion_factor")
        val _columnIndexOfEffectiveQuantityBase: Int = getColumnIndexOrThrow(_stmt, "effective_quantity_base")
        val _columnIndexOfRawUnitPricePaise: Int = getColumnIndexOrThrow(_stmt, "raw_unit_price_paise")
        val _columnIndexOfCorrectedUnitPricePaise: Int = getColumnIndexOrThrow(_stmt, "corrected_unit_price_paise")
        val _columnIndexOfRawLineAmountPaise: Int = getColumnIndexOrThrow(_stmt, "raw_line_amount_paise")
        val _columnIndexOfCorrectedLineAmountPaise: Int = getColumnIndexOrThrow(_stmt, "corrected_line_amount_paise")
        val _columnIndexOfGstRatePercent: Int = getColumnIndexOrThrow(_stmt, "gst_rate_percent")
        val _columnIndexOfReviewStatus: Int = getColumnIndexOrThrow(_stmt, "review_status")
        val _columnIndexOfRejectionReason: Int = getColumnIndexOrThrow(_stmt, "rejection_reason")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: PurchaseLineEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpInvoiceId: Long
          _tmpInvoiceId = _stmt.getLong(_columnIndexOfInvoiceId)
          val _tmpLineNumber: Int
          _tmpLineNumber = _stmt.getLong(_columnIndexOfLineNumber).toInt()
          val _tmpVariantId: Long?
          if (_stmt.isNull(_columnIndexOfVariantId)) {
            _tmpVariantId = null
          } else {
            _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          }
          val _tmpSupplierItemCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierItemCode)) {
            _tmpSupplierItemCode = null
          } else {
            _tmpSupplierItemCode = _stmt.getText(_columnIndexOfSupplierItemCode)
          }
          val _tmpRawDescription: String
          _tmpRawDescription = _stmt.getText(_columnIndexOfRawDescription)
          val _tmpCorrectedDescription: String?
          if (_stmt.isNull(_columnIndexOfCorrectedDescription)) {
            _tmpCorrectedDescription = null
          } else {
            _tmpCorrectedDescription = _stmt.getText(_columnIndexOfCorrectedDescription)
          }
          val _tmpHsnCode: String?
          if (_stmt.isNull(_columnIndexOfHsnCode)) {
            _tmpHsnCode = null
          } else {
            _tmpHsnCode = _stmt.getText(_columnIndexOfHsnCode)
          }
          val _tmpRawQuantity: Double
          _tmpRawQuantity = _stmt.getDouble(_columnIndexOfRawQuantity)
          val _tmpCorrectedQuantity: Double?
          if (_stmt.isNull(_columnIndexOfCorrectedQuantity)) {
            _tmpCorrectedQuantity = null
          } else {
            _tmpCorrectedQuantity = _stmt.getDouble(_columnIndexOfCorrectedQuantity)
          }
          val _tmpRawUnit: String
          _tmpRawUnit = _stmt.getText(_columnIndexOfRawUnit)
          val _tmpInventoryUnit: InventoryUnit
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfInventoryUnit)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfInventoryUnit)
          }
          val _tmp_1: InventoryUnit? = __converters.toInventoryUnit(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.InventoryUnit', but it was NULL.")
          } else {
            _tmpInventoryUnit = _tmp_1
          }
          val _tmpUnitConversionFactor: Int
          _tmpUnitConversionFactor = _stmt.getLong(_columnIndexOfUnitConversionFactor).toInt()
          val _tmpEffectiveQuantityBase: Int
          _tmpEffectiveQuantityBase = _stmt.getLong(_columnIndexOfEffectiveQuantityBase).toInt()
          val _tmpRawUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfRawUnitPricePaise)) {
            _tmpRawUnitPricePaise = null
          } else {
            _tmpRawUnitPricePaise = _stmt.getLong(_columnIndexOfRawUnitPricePaise)
          }
          val _tmpCorrectedUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfCorrectedUnitPricePaise)) {
            _tmpCorrectedUnitPricePaise = null
          } else {
            _tmpCorrectedUnitPricePaise = _stmt.getLong(_columnIndexOfCorrectedUnitPricePaise)
          }
          val _tmpRawLineAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfRawLineAmountPaise)) {
            _tmpRawLineAmountPaise = null
          } else {
            _tmpRawLineAmountPaise = _stmt.getLong(_columnIndexOfRawLineAmountPaise)
          }
          val _tmpCorrectedLineAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfCorrectedLineAmountPaise)) {
            _tmpCorrectedLineAmountPaise = null
          } else {
            _tmpCorrectedLineAmountPaise = _stmt.getLong(_columnIndexOfCorrectedLineAmountPaise)
          }
          val _tmpGstRatePercent: Double?
          if (_stmt.isNull(_columnIndexOfGstRatePercent)) {
            _tmpGstRatePercent = null
          } else {
            _tmpGstRatePercent = _stmt.getDouble(_columnIndexOfGstRatePercent)
          }
          val _tmpReviewStatus: LineReviewStatus
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfReviewStatus)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfReviewStatus)
          }
          val _tmp_3: LineReviewStatus? = __converters.toLineReviewStatus(_tmp_2)
          if (_tmp_3 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.LineReviewStatus', but it was NULL.")
          } else {
            _tmpReviewStatus = _tmp_3
          }
          val _tmpRejectionReason: String?
          if (_stmt.isNull(_columnIndexOfRejectionReason)) {
            _tmpRejectionReason = null
          } else {
            _tmpRejectionReason = _stmt.getText(_columnIndexOfRejectionReason)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = PurchaseLineEntity(_tmpId,_tmpInvoiceId,_tmpLineNumber,_tmpVariantId,_tmpSupplierItemCode,_tmpRawDescription,_tmpCorrectedDescription,_tmpHsnCode,_tmpRawQuantity,_tmpCorrectedQuantity,_tmpRawUnit,_tmpInventoryUnit,_tmpUnitConversionFactor,_tmpEffectiveQuantityBase,_tmpRawUnitPricePaise,_tmpCorrectedUnitPricePaise,_tmpRawLineAmountPaise,_tmpCorrectedLineAmountPaise,_tmpGstRatePercent,_tmpReviewStatus,_tmpRejectionReason,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getInvoiceWithLines(invoiceId: Long): Flow<PurchaseInvoiceWithLines?> {
    val _sql: String = "SELECT * FROM purchase_invoices WHERE id = ?"
    return createFlow(__db, true, arrayOf("purchase_lines", "purchase_invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, invoiceId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierName: Int = getColumnIndexOrThrow(_stmt, "supplier_name")
        val _columnIndexOfSupplierGstin: Int = getColumnIndexOrThrow(_stmt, "supplier_gstin")
        val _columnIndexOfInvoiceNumber: Int = getColumnIndexOrThrow(_stmt, "invoice_number")
        val _columnIndexOfInvoiceDate: Int = getColumnIndexOrThrow(_stmt, "invoice_date")
        val _columnIndexOfConsigneeName: Int = getColumnIndexOrThrow(_stmt, "consignee_name")
        val _columnIndexOfConsigneeGstin: Int = getColumnIndexOrThrow(_stmt, "consignee_gstin")
        val _columnIndexOfTotalAmountPaise: Int = getColumnIndexOrThrow(_stmt, "total_amount_paise")
        val _columnIndexOfTotalQuantity: Int = getColumnIndexOrThrow(_stmt, "total_quantity")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "image_uri")
        val _columnIndexOfRawOcrText: Int = getColumnIndexOrThrow(_stmt, "raw_ocr_text")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSupportingDocRef: Int = getColumnIndexOrThrow(_stmt, "supporting_doc_ref")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _collectionLines: LongSparseArray<MutableList<PurchaseLineEntity>> = LongSparseArray<MutableList<PurchaseLineEntity>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionLines.containsKey(_tmpKey)) {
            _collectionLines.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshippurchaseLinesAsdevVigneshCycleshopDataLocalEntityPurchaseLineEntity(_connection, _collectionLines)
        val _result: PurchaseInvoiceWithLines?
        if (_stmt.step()) {
          val _tmpInvoice: PurchaseInvoiceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierName: String
          _tmpSupplierName = _stmt.getText(_columnIndexOfSupplierName)
          val _tmpSupplierGstin: String?
          if (_stmt.isNull(_columnIndexOfSupplierGstin)) {
            _tmpSupplierGstin = null
          } else {
            _tmpSupplierGstin = _stmt.getText(_columnIndexOfSupplierGstin)
          }
          val _tmpInvoiceNumber: String
          _tmpInvoiceNumber = _stmt.getText(_columnIndexOfInvoiceNumber)
          val _tmpInvoiceDate: String?
          if (_stmt.isNull(_columnIndexOfInvoiceDate)) {
            _tmpInvoiceDate = null
          } else {
            _tmpInvoiceDate = _stmt.getText(_columnIndexOfInvoiceDate)
          }
          val _tmpConsigneeName: String?
          if (_stmt.isNull(_columnIndexOfConsigneeName)) {
            _tmpConsigneeName = null
          } else {
            _tmpConsigneeName = _stmt.getText(_columnIndexOfConsigneeName)
          }
          val _tmpConsigneeGstin: String?
          if (_stmt.isNull(_columnIndexOfConsigneeGstin)) {
            _tmpConsigneeGstin = null
          } else {
            _tmpConsigneeGstin = _stmt.getText(_columnIndexOfConsigneeGstin)
          }
          val _tmpTotalAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfTotalAmountPaise)) {
            _tmpTotalAmountPaise = null
          } else {
            _tmpTotalAmountPaise = _stmt.getLong(_columnIndexOfTotalAmountPaise)
          }
          val _tmpTotalQuantity: Double?
          if (_stmt.isNull(_columnIndexOfTotalQuantity)) {
            _tmpTotalQuantity = null
          } else {
            _tmpTotalQuantity = _stmt.getDouble(_columnIndexOfTotalQuantity)
          }
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawOcrText: String?
          if (_stmt.isNull(_columnIndexOfRawOcrText)) {
            _tmpRawOcrText = null
          } else {
            _tmpRawOcrText = _stmt.getText(_columnIndexOfRawOcrText)
          }
          val _tmpStatus: PurchaseInvoiceStatus
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfStatus)
          }
          val _tmp_1: PurchaseInvoiceStatus? = __converters.toPurchaseInvoiceStatus(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.PurchaseInvoiceStatus', but it was NULL.")
          } else {
            _tmpStatus = _tmp_1
          }
          val _tmpSupportingDocRef: String?
          if (_stmt.isNull(_columnIndexOfSupportingDocRef)) {
            _tmpSupportingDocRef = null
          } else {
            _tmpSupportingDocRef = _stmt.getText(_columnIndexOfSupportingDocRef)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _tmpInvoice = PurchaseInvoiceEntity(_tmpId,_tmpSupplierName,_tmpSupplierGstin,_tmpInvoiceNumber,_tmpInvoiceDate,_tmpConsigneeName,_tmpConsigneeGstin,_tmpTotalAmountPaise,_tmpTotalQuantity,_tmpImageUri,_tmpRawOcrText,_tmpStatus,_tmpSupportingDocRef,_tmpCreatedAt,_tmpUpdatedAt)
          val _tmpLinesCollection: MutableList<PurchaseLineEntity>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpLinesCollection = checkNotNull(_collectionLines.get(_tmpKey_1))
          _result = PurchaseInvoiceWithLines(_tmpInvoice,_tmpLinesCollection)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllInvoicesWithLines(): Flow<List<PurchaseInvoiceWithLines>> {
    val _sql: String = "SELECT * FROM purchase_invoices ORDER BY created_at DESC"
    return createFlow(__db, true, arrayOf("purchase_lines", "purchase_invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierName: Int = getColumnIndexOrThrow(_stmt, "supplier_name")
        val _columnIndexOfSupplierGstin: Int = getColumnIndexOrThrow(_stmt, "supplier_gstin")
        val _columnIndexOfInvoiceNumber: Int = getColumnIndexOrThrow(_stmt, "invoice_number")
        val _columnIndexOfInvoiceDate: Int = getColumnIndexOrThrow(_stmt, "invoice_date")
        val _columnIndexOfConsigneeName: Int = getColumnIndexOrThrow(_stmt, "consignee_name")
        val _columnIndexOfConsigneeGstin: Int = getColumnIndexOrThrow(_stmt, "consignee_gstin")
        val _columnIndexOfTotalAmountPaise: Int = getColumnIndexOrThrow(_stmt, "total_amount_paise")
        val _columnIndexOfTotalQuantity: Int = getColumnIndexOrThrow(_stmt, "total_quantity")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "image_uri")
        val _columnIndexOfRawOcrText: Int = getColumnIndexOrThrow(_stmt, "raw_ocr_text")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSupportingDocRef: Int = getColumnIndexOrThrow(_stmt, "supporting_doc_ref")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _collectionLines: LongSparseArray<MutableList<PurchaseLineEntity>> = LongSparseArray<MutableList<PurchaseLineEntity>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionLines.containsKey(_tmpKey)) {
            _collectionLines.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshippurchaseLinesAsdevVigneshCycleshopDataLocalEntityPurchaseLineEntity(_connection, _collectionLines)
        val _result: MutableList<PurchaseInvoiceWithLines> = mutableListOf()
        while (_stmt.step()) {
          val _item: PurchaseInvoiceWithLines
          val _tmpInvoice: PurchaseInvoiceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierName: String
          _tmpSupplierName = _stmt.getText(_columnIndexOfSupplierName)
          val _tmpSupplierGstin: String?
          if (_stmt.isNull(_columnIndexOfSupplierGstin)) {
            _tmpSupplierGstin = null
          } else {
            _tmpSupplierGstin = _stmt.getText(_columnIndexOfSupplierGstin)
          }
          val _tmpInvoiceNumber: String
          _tmpInvoiceNumber = _stmt.getText(_columnIndexOfInvoiceNumber)
          val _tmpInvoiceDate: String?
          if (_stmt.isNull(_columnIndexOfInvoiceDate)) {
            _tmpInvoiceDate = null
          } else {
            _tmpInvoiceDate = _stmt.getText(_columnIndexOfInvoiceDate)
          }
          val _tmpConsigneeName: String?
          if (_stmt.isNull(_columnIndexOfConsigneeName)) {
            _tmpConsigneeName = null
          } else {
            _tmpConsigneeName = _stmt.getText(_columnIndexOfConsigneeName)
          }
          val _tmpConsigneeGstin: String?
          if (_stmt.isNull(_columnIndexOfConsigneeGstin)) {
            _tmpConsigneeGstin = null
          } else {
            _tmpConsigneeGstin = _stmt.getText(_columnIndexOfConsigneeGstin)
          }
          val _tmpTotalAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfTotalAmountPaise)) {
            _tmpTotalAmountPaise = null
          } else {
            _tmpTotalAmountPaise = _stmt.getLong(_columnIndexOfTotalAmountPaise)
          }
          val _tmpTotalQuantity: Double?
          if (_stmt.isNull(_columnIndexOfTotalQuantity)) {
            _tmpTotalQuantity = null
          } else {
            _tmpTotalQuantity = _stmt.getDouble(_columnIndexOfTotalQuantity)
          }
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawOcrText: String?
          if (_stmt.isNull(_columnIndexOfRawOcrText)) {
            _tmpRawOcrText = null
          } else {
            _tmpRawOcrText = _stmt.getText(_columnIndexOfRawOcrText)
          }
          val _tmpStatus: PurchaseInvoiceStatus
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfStatus)
          }
          val _tmp_1: PurchaseInvoiceStatus? = __converters.toPurchaseInvoiceStatus(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.PurchaseInvoiceStatus', but it was NULL.")
          } else {
            _tmpStatus = _tmp_1
          }
          val _tmpSupportingDocRef: String?
          if (_stmt.isNull(_columnIndexOfSupportingDocRef)) {
            _tmpSupportingDocRef = null
          } else {
            _tmpSupportingDocRef = _stmt.getText(_columnIndexOfSupportingDocRef)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _tmpInvoice = PurchaseInvoiceEntity(_tmpId,_tmpSupplierName,_tmpSupplierGstin,_tmpInvoiceNumber,_tmpInvoiceDate,_tmpConsigneeName,_tmpConsigneeGstin,_tmpTotalAmountPaise,_tmpTotalQuantity,_tmpImageUri,_tmpRawOcrText,_tmpStatus,_tmpSupportingDocRef,_tmpCreatedAt,_tmpUpdatedAt)
          val _tmpLinesCollection: MutableList<PurchaseLineEntity>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpLinesCollection = checkNotNull(_collectionLines.get(_tmpKey_1))
          _item = PurchaseInvoiceWithLines(_tmpInvoice,_tmpLinesCollection)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getInvoicesByStatus(status: PurchaseInvoiceStatus): Flow<List<PurchaseInvoiceWithLines>> {
    val _sql: String = "SELECT * FROM purchase_invoices WHERE status = ? ORDER BY created_at DESC"
    return createFlow(__db, true, arrayOf("purchase_lines", "purchase_invoices")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __converters.fromPurchaseInvoiceStatus(status)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierName: Int = getColumnIndexOrThrow(_stmt, "supplier_name")
        val _columnIndexOfSupplierGstin: Int = getColumnIndexOrThrow(_stmt, "supplier_gstin")
        val _columnIndexOfInvoiceNumber: Int = getColumnIndexOrThrow(_stmt, "invoice_number")
        val _columnIndexOfInvoiceDate: Int = getColumnIndexOrThrow(_stmt, "invoice_date")
        val _columnIndexOfConsigneeName: Int = getColumnIndexOrThrow(_stmt, "consignee_name")
        val _columnIndexOfConsigneeGstin: Int = getColumnIndexOrThrow(_stmt, "consignee_gstin")
        val _columnIndexOfTotalAmountPaise: Int = getColumnIndexOrThrow(_stmt, "total_amount_paise")
        val _columnIndexOfTotalQuantity: Int = getColumnIndexOrThrow(_stmt, "total_quantity")
        val _columnIndexOfImageUri: Int = getColumnIndexOrThrow(_stmt, "image_uri")
        val _columnIndexOfRawOcrText: Int = getColumnIndexOrThrow(_stmt, "raw_ocr_text")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfSupportingDocRef: Int = getColumnIndexOrThrow(_stmt, "supporting_doc_ref")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _collectionLines: LongSparseArray<MutableList<PurchaseLineEntity>> = LongSparseArray<MutableList<PurchaseLineEntity>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionLines.containsKey(_tmpKey)) {
            _collectionLines.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshippurchaseLinesAsdevVigneshCycleshopDataLocalEntityPurchaseLineEntity(_connection, _collectionLines)
        val _result: MutableList<PurchaseInvoiceWithLines> = mutableListOf()
        while (_stmt.step()) {
          val _item: PurchaseInvoiceWithLines
          val _tmpInvoice: PurchaseInvoiceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierName: String
          _tmpSupplierName = _stmt.getText(_columnIndexOfSupplierName)
          val _tmpSupplierGstin: String?
          if (_stmt.isNull(_columnIndexOfSupplierGstin)) {
            _tmpSupplierGstin = null
          } else {
            _tmpSupplierGstin = _stmt.getText(_columnIndexOfSupplierGstin)
          }
          val _tmpInvoiceNumber: String
          _tmpInvoiceNumber = _stmt.getText(_columnIndexOfInvoiceNumber)
          val _tmpInvoiceDate: String?
          if (_stmt.isNull(_columnIndexOfInvoiceDate)) {
            _tmpInvoiceDate = null
          } else {
            _tmpInvoiceDate = _stmt.getText(_columnIndexOfInvoiceDate)
          }
          val _tmpConsigneeName: String?
          if (_stmt.isNull(_columnIndexOfConsigneeName)) {
            _tmpConsigneeName = null
          } else {
            _tmpConsigneeName = _stmt.getText(_columnIndexOfConsigneeName)
          }
          val _tmpConsigneeGstin: String?
          if (_stmt.isNull(_columnIndexOfConsigneeGstin)) {
            _tmpConsigneeGstin = null
          } else {
            _tmpConsigneeGstin = _stmt.getText(_columnIndexOfConsigneeGstin)
          }
          val _tmpTotalAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfTotalAmountPaise)) {
            _tmpTotalAmountPaise = null
          } else {
            _tmpTotalAmountPaise = _stmt.getLong(_columnIndexOfTotalAmountPaise)
          }
          val _tmpTotalQuantity: Double?
          if (_stmt.isNull(_columnIndexOfTotalQuantity)) {
            _tmpTotalQuantity = null
          } else {
            _tmpTotalQuantity = _stmt.getDouble(_columnIndexOfTotalQuantity)
          }
          val _tmpImageUri: String?
          if (_stmt.isNull(_columnIndexOfImageUri)) {
            _tmpImageUri = null
          } else {
            _tmpImageUri = _stmt.getText(_columnIndexOfImageUri)
          }
          val _tmpRawOcrText: String?
          if (_stmt.isNull(_columnIndexOfRawOcrText)) {
            _tmpRawOcrText = null
          } else {
            _tmpRawOcrText = _stmt.getText(_columnIndexOfRawOcrText)
          }
          val _tmpStatus: PurchaseInvoiceStatus
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfStatus)
          }
          val _tmp_2: PurchaseInvoiceStatus? = __converters.toPurchaseInvoiceStatus(_tmp_1)
          if (_tmp_2 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.PurchaseInvoiceStatus', but it was NULL.")
          } else {
            _tmpStatus = _tmp_2
          }
          val _tmpSupportingDocRef: String?
          if (_stmt.isNull(_columnIndexOfSupportingDocRef)) {
            _tmpSupportingDocRef = null
          } else {
            _tmpSupportingDocRef = _stmt.getText(_columnIndexOfSupportingDocRef)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _tmpInvoice = PurchaseInvoiceEntity(_tmpId,_tmpSupplierName,_tmpSupplierGstin,_tmpInvoiceNumber,_tmpInvoiceDate,_tmpConsigneeName,_tmpConsigneeGstin,_tmpTotalAmountPaise,_tmpTotalQuantity,_tmpImageUri,_tmpRawOcrText,_tmpStatus,_tmpSupportingDocRef,_tmpCreatedAt,_tmpUpdatedAt)
          val _tmpLinesCollection: MutableList<PurchaseLineEntity>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpLinesCollection = checkNotNull(_collectionLines.get(_tmpKey_1))
          _item = PurchaseInvoiceWithLines(_tmpInvoice,_tmpLinesCollection)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getVerifiedMapping(supplierIdentifier: String, itemCode: String?): SupplierProductMappingEntity? {
    val _sql: String = """
        |
        |        SELECT * FROM supplier_product_mappings 
        |        WHERE supplier_identifier = ? 
        |          AND (supplier_item_code = ? OR ? IS NULL)
        |        LIMIT 1
        |    
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, supplierIdentifier)
        _argIndex = 2
        if (itemCode == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, itemCode)
        }
        _argIndex = 3
        if (itemCode == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, itemCode)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierIdentifier: Int = getColumnIndexOrThrow(_stmt, "supplier_identifier")
        val _columnIndexOfSupplierItemCode: Int = getColumnIndexOrThrow(_stmt, "supplier_item_code")
        val _columnIndexOfSupplierDescPattern: Int = getColumnIndexOrThrow(_stmt, "supplier_desc_pattern")
        val _columnIndexOfVariantId: Int = getColumnIndexOrThrow(_stmt, "variant_id")
        val _columnIndexOfVerifiedByOwner: Int = getColumnIndexOrThrow(_stmt, "verified_by_owner")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: SupplierProductMappingEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierIdentifier: String
          _tmpSupplierIdentifier = _stmt.getText(_columnIndexOfSupplierIdentifier)
          val _tmpSupplierItemCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierItemCode)) {
            _tmpSupplierItemCode = null
          } else {
            _tmpSupplierItemCode = _stmt.getText(_columnIndexOfSupplierItemCode)
          }
          val _tmpSupplierDescPattern: String
          _tmpSupplierDescPattern = _stmt.getText(_columnIndexOfSupplierDescPattern)
          val _tmpVariantId: Long
          _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          val _tmpVerifiedByOwner: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfVerifiedByOwner).toInt()
          _tmpVerifiedByOwner = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = SupplierProductMappingEntity(_tmpId,_tmpSupplierIdentifier,_tmpSupplierItemCode,_tmpSupplierDescPattern,_tmpVariantId,_tmpVerifiedByOwner,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getMappingsForSupplier(supplierIdentifier: String): Flow<List<SupplierProductMappingEntity>> {
    val _sql: String = "SELECT * FROM supplier_product_mappings WHERE supplier_identifier = ?"
    return createFlow(__db, false, arrayOf("supplier_product_mappings")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, supplierIdentifier)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierIdentifier: Int = getColumnIndexOrThrow(_stmt, "supplier_identifier")
        val _columnIndexOfSupplierItemCode: Int = getColumnIndexOrThrow(_stmt, "supplier_item_code")
        val _columnIndexOfSupplierDescPattern: Int = getColumnIndexOrThrow(_stmt, "supplier_desc_pattern")
        val _columnIndexOfVariantId: Int = getColumnIndexOrThrow(_stmt, "variant_id")
        val _columnIndexOfVerifiedByOwner: Int = getColumnIndexOrThrow(_stmt, "verified_by_owner")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: MutableList<SupplierProductMappingEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SupplierProductMappingEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierIdentifier: String
          _tmpSupplierIdentifier = _stmt.getText(_columnIndexOfSupplierIdentifier)
          val _tmpSupplierItemCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierItemCode)) {
            _tmpSupplierItemCode = null
          } else {
            _tmpSupplierItemCode = _stmt.getText(_columnIndexOfSupplierItemCode)
          }
          val _tmpSupplierDescPattern: String
          _tmpSupplierDescPattern = _stmt.getText(_columnIndexOfSupplierDescPattern)
          val _tmpVariantId: Long
          _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          val _tmpVerifiedByOwner: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfVerifiedByOwner).toInt()
          _tmpVerifiedByOwner = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = SupplierProductMappingEntity(_tmpId,_tmpSupplierIdentifier,_tmpSupplierItemCode,_tmpSupplierDescPattern,_tmpVariantId,_tmpVerifiedByOwner,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllVerifiedMappings(): Flow<List<SupplierProductMappingEntity>> {
    val _sql: String = "SELECT * FROM supplier_product_mappings"
    return createFlow(__db, false, arrayOf("supplier_product_mappings")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSupplierIdentifier: Int = getColumnIndexOrThrow(_stmt, "supplier_identifier")
        val _columnIndexOfSupplierItemCode: Int = getColumnIndexOrThrow(_stmt, "supplier_item_code")
        val _columnIndexOfSupplierDescPattern: Int = getColumnIndexOrThrow(_stmt, "supplier_desc_pattern")
        val _columnIndexOfVariantId: Int = getColumnIndexOrThrow(_stmt, "variant_id")
        val _columnIndexOfVerifiedByOwner: Int = getColumnIndexOrThrow(_stmt, "verified_by_owner")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updated_at")
        val _result: MutableList<SupplierProductMappingEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SupplierProductMappingEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpSupplierIdentifier: String
          _tmpSupplierIdentifier = _stmt.getText(_columnIndexOfSupplierIdentifier)
          val _tmpSupplierItemCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierItemCode)) {
            _tmpSupplierItemCode = null
          } else {
            _tmpSupplierItemCode = _stmt.getText(_columnIndexOfSupplierItemCode)
          }
          val _tmpSupplierDescPattern: String
          _tmpSupplierDescPattern = _stmt.getText(_columnIndexOfSupplierDescPattern)
          val _tmpVariantId: Long
          _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          val _tmpVerifiedByOwner: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfVerifiedByOwner).toInt()
          _tmpVerifiedByOwner = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = SupplierProductMappingEntity(_tmpId,_tmpSupplierIdentifier,_tmpSupplierItemCode,_tmpSupplierDescPattern,_tmpVariantId,_tmpVerifiedByOwner,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateInvoiceStatus(
    invoiceId: Long,
    status: PurchaseInvoiceStatus,
    updatedAt: Long,
  ) {
    val _sql: String = "UPDATE purchase_invoices SET status = ?, updated_at = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __converters.fromPurchaseInvoiceStatus(status)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 3
        _stmt.bindLong(_argIndex, invoiceId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteInvoice(invoiceId: Long) {
    val _sql: String = "DELETE FROM purchase_invoices WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, invoiceId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateLineDetails(
    lineId: Long,
    variantId: Long?,
    status: LineReviewStatus,
    correctedQty: Double?,
    correctedPricePaise: Long?,
  ) {
    val _sql: String = """
        |
        |        UPDATE purchase_lines 
        |        SET variant_id = ?, 
        |            review_status = ?,
        |            corrected_quantity = ?,
        |            corrected_unit_price_paise = ?
        |        WHERE id = ?
        |    
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (variantId == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, variantId)
        }
        _argIndex = 2
        val _tmp: String? = __converters.fromLineReviewStatus(status)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        _argIndex = 3
        if (correctedQty == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindDouble(_argIndex, correctedQty)
        }
        _argIndex = 4
        if (correctedPricePaise == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, correctedPricePaise)
        }
        _argIndex = 5
        _stmt.bindLong(_argIndex, lineId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshippurchaseLinesAsdevVigneshCycleshopDataLocalEntityPurchaseLineEntity(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<PurchaseLineEntity>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshippurchaseLinesAsdevVigneshCycleshopDataLocalEntityPurchaseLineEntity(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`invoice_id`,`line_number`,`variant_id`,`supplier_item_code`,`raw_description`,`corrected_description`,`hsn_code`,`raw_quantity`,`corrected_quantity`,`raw_unit`,`inventory_unit`,`unit_conversion_factor`,`effective_quantity_base`,`raw_unit_price_paise`,`corrected_unit_price_paise`,`raw_line_amount_paise`,`corrected_line_amount_paise`,`gst_rate_percent`,`review_status`,`rejection_reason`,`created_at` FROM `purchase_lines` WHERE `invoice_id` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "invoice_id")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfInvoiceId: Int = 1
      val _columnIndexOfLineNumber: Int = 2
      val _columnIndexOfVariantId: Int = 3
      val _columnIndexOfSupplierItemCode: Int = 4
      val _columnIndexOfRawDescription: Int = 5
      val _columnIndexOfCorrectedDescription: Int = 6
      val _columnIndexOfHsnCode: Int = 7
      val _columnIndexOfRawQuantity: Int = 8
      val _columnIndexOfCorrectedQuantity: Int = 9
      val _columnIndexOfRawUnit: Int = 10
      val _columnIndexOfInventoryUnit: Int = 11
      val _columnIndexOfUnitConversionFactor: Int = 12
      val _columnIndexOfEffectiveQuantityBase: Int = 13
      val _columnIndexOfRawUnitPricePaise: Int = 14
      val _columnIndexOfCorrectedUnitPricePaise: Int = 15
      val _columnIndexOfRawLineAmountPaise: Int = 16
      val _columnIndexOfCorrectedLineAmountPaise: Int = 17
      val _columnIndexOfGstRatePercent: Int = 18
      val _columnIndexOfReviewStatus: Int = 19
      val _columnIndexOfRejectionReason: Int = 20
      val _columnIndexOfCreatedAt: Int = 21
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<PurchaseLineEntity>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: PurchaseLineEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpInvoiceId: Long
          _tmpInvoiceId = _stmt.getLong(_columnIndexOfInvoiceId)
          val _tmpLineNumber: Int
          _tmpLineNumber = _stmt.getLong(_columnIndexOfLineNumber).toInt()
          val _tmpVariantId: Long?
          if (_stmt.isNull(_columnIndexOfVariantId)) {
            _tmpVariantId = null
          } else {
            _tmpVariantId = _stmt.getLong(_columnIndexOfVariantId)
          }
          val _tmpSupplierItemCode: String?
          if (_stmt.isNull(_columnIndexOfSupplierItemCode)) {
            _tmpSupplierItemCode = null
          } else {
            _tmpSupplierItemCode = _stmt.getText(_columnIndexOfSupplierItemCode)
          }
          val _tmpRawDescription: String
          _tmpRawDescription = _stmt.getText(_columnIndexOfRawDescription)
          val _tmpCorrectedDescription: String?
          if (_stmt.isNull(_columnIndexOfCorrectedDescription)) {
            _tmpCorrectedDescription = null
          } else {
            _tmpCorrectedDescription = _stmt.getText(_columnIndexOfCorrectedDescription)
          }
          val _tmpHsnCode: String?
          if (_stmt.isNull(_columnIndexOfHsnCode)) {
            _tmpHsnCode = null
          } else {
            _tmpHsnCode = _stmt.getText(_columnIndexOfHsnCode)
          }
          val _tmpRawQuantity: Double
          _tmpRawQuantity = _stmt.getDouble(_columnIndexOfRawQuantity)
          val _tmpCorrectedQuantity: Double?
          if (_stmt.isNull(_columnIndexOfCorrectedQuantity)) {
            _tmpCorrectedQuantity = null
          } else {
            _tmpCorrectedQuantity = _stmt.getDouble(_columnIndexOfCorrectedQuantity)
          }
          val _tmpRawUnit: String
          _tmpRawUnit = _stmt.getText(_columnIndexOfRawUnit)
          val _tmpInventoryUnit: InventoryUnit
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfInventoryUnit)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfInventoryUnit)
          }
          val _tmp_1: InventoryUnit? = __converters.toInventoryUnit(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.InventoryUnit', but it was NULL.")
          } else {
            _tmpInventoryUnit = _tmp_1
          }
          val _tmpUnitConversionFactor: Int
          _tmpUnitConversionFactor = _stmt.getLong(_columnIndexOfUnitConversionFactor).toInt()
          val _tmpEffectiveQuantityBase: Int
          _tmpEffectiveQuantityBase = _stmt.getLong(_columnIndexOfEffectiveQuantityBase).toInt()
          val _tmpRawUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfRawUnitPricePaise)) {
            _tmpRawUnitPricePaise = null
          } else {
            _tmpRawUnitPricePaise = _stmt.getLong(_columnIndexOfRawUnitPricePaise)
          }
          val _tmpCorrectedUnitPricePaise: Long?
          if (_stmt.isNull(_columnIndexOfCorrectedUnitPricePaise)) {
            _tmpCorrectedUnitPricePaise = null
          } else {
            _tmpCorrectedUnitPricePaise = _stmt.getLong(_columnIndexOfCorrectedUnitPricePaise)
          }
          val _tmpRawLineAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfRawLineAmountPaise)) {
            _tmpRawLineAmountPaise = null
          } else {
            _tmpRawLineAmountPaise = _stmt.getLong(_columnIndexOfRawLineAmountPaise)
          }
          val _tmpCorrectedLineAmountPaise: Long?
          if (_stmt.isNull(_columnIndexOfCorrectedLineAmountPaise)) {
            _tmpCorrectedLineAmountPaise = null
          } else {
            _tmpCorrectedLineAmountPaise = _stmt.getLong(_columnIndexOfCorrectedLineAmountPaise)
          }
          val _tmpGstRatePercent: Double?
          if (_stmt.isNull(_columnIndexOfGstRatePercent)) {
            _tmpGstRatePercent = null
          } else {
            _tmpGstRatePercent = _stmt.getDouble(_columnIndexOfGstRatePercent)
          }
          val _tmpReviewStatus: LineReviewStatus
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfReviewStatus)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfReviewStatus)
          }
          val _tmp_3: LineReviewStatus? = __converters.toLineReviewStatus(_tmp_2)
          if (_tmp_3 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.LineReviewStatus', but it was NULL.")
          } else {
            _tmpReviewStatus = _tmp_3
          }
          val _tmpRejectionReason: String?
          if (_stmt.isNull(_columnIndexOfRejectionReason)) {
            _tmpRejectionReason = null
          } else {
            _tmpRejectionReason = _stmt.getText(_columnIndexOfRejectionReason)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item_1 = PurchaseLineEntity(_tmpId,_tmpInvoiceId,_tmpLineNumber,_tmpVariantId,_tmpSupplierItemCode,_tmpRawDescription,_tmpCorrectedDescription,_tmpHsnCode,_tmpRawQuantity,_tmpCorrectedQuantity,_tmpRawUnit,_tmpInventoryUnit,_tmpUnitConversionFactor,_tmpEffectiveQuantityBase,_tmpRawUnitPricePaise,_tmpCorrectedUnitPricePaise,_tmpRawLineAmountPaise,_tmpCorrectedLineAmountPaise,_tmpGstRatePercent,_tmpReviewStatus,_tmpRejectionReason,_tmpCreatedAt)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
