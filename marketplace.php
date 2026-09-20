<?php
declare(strict_types=1);

/** GET /api/categories */
function api_categories_list(): void
{
    $rows = db()->query('SELECT id, parent_id, name, slug FROM categories ORDER BY parent_id IS NULL DESC, name')->fetchAll();
    api_ok($rows);
}

/** GET /api/products?category=&search=&page=&per_page= */
function api_products_list(): void
{
    [$page, $perPage, $offset] = api_paginate();
    $where = ['p.active = 1'];
    $params = [];

    if (!empty($_GET['category'])) {
        $where[] = 'p.category_id = ?';
        $params[] = (int)$_GET['category'];
    }
    if (!empty($_GET['search'])) {
        $where[] = '(p.name LIKE ? OR p.brand LIKE ?)';
        $like = '%' . $_GET['search'] . '%';
        $params[] = $like;
        $params[] = $like;
    }
    $whereSql = implode(' AND ', $where);

    $countStmt = db()->prepare("SELECT COUNT(*) c FROM products p WHERE {$whereSql}");
    $countStmt->execute($params);
    $total = (int)$countStmt->fetch()['c'];

    $sql = "SELECT p.id, p.name, p.slug, p.brand, p.price, p.sale_price, p.category_id,
                   (SELECT url FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.sort_order LIMIT 1) AS image_url,
                   (SELECT ROUND(AVG(r.rating),1) FROM reviews r WHERE r.product_id = p.id) AS rating
            FROM products p
            WHERE {$whereSql}
            ORDER BY p.created_at DESC
            LIMIT {$perPage} OFFSET {$offset}";
    $stmt = db()->prepare($sql);
    $stmt->execute($params);

    api_ok([
        'items' => $stmt->fetchAll(),
        'page' => $page,
        'per_page' => $perPage,
        'total' => $total,
    ]);
}

/** GET /api/products/{id} */
function api_products_show(int $id): void
{
    $stmt = db()->prepare(
        'SELECT p.*, s.business_name AS seller_name
         FROM products p JOIN sellers s ON s.id = p.seller_id
         WHERE p.id = ? AND p.active = 1'
    );
    $stmt->execute([$id]);
    $product = $stmt->fetch();
    if (!$product) api_error('Product not found', 404);

    $imgStmt = db()->prepare('SELECT url FROM product_images WHERE product_id = ? ORDER BY sort_order');
    $imgStmt->execute([$id]);
    $product['images'] = array_column($imgStmt->fetchAll(), 'url');

    $specStmt = db()->prepare('SELECT spec_key, spec_value FROM product_specifications WHERE product_id = ? ORDER BY sort_order');
    $specStmt->execute([$id]);
    $product['specifications'] = $specStmt->fetchAll();

    $reviewStmt = db()->prepare(
        'SELECT r.id, r.rating, r.body, u.name AS reviewer FROM reviews r JOIN users u ON u.id = r.user_id
         WHERE r.product_id = ? ORDER BY r.id DESC LIMIT 20'
    );
    $reviewStmt->execute([$id]);
    $product['reviews'] = $reviewStmt->fetchAll();

    $user = api_current_user();
    if ($user) {
        db()->prepare('INSERT IGNORE INTO recently_viewed (user_id, product_id) VALUES (?, ?)')->execute([$user['id'], $id]);
        db()->prepare('REPLACE INTO recently_viewed (user_id, product_id, viewed_at) VALUES (?, ?, NOW())')->execute([$user['id'], $id]);
    }

    api_ok($product);
}

/** Resolve (or lazily create) the active cart for the authenticated user. */
function api_active_cart_id(int $userId): int
{
    $stmt = db()->prepare("SELECT id FROM carts WHERE user_id = ? AND status = 'active' LIMIT 1");
    $stmt->execute([$userId]);
    $cart = $stmt->fetch();
    if ($cart) return (int)$cart['id'];

    db()->prepare("INSERT INTO carts (user_id, status) VALUES (?, 'active')")->execute([$userId]);
    return (int)db()->lastInsertId();
}

