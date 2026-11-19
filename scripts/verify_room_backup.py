"""Validate an ADB tar snapshot and materialize a standalone Room database."""

import argparse
from contextlib import closing
import hashlib
import json
from pathlib import Path, PurePosixPath
import sqlite3
import tarfile
import tempfile


DATABASE = "databases/vgl_cycleshop.db"
SIDECARS = (DATABASE + "-wal", DATABASE + "-journal")
IMAGE_DIRS = ("files/invoices", "cache/invoices")
APP_TABLES = (
    "brands", "products", "product_variants", "stock_movements",
    "purchase_invoices", "purchase_lines", "supplier_product_mappings",
)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_snapshot(archive_path, scratch):
    images = {}
    database_files = set()
    with tarfile.open(archive_path, "r:") as archive:
        for entry in archive:
            path = PurePosixPath(entry.name)
            name = path.as_posix()
            is_image = any(name.startswith(prefix + "/") for prefix in IMAGE_DIRS)
            if path.is_absolute() or ".." in path.parts or "\\" in name or ":" in name:
                raise ValueError("Unsafe archive path")
            if entry.isdir() and (name in IMAGE_DIRS or is_image):
                continue
            if not entry.isfile() or (name not in (DATABASE, *SIDECARS) and not is_image):
                raise ValueError(f"Unexpected archive entry {name}")
            if name in database_files or name in images:
                raise ValueError(f"Duplicate archive entry {name}")
            with archive.extractfile(entry) as source:
                data = source.read()
            if is_image:
                images[name] = digest(data)
            else:
                (scratch / path.name).write_bytes(data)
                database_files.add(name)
    if DATABASE not in database_files:
        raise ValueError("Archive is missing the Room database")
    return images


def summarize(database):
    integrity = [row[0] for row in database.execute("PRAGMA integrity_check")]
    foreign_key_errors = database.execute("PRAGMA foreign_key_check").fetchall()
    if integrity != ["ok"] or foreign_key_errors:
        raise ValueError("SQLite integrity or foreign key validation failed")
    tables = {
        row[0] for row in database.execute("SELECT name FROM sqlite_schema WHERE type = 'table'")
    }
    balances = database.execute("""
        SELECT v.sku, v.stock_quantity, COALESCE(SUM(m.delta), 0), COUNT(m.id)
        FROM product_variants v LEFT JOIN stock_movements m ON m.variant_id = v.id
        GROUP BY v.id ORDER BY v.sku
    """).fetchall()
    return {
        "schema_version": database.execute("PRAGMA user_version").fetchone()[0],
        "room_identity_hash": database.execute(
            "SELECT identity_hash FROM room_master_table WHERE id = 42"
        ).fetchone()[0],
        "integrity_check": "ok",
        "foreign_key_violations": 0,
        "logical_sha256": digest("\n".join(database.iterdump()).encode()),
        "row_counts": {
            table: database.execute(f'SELECT COUNT(*) FROM "{table}"').fetchone()[0]
            for table in APP_TABLES if table in tables
        },
        "stock_total": sum(row[1] for row in balances),
        "ledger_total": sum(row[2] for row in balances),
        "variant_balances": [
            dict(sku=sku, stock=stock, ledger_delta=delta, movements=count)
            for sku, stock, delta, count in balances
        ],
        "invoice_image_reference_count": database.execute(
            "SELECT COUNT(*) FROM purchase_invoices WHERE image_uri IS NOT NULL"
        ).fetchone()[0] if "purchase_invoices" in tables else 0,
    }


def verify(archive, restored, schema_export):
    if restored.exists():
        raise FileExistsError(restored)
    with tempfile.TemporaryDirectory() as tmp:
        scratch = Path(tmp)
        images = read_snapshot(archive, scratch)
        source_path = scratch / PurePosixPath(DATABASE).name
        with closing(sqlite3.connect(source_path, autocommit=True)) as source:
            expected = summarize(source)
            if schema_export is not None:
                exported = json.loads(schema_export.read_text(encoding="utf-8"))["database"]
                if (expected["schema_version"], expected["room_identity_hash"]) != (
                    exported["version"], exported["identityHash"],
                ):
                    raise ValueError("Snapshot differs from the selected Room schema export")
            with restored.open("xb"):
                pass
            with closing(sqlite3.connect(restored, autocommit=True)) as destination:
                source.backup(destination)
                if destination.execute("PRAGMA journal_mode=DELETE").fetchone()[0] != "delete":
                    raise ValueError("Could not create a standalone copy")
        uri = restored.resolve().as_uri() + "?mode=ro"
        with closing(sqlite3.connect(uri, uri=True)) as destination:
            actual = summarize(destination)
    if expected != actual:
        raise ValueError("Restored database differs from source snapshot")
    return actual | {
        "snapshot_sha256": digest(archive.read_bytes()),
        "restored_sha256": digest(restored.read_bytes()),
        "invoice_image_count": len(images),
        "invoice_images": images,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshot", type=Path)
    parser.add_argument("restored", type=Path, help="New output file on the host")
    parser.add_argument("--schema", type=Path, help="Expected Room JSON schema export")
    args = parser.parse_args()
    print(json.dumps(verify(args.snapshot, args.restored, args.schema), indent=2))


if __name__ == "__main__":
    main()
