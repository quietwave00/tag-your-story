package com.tagstory.api.domain.user.mapper;

import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Component
@Slf4j
public class UserControllerMapper {
    public <T, V> V toCommand(T client, Class<V> classOfReceiver) {
        try {
            V value = classOfReceiver.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(client, value);
            return value;
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new CustomException(ExceptionCode.CONVERSION_EXCEPTION);
        }
    }
}
