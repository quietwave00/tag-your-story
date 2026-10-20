if tonumber(redis.call('GET', KEYS[1]) or '0') >= 10 then return 0 end
if tonumber(redis.call('GET', KEYS[2]) or '0') >= 30 then return 0 end
if tonumber(redis.call('GET', KEYS[3]) or '0') >= 5 then return 0 end
local email = redis.call('INCR', KEYS[1])
if email == 1 then redis.call('EXPIRE', KEYS[1], 900) end
local ip = redis.call('INCR', KEYS[2])
if ip == 1 then redis.call('EXPIRE', KEYS[2], 900) end
return 1
