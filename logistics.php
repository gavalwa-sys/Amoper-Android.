<?php
declare(strict_types=1);

/**
 * POST /api/logistics/quotes
 * { origin, destination, delivery_speed, weight_kg, length_cm, width_cm, height_cm, package_type, description }
 * Creates a shipment placeholder + a draft quote with a simple distance/weight based estimate.
 * (A human ops user can revise the amounts later from the existing web Quote Manager.)
 */
function api_quotes_create(): void
{
    $user = api_require_auth();
    $in = api_input();
    $origin = api_body_str($in, 'origin');
    $destination = api_body_str($in, 'destination');
    $speed = api_body_str($in, 'delivery_speed', 'standard');
    $weight = (float)($in['weight_kg'] ?? 1);

    if (!$origin || !$destination) api_error('Origin and destination are required');

    $base = max(25.0, $weight * 8.5);
    $speedMultiplier = ['express' => 1.6, 'standard' => 1.0, 'economy' => 0.75][$speed] ?? 1.0;
    $baseAmount = round($base * $speedMultiplier, 2);
    $handling = round($baseAmount * 0.05, 2);
    $insurance = round($baseAmount * 0.02, 2);
    $total = round($baseAmount + $handling + $insurance, 2);
    $transitDays = ['express' => '1-2 days', 'standard' => '3-5 days', 'economy' => '6-9 days'][$speed] ?? '3-5 days';

    $db = db();
    $db->beginTransaction();
    try {
        $tracking = 'AMP' . date('ymd') . strtoupper(substr(bin2hex(random_bytes(3)), 0, 5));
        $db->prepare(
            'INSERT INTO shipments (customer_id, tracking_number, origin, destination, status, delivery_speed)
             VALUES (?, ?, ?, ?, "quote_requested", ?)'
        )->execute([$user['id'], $tracking, $origin, $destination, $speed]);
        $shipmentId = (int)$db->lastInsertId();

        if (!empty($in['package_type']) || !empty($in['weight_kg'])) {
            $db->prepare(
                'INSERT INTO cargo_packages (shipment_id, package_type, quantity, weight_kg, length_cm, width_cm, height_cm, description)
                 VALUES (?, ?, 1, ?, ?, ?, ?, ?)'
            )->execute([
                $shipmentId, $in['package_type'] ?? 'parcel', $weight,
                $in['length_cm'] ?? null, $in['width_cm'] ?? null, $in['height_cm'] ?? null,
                $in['description'] ?? null,
            ]);
        }

        $quoteNumber = 'QT' . date('ymd') . strtoupper(substr(bin2hex(random_bytes(3)), 0, 5));
        $validUntil = (new DateTime('+7 days'))->format('Y-m-d');
        $db->prepare(
            'INSERT INTO logistics_quotes (shipment_id, quote_number, currency, base_amount, handling_amount, insurance_amount, total_amount, transit_days, status, valid_until, created_by)
             VALUES (?, ?, "ZMW", ?, ?, ?, ?, ?, "sent", ?, ?)'
        )->execute([$shipmentId, $quoteNumber, $baseAmount, $handling, $insurance, $total, $transitDays, $validUntil, $user['id']]);
        $quoteId = (int)$db->lastInsertId();

        $db->prepare('INSERT INTO shipment_events (shipment_id, status, note) VALUES (?, "quote_requested", "Quote requested from Android app")')
            ->execute([$shipmentId]);

        $db->commit();
    } catch (Throwable $e) {
        $db->rollBack();
        api_error('Could not create quote. Please try again.', 500);
    }

    api_quotes_show($quoteId);
}

/** GET /api/logistics/quotes */
function api_quotes_list(): void
{
    $user = api_require_auth();
    $stmt = db()->prepare(
        'SELECT q.id, q.quote_number, q.total_amount, q.currency, q.transit_days, q.status, q.valid_until,
                s.id AS shipment_id, s.tracking_number, s.origin, s.destination
         FROM logistics_quotes q JOIN shipments s ON s.id = q.shipment_id
         WHERE s.customer_id = ? ORDER BY q.created_at DESC'
    );
    $stmt->execute([$user['id']]);
    api_ok($stmt->fetchAll());
}

/** GET /api/logistics/quotes/{id} */
function api_quotes_show(int $id): void
{
    $user = api_require_auth();
    $stmt = db()->prepare(
        'SELECT q.*, s.tracking_number, s.origin, s.destination, s.customer_id
         FROM logistics_quotes q JOIN shipments s ON s.id = q.shipment_id WHERE q.id = ?'
    );
    $stmt->execute([$id]);
    $quote = $stmt->fetch();
    if (!$quote || (int)$quote['customer_id'] !== $user['id']) api_error('Quote not found', 404);
    unset($quote['customer_id']);
    api_ok($quote);
}

