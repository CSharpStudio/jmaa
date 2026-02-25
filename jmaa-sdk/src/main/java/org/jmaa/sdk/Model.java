package org.jmaa.sdk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jmaa.sdk.annotation.ModelServices;
import org.jmaa.sdk.annotation.ModelUniqueConstraints;
import org.jmaa.sdk.core.BaseModel;

import java.lang.annotation.Repeatable;

/**
 * 模型基类
 * <blockquote>
 *
 * <pre>
 * 最简单的情况下，构建后的模型'继承'自平面层次结构中定义的模型
 *
 *   {@code @}Model.Meta(name="a")                            Model
 *   {@code }class A1 extends Model{ }                        / | \
 *   {@code }                                                A3 A2 A1   <- definition classes
 *   {@code @}Model.Meta(name="a", inherit="a")               \ | /
 *   {@code }class A2 extends Model{ }                          a       <- registry class: registry.get("a")
 *   {@code }                                                   |
 *   {@code @}Model.Meta(name="a", inherit="a")              Records    <- model instances, like env.get("a")
 *   {@code }class A3 extends Model{ }
 *
 * 当一个模型被inherit扩展时，它的基类被修改包含当前类和其他继承的模型类。
 * 注意，模型实际是继承自其它构建好的模型，所以被继承的模型有扩展时，都会解析到到子模型
 *
 *   {@code @}Model.Meta(name="a")
 *   {@code }class A1 extends Model{ }                         Model
 *   {@code }                                                 / / \ \
 *   {@code @}Model.Meta(name="b")                           / A2 A1 \
 *   {@code }class B1 extends Model{ }                      /   \ /   \
 *   {@code }                                              B2    a    B1
 *   {@code @}Model.Meta(name="b", inherit={"a","b"})       \    |    /
 *   {@code }class B2 extends Model{ }                       \   |   /
 *   {@code }                                                 \  |  /
 *   {@code @}Model.Meta(name="a", inherit="a")                  b
 *   {@code }class A2 extends Model{ }
 * </pre>
 *
 * </blockquote>
 *
 * @author Eric Liang
 */
public class Model extends BaseModel {
    public Model() {
        isAuto = true;
        isAbstract = false;
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Meta {
        /**
         * 模型的名称(可选)
         */
        String name() default "";

        /**
         * 模型的标题(可选)
         */
        String label() default "";

        /**
         * 授权模型的名称，权限码read按此模型判断权限，角色权限配置时与此模型一起显示
         */
        String authModel() default "";

        /**
         * 模型的描述(可选)
         */
        String description() default "";

        /**
         * 继承的模型, 支持多继承
         */
        String[] inherit() default {};

        /**
         * 继承的模型, 支持多继承，弱引用，如果模型不存在则不继承
         */
        String[] inheritIf() default {};

        /**
         * 模型映射的表名(可选)
         */
        String table() default "";

        /**
         * 查询默认排序(可选)
         */
        String order() default "";

        /**
         * 记录展示的字段名(可选)
         */
        String[] present() default {};

        /**
         * 记录展示的格式化，使用{}限定字段，如:{code}-{name}
         */
        String presentFormat() default "";

        /**
         * 是否记录访问信息(可选)
         */
        BoolState logAccess() default BoolState.None;
    }

    @Repeatable(ModelServices.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Service {
        /**
         * 服务名称
         */
        String name() default "";

        /**
         * 标题
         */
        String label() default "";

        /**
         * 授权
         */
        String auth() default "";

        /**
         * 详细说明
         */
        String description() default "";

        /**
         * 服务类型,必须是{@link org.jmaa.sdk.Service}子类
         */
        Class<?> type() default Void.class;

        /**
         * 移除的服务名称, 指定移除时，其它参数将失效，使用@all移除所有服务
         */
        String[] remove() default {};
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ServiceMethod {
        /**
         * 服务方法名称(可选)
         */
        String name() default "";

        /**
         * 标题
         */
        String label() default "";

        /**
         * 授权
         */
        String auth() default "";

        /**
         * 方法描述
         */
        String doc() default "";

        /**
         * 是否操作记录，如果是，需提供id集合，否则为空集合
         */
        boolean ids() default true;
    }

    /**
     * @author Eric Liang
     */
    @Repeatable(ModelUniqueConstraints.class)
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UniqueConstraint {
        /**
         * 唯一约束名称
         */
        String name();

        /**
         * 字段
         */
        String[] fields();

        /**
         * 提示信息(可选)
         */
        String message() default "";

        /**
         * 是否用于更新
         */
        boolean update() default true;
    }

    /**
     * @author Eric Liang
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Constrains {
        /**
         * 约束的字段
         */
        String[] value() default {};
    }

    /**
     * @author Eric Liang
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnSaved {
        /**
         * 保存的字段
         */
        String[] value() default {};
    }

    /**
     * @author Eric Liang
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnDelete {
        /**
         * 卸载时是否触发
         */
        boolean atUninstall() default false;
    }

    /**
     * @author Eric Liang
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnChange {
        /**
         * 变更的字段
         */
        String[] value() default {};
    }

    /**
     * @author Eric Liang
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ActionMethod {
        /**
         * 动作名称
         */
        String value() default "";
    }
}
