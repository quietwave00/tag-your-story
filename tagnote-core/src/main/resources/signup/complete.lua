if redis.call('HGET', KEYS[1], 'state') == 'CLAIMED'
  and redis.call('HGET', KEYS[1], 'verifier') == ARGV[1]
  and redis.call('HGET', KEYS[1], 'owner') == ARGV[2] then
  redis.call('HSET', KEYS[1], 'state', 'DONE', 'user_id', ARGV[3])
  redis.call('HDEL', KEYS[1], 'password_hash', 'owner', 'claimed_at')
  redis.call('EXPIRE', KEYS[1], 600)
  return 1
end
return 0
