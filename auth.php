<?php
declare(strict_types=1);

/** POST /api/auth/register */
function api_auth_register(): void
{
    api_rate_limit('register', 10, 300);
    $in = api_input();
    $name = api_body_str($in, 'name');
    $email = strtolower((string)api_body_str($in, 'email'));
    $phone = api_body_str($in, 'phone');
    $password = (string)($in['password'] ?? '');
    $role = api_body_str($in, 'role', 'customer');
    $countryCode = strtoupper((string)api_body_str($in, 'country', 'ZM'));

    if (!$name || !$email || strlen($password) < 6) {
        api_error('Name, a valid email and a password of at least 6 characters are required');
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        api_error('Invalid email address');
    }
    if (!in_array($role, ['customer', 'driver', 'seller'], true)) {
        api_error('Invalid role. Use customer, driver or seller.');
    }

    $db = db();
    $exists = $db->prepare('SELECT id FROM users WHERE email = ?');
    $exists->execute([$email]);
    if ($exists->fetch()) api_error('An account with that email already exists', 409);

    $countryStmt = $db->prepare('SELECT id FROM countries WHERE code = ?');
    $countryStmt->execute([$countryCode]);
    $country = $countryStmt->fetch();
    $countryId = $country['id'] ?? null;

    $db->beginTransaction();
    try {
        $hash = password_hash($password, PASSWORD_BCRYPT);
        $db->prepare('INSERT INTO users (country_id, name, email, phone, password_hash, status) VALUES (?, ?, ?, ?, ?, "active")')
            ->execute([$countryId, $name, $email, $phone, $hash]);
        $userId = (int)$db->lastInsertId();

        $db->prepare('INSERT INTO user_roles (user_id, role) VALUES (?, ?)')->execute([$userId, $role]);

        if ($role === 'driver') {
            $db->prepare('INSERT INTO drivers (user_id, verification_status) VALUES (?, "pending")')->execute([$userId]);
        } elseif ($role === 'seller') {
            $db->prepare('INSERT INTO sellers (user_id, business_name, seller_type, verified) VALUES (?, ?, "individual", 0)')
                ->execute([$userId, $name . "'s Store"]);
        }

        $db->commit();
    } catch (Throwable $e) {
        $db->rollBack();
        api_error('Registration failed. Please try again.', 500);
    }

    audit_log('api.register', 'users', $userId);
    api_issue_session($userId, 'register');
}

/** POST /api/auth/login */
function api_auth_login(): void
{
    api_rate_limit('login', 20, 300);
    $in = api_input();
    $email = strtolower((string)api_body_str($in, 'email'));
    $password = (string)($in['password'] ?? '');
    if (!$email || !$password) api_error('Email and password are required');

    $stmt = db()->prepare('SELECT id, password_hash, status FROM users WHERE email = ?');
    $stmt->execute([$email]);
    $user = $stmt->fetch();

    if (!$user || !password_verify($password, $user['password_hash'])) {
        audit_log('api.login_failed', 'users', $user['id'] ?? null);
        api_error('Invalid email or password', 401);
    }
    if (($user['status'] ?? 'active') !== 'active') {
        api_error('This account is not active. Contact support.', 403);
    }

    audit_log('api.login', 'users', (int)$user['id']);
    api_issue_session((int)$user['id'], 'login');
}

/** Issue a fresh bearer token for the given user and return it with the profile. */
function api_issue_session(int $userId, string $deviceLabel): void
{
    $token = bin2hex(random_bytes(32));
    $hash = hash('sha256', $token);
    $expires = (new DateTime('+180 days'))->format('Y-m-d H:i:s');

    db()->prepare('INSERT INTO api_tokens (user_id, token_hash, device_label, expires_at) VALUES (?, ?, ?, ?)')
        ->execute([$userId, $hash, $deviceLabel . ':' . ($_SERVER['HTTP_USER_AGENT'] ?? 'android'), $expires]);

    api_ok([
        'token' => $token,
        'expires_at' => $expires,
        'user' => api_load_profile($userId),
    ], 201);
}

/** POST /api/auth/logout */
function api_auth_logout(): void
{
    $token = api_bearer_token();
    if (!$token) api_error('Authentication required', 401);
    $hash = hash('sha256', $token);
    db()->prepare('UPDATE api_tokens SET revoked_at = NOW() WHERE token_hash = ?')->execute([$hash]);
    api_ok(['message' => 'Logged out']);
}

/** GET /api/auth/me */
function api_auth_me(): void
{
    $user = api_require_auth();
    api_ok(api_load_profile($user['id']));
}

function api_load_profile(int $userId): array
{
    $stmt = db()->prepare('SELECT id, name, email, phone, status, created_at FROM users WHERE id = ?');
    $stmt->execute([$userId]);
    $user = $stmt->fetch();
    if (!$user) api_error('User not found', 404);

    $roleStmt = db()->prepare('SELECT role FROM user_roles WHERE user_id = ?');
    $roleStmt->execute([$userId]);
    $user['roles'] = array_column($roleStmt->fetchAll(), 'role');
    $user['id'] = (int)$user['id'];

    $driverStmt = db()->prepare('SELECT id, verification_status, rating FROM drivers WHERE user_id = ? LIMIT 1');
    $driverStmt->execute([$userId]);
    $driver = $driverStmt->fetch();
    if ($driver) $user['driver_profile'] = $driver;

    return $user;
}
