package io.github.notsyncing.cowherd.service;

/**
 * 类实例化方式
 */
public enum ComponentInstantiateType
{
    /**
     * 单实例
     */
    Singleton,

    /**
     * 每次均创建新对象
     */
    AlwaysNew
}
