local state = redis.call('HGET', KEYS[1], 'state')
if not state then return {'EXPIRED'} end
if state ~= 'READY' then return {'PENDING'} end
-- Empty verifier is a metadata peek used only before an atomic resend reservation.
if ARGV[1] == '' then
  return {'VALID', redis.call('HGET', KEYS[1], 'email'), redis.call('HGET', KEYS[1], 'password_hash')}
end
if redis.call('HGET', KEYS[1], 'verifier') == ARGV[1] then
  return {'VALID', redis.call('HGET', KEYS[1], 'email'), redis.call('HGET', KEYS[1], 'password_hash')}
end
local failures = redis.call('HINCRBY', KEYS[1], 'failures', 1)
if failures >= 5 then redis.call('DEL', KEYS[1]) end
return {'INVALID'}