function api_cart_payload(int $cartId): array
{
    $stmt = db()->prepare(
        'SELECT ci.product_id, ci.quantity, p.name, p.price, p.sale_price,
                (SELECT url FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.sort_order LIMIT 1) AS image_url
         FROM cart_items ci JOIN products p ON p.id = ci.product_id
         WHERE ci.cart_id = ?'
    );
    $stmt->execute([$cartId]);
    $items = $stmt->fetchAll();

    $subtotal = 0.0;
    foreach ($items as &$item) {
        $unit = $item['sale_price'] !== null ? (float)$item['sale_price'] : (float)$item['price'];
        $item['unit_price'] = $unit;
        $item['line_total'] = round($unit * (int)$item['quantity'], 2);
        $subtotal += $item['line_total'];
    }
    unset($item);

    return ['items' => $items, 'subtotal' => round($subtotal, 2)];
}

/** GET /api/cart */
function api_cart_show(): void
{
    $user = api_require_auth();
    api_ok(api_cart_payload(api_active_cart_id($user['id'])));
}

/** POST /api/cart/items  { product_id, quantity } */
function api_cart_add(): void
{
    $user = api_require_auth();
    $in = api_input();
    $productId = (int)($in['product_id'] ?? 0);
    $quantity = max(1, (int)($in['quantity'] ?? 1));
    if (!$productId) api_error('product_id is required');

    $exists = db()->prepare('SELECT id FROM products WHERE id = ? AND active = 1');
    $exists->execute([$productId]);
    if (!$exists->fetch()) api_error('Product not found', 404);

    $cartId = api_active_cart_id($user['id']);
    db()->prepare(
        'INSERT INTO cart_items (cart_id, product_id, quantity) VALUES (?, ?, ?)
         ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)'
    )->execute([$cartId, $productId, $quantity]);

    api_ok(api_cart_payload($cartId), 201);
}

/** PUT /api/cart/items/{productId}  { quantity } */
function api_cart_update(int $productId): void
{
    $user = api_require_auth();
    $in = api_input();
    $quantity = (int)($in['quantity'] ?? 0);
    $cartId = api_active_cart_id($user['id']);

    if ($quantity <= 0) {
        db()->prepare('DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?')->execute([$cartId, $productId]);
    } else {
        db()->prepare('UPDATE cart_items SET quantity = ? WHERE cart_id = ? AND product_id = ?')
            ->execute([$quantity, $cartId, $productId]);
    }
    api_ok(api_cart_payload($cartId));
}

/** DELETE /api/cart/items/{productId} */
function api_cart_remove(int $productId): void
{
    $user = api_require_auth();
    $cartId = api_active_cart_id($user['id']);
    db()->prepare('DELETE FROM cart_items WHERE cart_id = ? AND product_id = ?')->execute([$cartId, $productId]);
    api_ok(api_cart_payload($cartId));
}

/**
 * POST /api/orders  { address: {full_name,phone,area,street,city,region,country},
 *                      delivery_fee?, notes? }
 * Converts the active cart into an order. Payment integration is intentionally
 * left as-is (matches the wider AMOPER project decision) — order is created
 * with payment_status = 'unpaid' for the existing checkout/payment flow to pick up.
 */
