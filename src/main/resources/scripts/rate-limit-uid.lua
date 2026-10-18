--初始化键和变量
local keyOfBlackList = KEYS[1]
local keyOfCount = KEYS[2]
local count = tonumber(ARGV[1])
local windowSecond = tonumber(ARGV[2])
local banSecond = tonumber(ARGV[3])

--检查是否已被限流
if redis.call('EXISTS', keyOfBlackList) == 1 then
    return false
    end

--计数器自增 1
local result = redis.call('INCR', keyOfCount)

--若返回值为 1 或计数器 ttl 小于 0，说明是新创建的计数器，手动设置窗口长度
if result == 1 or redis.call('TTL', keyOfCount) < 0
then
    redis.call('EXPIRE', keyOfCount, windowSecond)
    end

--若返回值大于 count，则限流
if result > count
then
    redis.call('SET', keyOfBlackList, '1', 'EX', banSecond)
    return false
    end
return true