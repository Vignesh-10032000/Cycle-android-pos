package dev.vignesh.cycleshop.`data`.local.dao

import androidx.collection.LongSparseArray
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import dev.vignesh.cycleshop.`data`.local.Converters
import dev.vignesh.cycleshop.`data`.local.entity.BrandEntity
import dev.vignesh.cycleshop.`data`.local.entity.ProductEntity
import dev.vignesh.cycleshop.`data`.local.entity.ProductVariantEntity
import dev.vignesh.cycleshop.`data`.local.model.ProductType
import dev.vignesh.cycleshop.`data`.local.model.ProductWithVariants
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class ProductDao_Impl(
  __db: RoomDatabase,
) : ProductDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBrandEntity: EntityInsertAdapter<BrandEntity>

  private val __insertAdapterOfProductEntity: EntityInsertAdapter<ProductEntity>

  private val __converters: Converters = Converters()

  private val __insertAdapterOfProductVariantEntity: EntityInsertAdapter<ProductVariantEntity>

  private val __deleteAdapterOfBrandEntity: EntityDeleteOrUpdateAdapter<BrandEntity>

  private val __deleteAdapterOfProductEntity: EntityDeleteOrUpdateAdapter<ProductEntity>

  private val __deleteAdapterOfProductVariantEntity:
      EntityDeleteOrUpdateAdapter<ProductVariantEntity>

  private val __updateAdapterOfProductEntity: EntityDeleteOrUpdateAdapter<ProductEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBrandEntity = object : EntityInsertAdapter<BrandEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `brands` (`id`,`name`) VALUES (nullif(?, 0),?)"

      protected override fun bind(statement: SQLiteStatement, entity: BrandEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
      }
    }
    this.__insertAdapterOfProductEntity = object : EntityInsertAdapter<ProductEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `products` (`id`,`brand_id`,`name`,`category`,`type`,`is_active`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProductEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.brandId)
        statement.bindText(3, entity.name)
        statement.bindText(4, entity.category)
        val _tmp: String? = __converters.fromProductType(entity.type)
        if (_tmp == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmp)
        }
        val _tmp_1: Int = if (entity.isActive) 1 else 0
        statement.bindLong(6, _tmp_1.toLong())
        statement.bindLong(7, entity.createdAt)
      }
    }
    this.__insertAdapterOfProductVariantEntity = object : EntityInsertAdapter<ProductVariantEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `product_variants` (`id`,`product_id`,`sku`,`supplier_code`,`size`,`color`,`mrp_paise`,`cost_price_paise`,`selling_price_paise`,`stock_quantity`,`specs_json`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ProductVariantEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.productId)
        statement.bindText(3, entity.sku)
        val _tmpSupplierCode: String? = entity.supplierCode
        if (_tmpSupplierCode == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpSupplierCode)
        }
        val _tmpSize: String? = entity.size
        if (_tmpSize == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpSize)
        }
        val _tmpColor: String? = entity.color
        if (_tmpColor == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpColor)
        }
        statement.bindLong(7, entity.mrpPaise)
        statement.bindLong(8, entity.costPricePaise)
        statement.bindLong(9, entity.sellingPricePaise)
        statement.bindLong(10, entity.stockQuantity.toLong())
        val _tmpSpecsJson: String? = entity.specsJson
        if (_tmpSpecsJson == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpSpecsJson)
        }
        statement.bindLong(12, entity.createdAt)
      }
    }
    this.__deleteAdapterOfBrandEntity = object : EntityDeleteOrUpdateAdapter<BrandEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `brands` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BrandEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__deleteAdapterOfProductEntity = object : EntityDeleteOrUpdateAdapter<ProductEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `products` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ProductEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__deleteAdapterOfProductVariantEntity = object : EntityDeleteOrUpdateAdapter<ProductVariantEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `product_variants` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ProductVariantEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfProductEntity = object : EntityDeleteOrUpdateAdapter<ProductEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `products` SET `id` = ?,`brand_id` = ?,`name` = ?,`category` = ?,`type` = ?,`is_active` = ?,`created_at` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ProductEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.brandId)
        statement.bindText(3, entity.name)
        statement.bindText(4, entity.category)
        val _tmp: String? = __converters.fromProductType(entity.type)
        if (_tmp == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmp)
        }
        val _tmp_1: Int = if (entity.isActive) 1 else 0
        statement.bindLong(6, _tmp_1.toLong())
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.id)
      }
    }
  }

  public override suspend fun insertBrand(brand: BrandEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfBrandEntity.insertAndReturnId(_connection, brand)
    _result
  }

  public override suspend fun insertProduct(product: ProductEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfProductEntity.insertAndReturnId(_connection, product)
    _result
  }

  public override suspend fun insertVariant(variant: ProductVariantEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfProductVariantEntity.insertAndReturnId(_connection, variant)
    _result
  }

  public override suspend fun deleteBrand(brand: BrandEntity): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfBrandEntity.handle(_connection, brand)
    _result
  }

  public override suspend fun deleteProduct(product: ProductEntity): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfProductEntity.handle(_connection, product)
    _result
  }

  public override suspend fun deleteVariant(variant: ProductVariantEntity): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __deleteAdapterOfProductVariantEntity.handle(_connection, variant)
    _result
  }

  public override suspend fun updateProduct(product: ProductEntity): Int = performSuspending(__db, false, true) { _connection ->
    var _result: Int = 0
    _result += __updateAdapterOfProductEntity.handle(_connection, product)
    _result
  }

  public override fun getAllBrands(): Flow<List<BrandEntity>> {
    val _sql: String = "SELECT * FROM brands ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("brands")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _result: MutableList<BrandEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BrandEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          _item = BrandEntity(_tmpId,_tmpName)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBrandById(id: Long): BrandEntity? {
    val _sql: String = "SELECT * FROM brands WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _result: BrandEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          _result = BrandEntity(_tmpId,_tmpName)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBrandByName(name: String): BrandEntity? {
    val _sql: String = "SELECT * FROM brands WHERE name = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, name)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _result: BrandEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          _result = BrandEntity(_tmpId,_tmpName)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getActiveProducts(): Flow<List<ProductEntity>> {
    val _sql: String = "SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("products")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<ProductEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProductEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_1: ProductType? = __converters.toProductType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_1
          }
          val _tmpIsActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_2 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllProducts(): Flow<List<ProductEntity>> {
    val _sql: String = "SELECT * FROM products ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("products")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<ProductEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProductEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_1: ProductType? = __converters.toProductType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_1
          }
          val _tmpIsActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_2 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getProductById(id: Long): ProductEntity? {
    val _sql: String = "SELECT * FROM products WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: ProductEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_1: ProductType? = __converters.toProductType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_1
          }
          val _tmpIsActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_2 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _result = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getProductsByBrand(brandId: Long): Flow<List<ProductEntity>> {
    val _sql: String = "SELECT * FROM products WHERE brand_id = ? AND is_active = 1 ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("products")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, brandId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<ProductEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProductEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_1: ProductType? = __converters.toProductType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_1
          }
          val _tmpIsActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_2 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getProductsByType(type: ProductType): Flow<List<ProductEntity>> {
    val _sql: String = "SELECT * FROM products WHERE type = ? AND is_active = 1 ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("products")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        val _tmp: String? = __converters.fromProductType(type)
        if (_tmp == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, _tmp)
        }
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<ProductEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProductEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp_1: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp_1 = null
          } else {
            _tmp_1 = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_2: ProductType? = __converters.toProductType(_tmp_1)
          if (_tmp_2 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_2
          }
          val _tmpIsActive: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_3 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getProductsByCategory(category: String): Flow<List<ProductEntity>> {
    val _sql: String = "SELECT * FROM products WHERE category = ? AND is_active = 1 ORDER BY name ASC"
    return createFlow(__db, false, arrayOf("products")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _result: MutableList<ProductEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProductEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_1: ProductType? = __converters.toProductType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_1
          }
          val _tmpIsActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_2 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getVariantById(id: Long): ProductVariantEntity? {
    val _sql: String = "SELECT * FROM product_variants WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
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

  public override suspend fun getVariantBySku(sku: String): ProductVariantEntity? {
    val _sql: String = "SELECT * FROM product_variants WHERE sku = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sku)
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

  public override suspend fun getVariantsBySupplierCode(supplierCode: String): List<ProductVariantEntity> {
    val _sql: String = "SELECT * FROM product_variants WHERE supplier_code = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, supplierCode)
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

  public override fun getVariantsForProduct(productId: Long): Flow<List<ProductVariantEntity>> {
    val _sql: String = "SELECT * FROM product_variants WHERE product_id = ? ORDER BY id ASC"
    return createFlow(__db, false, arrayOf("product_variants")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, productId)
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

  public override fun getProductWithVariants(productId: Long): Flow<ProductWithVariants?> {
    val _sql: String = "SELECT * FROM products WHERE id = ?"
    return createFlow(__db, true, arrayOf("product_variants", "products")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, productId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _collectionVariants: LongSparseArray<MutableList<ProductVariantEntity>> = LongSparseArray<MutableList<ProductVariantEntity>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionVariants.containsKey(_tmpKey)) {
            _collectionVariants.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipproductVariantsAsdevVigneshCycleshopDataLocalEntityProductVariantEntity(_connection, _collectionVariants)
        val _result: ProductWithVariants?
        if (_stmt.step()) {
          val _tmpProduct: ProductEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_1: ProductType? = __converters.toProductType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_1
          }
          val _tmpIsActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_2 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _tmpProduct = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
          val _tmpVariantsCollection: MutableList<ProductVariantEntity>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpVariantsCollection = checkNotNull(_collectionVariants.get(_tmpKey_1))
          _result = ProductWithVariants(_tmpProduct,_tmpVariantsCollection)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getActiveProductsWithVariants(): Flow<List<ProductWithVariants>> {
    val _sql: String = "SELECT * FROM products WHERE is_active = 1 ORDER BY name ASC"
    return createFlow(__db, true, arrayOf("product_variants", "products")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfBrandId: Int = getColumnIndexOrThrow(_stmt, "brand_id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfIsActive: Int = getColumnIndexOrThrow(_stmt, "is_active")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "created_at")
        val _collectionVariants: LongSparseArray<MutableList<ProductVariantEntity>> = LongSparseArray<MutableList<ProductVariantEntity>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionVariants.containsKey(_tmpKey)) {
            _collectionVariants.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipproductVariantsAsdevVigneshCycleshopDataLocalEntityProductVariantEntity(_connection, _collectionVariants)
        val _result: MutableList<ProductWithVariants> = mutableListOf()
        while (_stmt.step()) {
          val _item: ProductWithVariants
          val _tmpProduct: ProductEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpBrandId: Long
          _tmpBrandId = _stmt.getLong(_columnIndexOfBrandId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpType: ProductType
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfType)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfType)
          }
          val _tmp_1: ProductType? = __converters.toProductType(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'dev.vignesh.cycleshop.`data`.local.model.ProductType', but it was NULL.")
          } else {
            _tmpType = _tmp_1
          }
          val _tmpIsActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsActive).toInt()
          _tmpIsActive = _tmp_2 != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _tmpProduct = ProductEntity(_tmpId,_tmpBrandId,_tmpName,_tmpCategory,_tmpType,_tmpIsActive,_tmpCreatedAt)
          val _tmpVariantsCollection: MutableList<ProductVariantEntity>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpVariantsCollection = checkNotNull(_collectionVariants.get(_tmpKey_1))
          _item = ProductWithVariants(_tmpProduct,_tmpVariantsCollection)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateVariantMetadata(
    id: Long,
    supplierCode: String?,
    size: String?,
    color: String?,
    mrpPaise: Long,
    costPricePaise: Long,
    sellingPricePaise: Long,
    specsJson: String?,
  ): Int {
    val _sql: String = """
        |
        |        UPDATE product_variants 
        |        SET supplier_code = ?,
        |            size = ?,
        |            color = ?,
        |            mrp_paise = ?,
        |            cost_price_paise = ?,
        |            selling_price_paise = ?,
        |            specs_json = ?
        |        WHERE id = ?
        |    
        """.trimMargin()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        if (supplierCode == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, supplierCode)
        }
        _argIndex = 2
        if (size == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, size)
        }
        _argIndex = 3
        if (color == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, color)
        }
        _argIndex = 4
        _stmt.bindLong(_argIndex, mrpPaise)
        _argIndex = 5
        _stmt.bindLong(_argIndex, costPricePaise)
        _argIndex = 6
        _stmt.bindLong(_argIndex, sellingPricePaise)
        _argIndex = 7
        if (specsJson == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, specsJson)
        }
        _argIndex = 8
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipproductVariantsAsdevVigneshCycleshopDataLocalEntityProductVariantEntity(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<ProductVariantEntity>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshipproductVariantsAsdevVigneshCycleshopDataLocalEntityProductVariantEntity(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`product_id`,`sku`,`supplier_code`,`size`,`color`,`mrp_paise`,`cost_price_paise`,`selling_price_paise`,`stock_quantity`,`specs_json`,`created_at` FROM `product_variants` WHERE `product_id` IN (")
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
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "product_id")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfProductId: Int = 1
      val _columnIndexOfSku: Int = 2
      val _columnIndexOfSupplierCode: Int = 3
      val _columnIndexOfSize: Int = 4
      val _columnIndexOfColor: Int = 5
      val _columnIndexOfMrpPaise: Int = 6
      val _columnIndexOfCostPricePaise: Int = 7
      val _columnIndexOfSellingPricePaise: Int = 8
      val _columnIndexOfStockQuantity: Int = 9
      val _columnIndexOfSpecsJson: Int = 10
      val _columnIndexOfCreatedAt: Int = 11
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<ProductVariantEntity>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: ProductVariantEntity
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
          _item_1 = ProductVariantEntity(_tmpId,_tmpProductId,_tmpSku,_tmpSupplierCode,_tmpSize,_tmpColor,_tmpMrpPaise,_tmpCostPricePaise,_tmpSellingPricePaise,_tmpStockQuantity,_tmpSpecsJson,_tmpCreatedAt)
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
