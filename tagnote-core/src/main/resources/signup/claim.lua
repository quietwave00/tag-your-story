local state = redis.call('HGET', KEYS[1], 'state')
if not state then return {'EXPIRED'} end
if state == 'DONE' then
  if redis.call('HGET', KEYS[1], 'verifier') == ARGV[1] then
    return {'COMPLETE', redis.call('HGET', KEYS[1], 'user_id')}
  end
  return {'INVALID'}
end
if state == 'CLAIMED' then
  local now = tonumber(redis.call('TIME')[1])
  if now - tonumber(redis.call('HGET', KEYS[1], 'claimed_at')) < 30 then return {'BUSY'} end
end
if state ~= 'READY' and state ~= 'CLAIMED' then return {'BUSY'} end
if redis.call('HGET', KEYS[1], 'verifier') ~= ARGV[1] then
  local failures = redis.call('HINCRBY', KEYS[1], 'failures', 1)
  if failures >= 5 then redis.call('DEL', KEYS[1]) end
  return {'INVALID'}
end
redis.call('HSET', KEYS[1], 'state', 'CLAIMED', 'claimed_at', redis.call('TIME')[1], 'owner', ARGV[2])
return {'CLAIMED', redis.call('HGET', KEYS[1], 'email'), redis.call('HGET', KEYS[1], 'password_hash')}
