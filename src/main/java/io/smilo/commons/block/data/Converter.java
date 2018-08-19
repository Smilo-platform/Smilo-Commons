package io.smilo.commons.block.data;

public interface Converter<T1,T2> {
    T1 from(T2 t);
    T2 to(T1 t);
}