function api_orders_create(): void
{
    $user = api_require_auth();
    $in = api_input();
    $cartId = api_active_cart_id($user['id']);
    $cart = api_cart_payload($cartId);
    if (empty($cart['items'])) api_error('Your cart is empty');

    $address = is_array($in['address'] ?? null) ? $in['address'] : [];
    $deliveryFee = (float)($in['delivery_fee'] ?? 0);

    $countryStmt = db()->prepare('SELECT country_id FROM users WHERE id = ?');
    $countryStmt->execute([$user['id']]);
    $countryId = $countryStmt->fetch()['country_id'] ?? 1;

    $db = db();
    $db->beginTransaction();
    try {
        $addressId = null;
        if ($address) {
            $db->prepare(
                'INSERT INTO addresses (user_id, country_id, full_name, phone, area, street, city_id, is_default)
                 VALUES (?, ?, ?, ?, ?, ?, NULL, 0)'
            )->execute([
                $user['id'], $countryId,
                $address['full_name'] ?? $user['name'], $address['phone'] ?? $user['phone'],
                $address['area'] ?? null, $address['street'] ?? null,
            ]);
            $addressId = (int)$db->lastInsertId();
        }

        $subtotal = $cart['subtotal'];
        $total = round($subtotal + $deliveryFee, 2);
        $orderNumber = 'AMO-' . date('ymd') . '-' . strtoupper(substr(bin2hex(random_bytes(3)), 0, 6));

        $db->prepare(
            'INSERT INTO orders (order_number, user_id, country_id, address_id, status, payment_status, subtotal, delivery_fee, tax, total)
             VALUES (?, ?, ?, ?, "pending", "unpaid", ?, ?, 0, ?)'
        )->execute([$orderNumber, $user['id'], $countryId, $addressId, $subtotal, $deliveryFee, $total]);
        $orderId = (int)$db->lastInsertId();

        $itemStmt = $db->prepare(
            'INSERT INTO order_items (order_id, seller_id, product_id, quantity, unit_price)
             SELECT ?, p.seller_id, p.id, ?, ? FROM products p WHERE p.id = ?'
        );
        foreach ($cart['items'] as $item) {
            $itemStmt->execute([$orderId, $item['quantity'], $item['unit_price'], $item['product_id']]);
        }

        $db->prepare('INSERT INTO order_status_history (order_id, status, note, changed_by) VALUES (?, "pending", "Order placed via Android app", ?)')
            ->execute([$orderId, $user['id']]);

        $db->prepare('DELETE FROM cart_items WHERE cart_id = ?')->execute([$cartId]);
        $db->prepare('UPDATE carts SET status = "converted" WHERE id = ?')->execute([$cartId]);

        $db->commit();
    } catch (Throwable $e) {
        $db->rollBack();
        api_error('Could not place order. Please try again.', 500);
    }

    audit_log('api.order_created', 'orders', $orderId);
    api_orders_show($orderId);
}

/** GET /api/orders */
function api_orders_list(): void
{
    $user = api_require_auth();
    [$page, $perPage, $offset] = api_paginate();
    $stmt = db()->prepare(
        "SELECT id, order_number, status, payment_status, total, created_at
         FROM orders WHERE user_id = ? ORDER BY created_at DESC LIMIT {$perPage} OFFSET {$offset}"
    );
    $stmt->execute([$user['id']]);
    api_ok(['items' => $stmt->fetchAll(), 'page' => $page, 'per_page' => $perPage]);
}

/** GET /api/orders/{id} */
function api_orders_show(int $id): void
{
    $user = api_require_auth();
    $stmt = db()->prepare('SELECT * FROM orders WHERE id = ? AND user_id = ?');
    $stmt->execute([$id, $user['id']]);
    $order = $stmt->fetch();
    if (!$order) api_error('Order not found', 404);

    $itemStmt = db()->prepare(
        'SELECT oi.product_id, oi.quantity, oi.unit_price, p.name,
                (SELECT url FROM product_images pi WHERE pi.product_id = p.id ORDER BY pi.sort_order LIMIT 1) AS image_url
         FROM order_items oi JOIN products p ON p.id = oi.product_id WHERE oi.order_id = ?'
    );
    $itemStmt->execute([$id]);
    $order['items'] = $itemStmt->fetchAll();

    $shipStmt = db()->prepare('SELECT id, tracking_number, status, estimated_date FROM shipments WHERE order_id = ?');
    $shipStmt->execute([$id]);
    $order['shipments'] = $shipStmt->fetchAll();

    api_ok($order);
}
