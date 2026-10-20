if redis.call('HGET', KEYS[1], 'state') == 'CLAIMED'
  and redis.call('HGET', KEYS[1], 'verifier') == ARGV[1]
  and redis.call('HGET', KEYS[1], 'owner') == ARGV[2] then
  redis.call('HSET', KEYS[1], 'state', 'READY')
  redis.call('HDEL', KEYS[1], 'owner', 'claimed_at')
  return 1
end
return 0
