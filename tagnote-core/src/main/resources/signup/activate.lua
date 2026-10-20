if redis.call('HGET', KEYS[1], 'state') ~= 'PENDING' then return 0 end
if redis.call('HGET', KEYS[1], 'verifier') ~= ARGV[1] then return 0 end
redis.call('HSET', KEYS[1], 'state', 'READY')
redis.call('EXPIRE', KEYS[1], 600)
return 1
