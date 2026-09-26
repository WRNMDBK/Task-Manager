--初始化键和变量
local keyOfBlackList = KEYS[1]
local keyOfCount = KEYS[2]
local count = tonumber(ARGV[1])
local second = tonumber(ARGV[2])
local banSecond = tonumber(ARGV[3])

--先判断是否已被限流
if redis.call('EXISTS', keyOfBlackList) == 1 then
    return false
    end

--计数器加 1，若返回值为 1 则说明是新创建的计数器，需要手动设置过期时间
local incrResult = redis.call('INCR', keyOfCount)

--设置过期时间
if incrResult == 1 or redis.call('TTL', keyOfCount) < 0 then
    redis.call('EXPIRE', keyOfCount, second)
    end

--当结果大于限度则闯进入限流黑名单
if incrResult > count then
    redis.call('SET',keyOfBlackList, '1', 'EX', banSecond)
    return false
    end

--没被限流则返回 0
return true