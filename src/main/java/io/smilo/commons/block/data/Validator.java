package io.smilo.commons.block.data;

public interface Validator<T> {
    boolean isValid(T data);

    void hash(T data);

    T sign(T data, String privateKey, int index);

    boolean supports(Class<?> clazz);
}