/** POST /api/logistics/quotes/{id}/accept — books the shipment. */
function api_quotes_accept(int $id): void
{
    $user = api_require_auth();
    $stmt = db()->prepare(
        'SELECT q.id, q.shipment_id, s.customer_id FROM logistics_quotes q
         JOIN shipments s ON s.id = q.shipment_id WHERE q.id = ?'
    );
    $stmt->execute([$id]);
    $quote = $stmt->fetch();
    if (!$quote || (int)$quote['customer_id'] !== $user['id']) api_error('Quote not found', 404);

    $db = db();
    $db->beginTransaction();
    try {
        $db->prepare('UPDATE logistics_quotes SET status = "accepted", accepted_at = NOW() WHERE id = ?')->execute([$id]);
        $db->prepare('UPDATE shipments SET status = "booked" WHERE id = ?')->execute([$quote['shipment_id']]);
        $db->prepare('INSERT INTO shipment_events (shipment_id, status, note) VALUES (?, "booked", "Quote accepted, shipment booked")')
            ->execute([$quote['shipment_id']]);
        $db->prepare('INSERT INTO shipment_workflows (shipment_id, stage, progress, last_action, updated_by)
                      VALUES (?, "booking", 10, "Shipment booked", ?)
                      ON DUPLICATE KEY UPDATE stage = "booking", progress = 10, last_action = "Shipment booked", updated_by = VALUES(updated_by)')
            ->execute([$quote['shipment_id'], $user['id']]);
        $db->commit();
    } catch (Throwable $e) {
        $db->rollBack();
        api_error('Could not accept quote', 500);
    }

    api_shipments_show((int)$quote['shipment_id']);
}

/** GET /api/logistics/shipments */
function api_shipments_list(): void
{
    $user = api_require_auth();
    [$page, $perPage, $offset] = api_paginate();
    $stmt = db()->prepare(
        "SELECT id, tracking_number, origin, destination, status, delivery_speed, estimated_date
         FROM shipments WHERE customer_id = ? ORDER BY id DESC LIMIT {$perPage} OFFSET {$offset}"
    );
    $stmt->execute([$user['id']]);
    api_ok(['items' => $stmt->fetchAll(), 'page' => $page, 'per_page' => $perPage]);
}

/** GET /api/logistics/shipments/{id} */
function api_shipments_show(int $id): void
{
    $user = api_require_auth();
    $stmt = db()->prepare('SELECT * FROM shipments WHERE id = ?');
    $stmt->execute([$id]);
    $shipment = $stmt->fetch();
    if (!$shipment) api_error('Shipment not found', 404);

    $isOwner = (int)($shipment['customer_id'] ?? 0) === $user['id'];
    $isAssignedDriver = api_is_driver_for_shipment($user['id'], $id);
    if (!$isOwner && !$isAssignedDriver) api_error('Shipment not found', 404);

    $eventStmt = db()->prepare('SELECT status, location, note, happened_at FROM shipment_events WHERE shipment_id = ? ORDER BY happened_at DESC');
    $eventStmt->execute([$id]);
    $shipment['events'] = $eventStmt->fetchAll();

    $pkgStmt = db()->prepare('SELECT package_type, quantity, weight_kg, description FROM cargo_packages WHERE shipment_id = ?');
    $pkgStmt->execute([$id]);
    $shipment['packages'] = $pkgStmt->fetchAll();

    $locStmt = db()->prepare('SELECT latitude, longitude, recorded_at FROM driver_locations WHERE shipment_id = ? ORDER BY recorded_at DESC LIMIT 1');
    $locStmt->execute([$id]);
    $shipment['last_known_location'] = $locStmt->fetch() ?: null;

    api_ok($shipment);
}

/** GET /api/logistics/track/{trackingNumber} */
function api_shipments_track(string $trackingNumber): void
{
    $user = api_require_auth();
    $stmt = db()->prepare('SELECT id FROM shipments WHERE tracking_number = ?');
    $stmt->execute([$trackingNumber]);
    $row = $stmt->fetch();
    if (!$row) api_error('No shipment found with that tracking number', 404);
    api_shipments_show((int)$row['id']);
}

function api_is_driver_for_shipment(int $userId, int $shipmentId): bool
{
    $stmt = db()->prepare(
        'SELECT 1 FROM driver_deliveries dd JOIN drivers d ON d.id = dd.driver_id
         WHERE d.user_id = ? AND dd.shipment_id = ? LIMIT 1'
    );
    $stmt->execute([$userId, $shipmentId]);
    return (bool)$stmt->fetch();
}

/** GET /api/driver/deliveries — deliveries assigned to the authenticated driver. */
function api_driver_deliveries(): void
{
    $user = api_require_role(['driver']);
    $driverStmt = db()->prepare('SELECT id FROM drivers WHERE user_id = ?');
    $driverStmt->execute([$user['id']]);
    $driver = $driverStmt->fetch();
    if (!$driver) api_error('Driver profile not found', 404);

    $stmt = db()->prepare(
        'SELECT s.id AS shipment_id, s.tracking_number, s.origin, s.destination, s.status, s.delivery_speed,
                dd.accepted_at, dd.completed_at, dd.earnings
         FROM driver_deliveries dd JOIN shipments s ON s.id = dd.shipment_id
         WHERE dd.driver_id = ? ORDER BY (dd.completed_at IS NOT NULL), s.id DESC'
    );
    $stmt->execute([$driver['id']]);
    api_ok($stmt->fetchAll());
}

/** PUT /api/driver/deliveries/{shipmentId}/status  { status, note, location } */
function api_driver_delivery_update(int $shipmentId): void
{
    $user = api_require_role(['driver']);
    $in = api_input();
    $status = api_body_str($in, 'status');
    $allowed = ['picked_up', 'in_transit', 'out_for_delivery', 'delivered', 'delivery_failed'];
    if (!$status || !in_array($status, $allowed, true)) {
        api_error('status must be one of: ' . implode(', ', $allowed));
    }

    $driverStmt = db()->prepare('SELECT id FROM drivers WHERE user_id = ?');
    $driverStmt->execute([$user['id']]);
    $driver = $driverStmt->fetch();
    if (!$driver || !api_is_driver_for_shipment($user['id'], $shipmentId)) {
        api_error('You are not assigned to this shipment', 403);
    }

    $db = db();
    $db->beginTransaction();
    try {
        $db->prepare('UPDATE shipments SET status = ? WHERE id = ?')->execute([$status, $shipmentId]);
        $db->prepare('INSERT INTO shipment_events (shipment_id, status, location, note) VALUES (?, ?, ?, ?)')
            ->execute([$shipmentId, $status, $in['location'] ?? null, $in['note'] ?? null]);
        if ($status === 'delivered') {
            $db->prepare('UPDATE driver_deliveries SET completed_at = NOW() WHERE driver_id = ? AND shipment_id = ?')
                ->execute([$driver['id'], $shipmentId]);
        }
        $db->commit();
    } catch (Throwable $e) {
        $db->rollBack();
        api_error('Could not update delivery status', 500);
    }

    api_ok(['shipment_id' => $shipmentId, 'status' => $status]);
}

/** POST /api/driver/location  { shipment_id?, latitude, longitude } — GPS heartbeat. */
function api_driver_location_ping(): void
{
    $user = api_require_role(['driver']);
    $in = api_input();
    $lat = $in['latitude'] ?? null;
    $lng = $in['longitude'] ?? null;
    if ($lat === null || $lng === null) api_error('latitude and longitude are required');

    $driverStmt = db()->prepare('SELECT id FROM drivers WHERE user_id = ?');
    $driverStmt->execute([$user['id']]);
    $driver = $driverStmt->fetch();
    if (!$driver) api_error('Driver profile not found', 404);

    $shipmentId = !empty($in['shipment_id']) ? (int)$in['shipment_id'] : null;
    db()->prepare('INSERT INTO driver_locations (driver_id, shipment_id, latitude, longitude) VALUES (?, ?, ?, ?)')
        ->execute([$driver['id'], $shipmentId, $lat, $lng]);

    api_ok(['message' => 'Location recorded'], 201);
}

/** GET /api/notifications */
function api_notifications_list(): void
{
    $user = api_require_auth();
    $stmt = db()->prepare('SELECT id, title, body, read_at, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 50');
    $stmt->execute([$user['id']]);
    api_ok($stmt->fetchAll());
}

/** POST /api/notifications/{id}/read */
function api_notifications_read(int $id): void
{
    $user = api_require_auth();
    db()->prepare('UPDATE notifications SET read_at = NOW() WHERE id = ? AND user_id = ? AND read_at IS NULL')
        ->execute([$id, $user['id']]);
    api_ok(['message' => 'Marked as read']);
}
