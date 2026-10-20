-- KEYS: challenge, hashed-email counter, hashed-IP counter.
-- Limits are fixed one-hour windows; rejected and failed deliveries do not refresh them.
if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
if tonumber(redis.call('GET', KEYS[2]) or '0') >= 5 then return 2 end
if tonumber(redis.call('GET', KEYS[3]) or '0') >= 20 then return 2 end
local now = tonumber(redis.call('TIME')[1])
redis.call('HSET', KEYS[1], 'email', ARGV[1], 'password_hash', ARGV[2],
  'verifier', ARGV[3], 'state', 'PENDING', 'failures', 0, 'sends', 1,
  'last_send', now)
redis.call('EXPIRE', KEYS[1], 30)
local e = redis.call('INCR', KEYS[2])
if e == 1 then redis.call('EXPIRE', KEYS[2], 3600) end
local ip = redis.call('INCR', KEYS[3])
if ip == 1 then redis.call('EXPIRE', KEYS[3], 3600) end
return 1
