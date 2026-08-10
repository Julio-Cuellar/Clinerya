WITH supplier_material_stats AS (
    SELECT
        po.clinic_id,
        po.supplier_id,
        prl.material_id,
        COUNT(DISTINCT pr.id)::integer AS receipt_count,
        MIN(pr.created_at) AS created_at,
        MAX(pr.received_at) AS last_supplied_at
    FROM core.purchase_receipt_lines prl
    JOIN core.purchase_receipts pr ON pr.id = prl.receipt_id
    JOIN core.purchase_orders po ON po.id = pr.purchase_order_id
    GROUP BY po.clinic_id, po.supplier_id, prl.material_id
),
latest_supplier_material AS (
    SELECT DISTINCT ON (po.clinic_id, po.supplier_id, prl.material_id)
        po.clinic_id,
        po.supplier_id,
        prl.material_id,
        prl.material_name,
        pol.unit_of_measure,
        prl.unit_cost
    FROM core.purchase_receipt_lines prl
    JOIN core.purchase_receipts pr ON pr.id = prl.receipt_id
    JOIN core.purchase_orders po ON po.id = pr.purchase_order_id
    JOIN core.purchase_order_lines pol ON pol.id = prl.purchase_order_line_id
    ORDER BY po.clinic_id, po.supplier_id, prl.material_id, pr.received_at DESC, pr.id DESC
)
INSERT INTO core.supplier_materials (
    id,
    clinic_id,
    supplier_id,
    material_id,
    material_name,
    unit_of_measure,
    supplier_unit_cost,
    last_supplied_at,
    receipt_count,
    active,
    created_at,
    updated_at
)
SELECT
    MD5(stats.supplier_id::text || ':' || stats.material_id::text)::uuid,
    stats.clinic_id,
    stats.supplier_id,
    stats.material_id,
    latest.material_name,
    latest.unit_of_measure,
    latest.unit_cost,
    stats.last_supplied_at,
    stats.receipt_count,
    true,
    stats.created_at,
    stats.last_supplied_at
FROM supplier_material_stats stats
JOIN latest_supplier_material latest
    ON latest.clinic_id = stats.clinic_id
    AND latest.supplier_id = stats.supplier_id
    AND latest.material_id = stats.material_id
ON CONFLICT (supplier_id, material_id) DO UPDATE SET
    material_name = EXCLUDED.material_name,
    unit_of_measure = EXCLUDED.unit_of_measure,
    supplier_unit_cost = CASE
        WHEN core.supplier_materials.last_supplied_at IS NULL
            OR EXCLUDED.last_supplied_at >= core.supplier_materials.last_supplied_at
        THEN EXCLUDED.supplier_unit_cost
        ELSE core.supplier_materials.supplier_unit_cost
    END,
    last_supplied_at = CASE
        WHEN core.supplier_materials.last_supplied_at IS NULL
        THEN EXCLUDED.last_supplied_at
        ELSE GREATEST(core.supplier_materials.last_supplied_at, EXCLUDED.last_supplied_at)
    END,
    receipt_count = GREATEST(core.supplier_materials.receipt_count, EXCLUDED.receipt_count),
    updated_at = GREATEST(core.supplier_materials.updated_at, EXCLUDED.updated_at);
