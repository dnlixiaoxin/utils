package com.lxx.common.utils.redis;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class RedisCheckInUtil {

    private static final String CHECK_IN_PRE_KEY = "USER_CHECK_IN_DAY_";

    private static final String CONTINUOUS_CHECK_IN_COUNT_PRE_KEY = "USER_CHECK_IN_CONTINUOUS_COUNT_";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户签到
     *
     * @param userId 用户ID
     */
    public void checkIn(Long userId) {
        String today = LocalDate.now().format(DATE_TIME_FORMATTER);
        if (isCheckIn(userId, today))
            return;
        stringRedisTemplate.opsForValue().setBit(getCheckInKey(today), userId, true);
        updateContinuousCheckIn(userId);
    }

    /**
     * 检查用户是否签到
     *
     * @param userId
     * @param date
     * @return
     */
    public boolean isCheckIn(Long userId, String date) {
        Boolean isCheckIn = stringRedisTemplate.opsForValue().getBit(getCheckInKey(date), userId);
        return Optional.ofNullable(isCheckIn).orElse(false);
    }

    /**
     * 统计特定日期签到总人数
     *
     * @param date
     * @return
     */
    public Long countDateCheckIn(String date) {
        byte[] key = getCheckInKey(date).getBytes();
        Long result = stringRedisTemplate.execute((RedisCallback<Long>) connection -> connection.bitCount(key));
        return Optional.ofNullable(result).orElse(0L);
    }

    /**
     * 获取用户某个时间段签到次数
     *
     * @param userId
     * @param startDate
     * @param endDate
     * @return
     */
    public Long countCheckIn(Long userId, LocalDate startDate, LocalDate endDate) {
        AtomicLong count = new AtomicLong(0);
        long distance = Duration.between(startDate, endDate).get(ChronoUnit.DAYS);
        if (distance < 0) {
            return count.get();
        }
        Stream.iterate(startDate, d -> d.plusDays(1)).limit(distance + 1).forEach((date) -> {
            Boolean isCheckIn = stringRedisTemplate.opsForValue().
                    getBit(getCheckInKey(date.format(DATE_TIME_FORMATTER)), userId);
            if (isCheckIn != null && isCheckIn)
                count.incrementAndGet();
        });
        return count.get();
    }

    /**
     * 更新用户连续签到天数：+1
     *
     * @param userId
     */
    public void updateContinuousCheckIn(Long userId) {
        String key = getContinuousCheckInKey(userId);
        String val = stringRedisTemplate.opsForValue().get(key);
        long count = 0;
        if (val != null) {
            count = Long.parseLong(val);
        }
        count++;
        // 现在时间
        LocalDateTime now = LocalDateTime.now();
        // 截止时间（到第三天 00:00:00）
        LocalDateTime endDateTime = LocalDateTime.now().plusDays(2).withHour(0).withMinute(0).withSecond(0);
        long between = Duration.between(endDateTime, now).abs().getSeconds();
        stringRedisTemplate.opsForValue().set(key, String.valueOf(count), between, TimeUnit.SECONDS);
    }

    /**
     * 获取用户连续签到天数
     *
     * @param userId
     * @return
     */
    public Long getContinuousCheckIn(Long userId) {
        String key = getContinuousCheckInKey(userId);
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null) {
            return 0L;
        }
        return Long.parseLong(val);
    }

    /**
     * 一段时间内用户签到list集合
     *
     * @param userId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<Integer> userCheckIn(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Integer> dateList = new LinkedList<>();
        long distance = Duration.between(startDate, endDate).get(ChronoUnit.DAYS);
        Stream.iterate(startDate, d -> d.plusDays(1)).limit(distance + 1).forEach((date) -> {
            Boolean isCheckIn = stringRedisTemplate.opsForValue().
                    getBit(getCheckInKey(date.format(DATE_TIME_FORMATTER)), userId);
            dateList.add((isCheckIn != null && isCheckIn) ? 1 : 0);
        });
        return dateList;
    }

    /**
     * 获取签到记录key
     *
     * @param date
     * @return
     */
    private String getCheckInKey(String date) {
        return CHECK_IN_PRE_KEY + date;
    }

    /**
     * 获取用户连续签到key
     *
     * @param userId
     * @return
     */
    private String getContinuousCheckInKey(Long userId) {
        return CONTINUOUS_CHECK_IN_COUNT_PRE_KEY + userId;
    }

}
