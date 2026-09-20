<?php

declare(strict_types=1);

/**
 * AMOPER Mobile API front controller.
 *
 * Reached two ways (see .htaccess):
 *   /api.php?r=auth/login          (no rewrite needed)
 *   /api/auth/login                (rewritten to api/index.php?r=auth/login)
 *
 * Every response is JSON: {"ok":true,"data":...} or {"ok":false,"error":"..."}
 */

require_once __DIR__ . '/bootstrap.php';
require_once __DIR__ . '/auth.php';
require_once __DIR__ . '/marketplace.php';
require_once __DIR__ . '/logistics.php';

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$route = trim((string)($_GET['r'] ?? ''), '/');
$segments = $route === '' ? [] : explode('/', $route);

try {
    match (true) {
        // ---- Auth ----
        $method === 'POST' && $segments === ['auth', 'register'] => api_auth_register(),
        $method === 'POST' && $segments === ['auth', 'login'] => api_auth_login(),
        $method === 'POST' && $segments === ['auth', 'logout'] => api_auth_logout(),
        $method === 'GET' && $segments === ['auth', 'me'] => api_auth_me(),

        // ---- Marketplace ----
        $method === 'GET' && $segments === ['categories'] => api_categories_list(),
        $method === 'GET' && $segments === ['products'] => api_products_list(),
        $method === 'GET' && count($segments) === 2 && $segments[0] === 'products' => api_products_show((int)$segments[1]),

        $method === 'GET' && $segments === ['cart'] => api_cart_show(),
        $method === 'POST' && $segments === ['cart', 'items'] => api_cart_add(),
        $method === 'PUT' && count($segments) === 3 && $segments[0] === 'cart' && $segments[1] === 'items' => api_cart_update((int)$segments[2]),
        $method === 'DELETE' && count($segments) === 3 && $segments[0] === 'cart' && $segments[1] === 'items' => api_cart_remove((int)$segments[2]),

        $method === 'POST' && $segments === ['orders'] => api_orders_create(),
        $method === 'GET' && $segments === ['orders'] => api_orders_list(),
        $method === 'GET' && count($segments) === 2 && $segments[0] === 'orders' => api_orders_show((int)$segments[1]),

        // ---- Logistics ----
        $method === 'POST' && $segments === ['logistics', 'quotes'] => api_quotes_create(),
        $method === 'GET' && $segments === ['logistics', 'quotes'] => api_quotes_list(),
        $method === 'GET' && count($segments) === 3 && $segments[0] === 'logistics' && $segments[1] === 'quotes' => api_quotes_show((int)$segments[2]),
        $method === 'POST' && count($segments) === 4 && $segments[0] === 'logistics' && $segments[1] === 'quotes' && $segments[3] === 'accept' => api_quotes_accept((int)$segments[2]),

        $method === 'GET' && $segments === ['logistics', 'shipments'] => api_shipments_list(),
        $method === 'GET' && count($segments) === 3 && $segments[0] === 'logistics' && $segments[1] === 'shipments' => api_shipments_show((int)$segments[2]),
        $method === 'GET' && count($segments) === 3 && $segments[0] === 'logistics' && $segments[1] === 'track' => api_shipments_track((string)$segments[2]),

        // ---- Driver ----
        $method === 'GET' && $segments === ['driver', 'deliveries'] => api_driver_deliveries(),
        $method === 'PUT' && count($segments) === 4 && $segments[0] === 'driver' && $segments[1] === 'deliveries' && $segments[3] === 'status' => api_driver_delivery_update((int)$segments[2]),
        $method === 'POST' && $segments === ['driver', 'location'] => api_driver_location_ping(),

        // ---- Notifications ----
        $method === 'GET' && $segments === ['notifications'] => api_notifications_list(),
        $method === 'POST' && count($segments) === 3 && $segments[0] === 'notifications' && $segments[2] === 'read' => api_notifications_read((int)$segments[1]),

        // ---- Health check ----
        $segments === [] || $segments === ['ping'] => api_ok(['message' => 'AMOPER API is up', 'time' => date('c')]),

        default => api_error('Not found: ' . $method . ' /' . $route, 404),
    };
} catch (Throwable $e) {
    error_log('[AMOPER API] ' . $e->getMessage());
    api_error('Server error. Please try again.', 500);
}
