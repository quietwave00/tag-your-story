if redis.call('HGET', KEYS[1], 'state') ~= 'READY' then return 0 end
local now = tonumber(redis.call('TIME')[1])
if now - tonumber(redis.call('HGET', KEYS[1], 'last_send')) < 60 then return 2 end
if tonumber(redis.call('HGET', KEYS[1], 'sends')) >= 5 then return 2 end
if tonumber(redis.call('GET', KEYS[2]) or '0') >= 5 then return 2 end
if tonumber(redis.call('GET', KEYS[3]) or '0') >= 20 then return 2 end
redis.call('HSET', KEYS[1], 'verifier', ARGV[1], 'state', 'PENDING',
  'failures', 0, 'last_send', now)
redis.call('HINCRBY', KEYS[1], 'sends', 1)
redis.call('EXPIRE', KEYS[1], 30)
local e = redis.call('INCR', KEYS[2])
if e == 1 then redis.call('EXPIRE', KEYS[2], 3600) end
local ip = redis.call('INCR', KEYS[3])
if ip == 1 then redis.call('EXPIRE', KEYS[3], 3600) end
return 1
