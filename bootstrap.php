<?php
declare(strict_types=1);

// Reuses the existing app bootstrap (db(), env_value(), audit_log(), etc.)
require_once __DIR__ . '/../app/bootstrap.php';

header('Content-Type: application/json; charset=utf-8');
// Adjust this to your real app origin(s) in production instead of *.
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

/** Send a JSON success response and stop. */
function api_ok(mixed $data = null, int $status = 200): never
{
    http_response_code($status);
    echo json_encode(['ok' => true, 'data' => $data], JSON_UNESCAPED_SLASHES);
    exit;
}

/** Send a JSON error response and stop. */
function api_error(string $message, int $status = 400): never
{
    http_response_code($status);
    echo json_encode(['ok' => false, 'error' => $message], JSON_UNESCAPED_SLASHES);
    exit;
}

/** Decode the JSON request body into an associative array. */
function api_input(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || $raw === '') return $_POST;
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : $_POST;
}

/** Pull a bearer token out of the Authorization header. */
function api_bearer_token(): ?string
{
    $header = $_SERVER['HTTP_AUTHORIZATION']
        ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
        ?? '';
    if ($header === '' && function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        $header = $headers['Authorization'] ?? $headers['authorization'] ?? '';
    }
    if (preg_match('/Bearer\s+(\S+)/i', $header, $m)) {
        return $m[1];
    }
    return null;
}

/**
 * Resolve the current API user from the bearer token, or null if
 * unauthenticated / token invalid / expired / revoked.
 * Returns ['id'=>..,'name'=>..,'email'=>..,'phone'=>..,'roles'=>[...]]
 */
function api_current_user(): ?array
{
    static $resolved = false;
    static $user = null;
    if ($resolved) return $user;
    $resolved = true;

    $token = api_bearer_token();
    if (!$token) return null;

    $hash = hash('sha256', $token);
    $stmt = db()->prepare(
        'SELECT t.id as token_id, u.id, u.name, u.email, u.phone, u.status
         FROM api_tokens t
         JOIN users u ON u.id = t.user_id
         WHERE t.token_hash = ?
           AND t.revoked_at IS NULL
           AND (t.expires_at IS NULL OR t.expires_at > NOW())
         LIMIT 1'
    );
    $stmt->execute([$hash]);
    $row = $stmt->fetch();
    if (!$row) return null;
    if (($row['status'] ?? 'active') !== 'active') return null;

    db()->prepare('UPDATE api_tokens SET last_used_at = NOW() WHERE id = ?')->execute([$row['token_id']]);

    $roleStmt = db()->prepare('SELECT role FROM user_roles WHERE user_id = ?');
    $roleStmt->execute([$row['id']]);
    $roles = array_column($roleStmt->fetchAll(), 'role');

    $user = [
        'id' => (int)$row['id'],
        'name' => $row['name'],
        'email' => $row['email'],
        'phone' => $row['phone'],
        'roles' => $roles,
    ];
    return $user;
}

/** Require any authenticated user; aborts the request otherwise. */
function api_require_auth(): array
{
    $user = api_current_user();
    if (!$user) api_error('Authentication required', 401);
    return $user;
}

/** Require the current user to hold at least one of the given roles. */
function api_require_role(array $roles): array
{
    $user = api_require_auth();
    if (!array_intersect($roles, $user['roles'])) {
        api_error('Insufficient permissions', 403);
    }
    return $user;
}

/** Simple per-token/IP rate limiter reusing the existing rate_limits table. */
function api_rate_limit(string $bucket, int $limit = 60, int $windowSeconds = 60): void
{
    $key = 'api:' . $bucket . ':' . ($_SERVER['REMOTE_ADDR'] ?? 'unknown');
    enforce_rate_limit($key, $limit, $windowSeconds);
}

function api_body_str(array $input, string $key, ?string $default = null): ?string
{
    $v = $input[$key] ?? $default;
    return $v === null ? null : trim((string)$v);
}

function api_paginate(): array
{
    $page = max(1, (int)($_GET['page'] ?? 1));
    $perPage = min(50, max(1, (int)($_GET['per_page'] ?? 20)));
    return [$page, $perPage, ($page - 1) * $perPage];
}
