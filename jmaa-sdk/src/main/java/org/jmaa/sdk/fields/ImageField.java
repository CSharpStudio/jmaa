package org.jmaa.sdk.fields;

import org.jmaa.sdk.core.Constants;

/**
 * 图片
 *
 * @author Eric Liang
 */
public class ImageField extends BinaryBaseField<ImageField> {
    @Related
    Integer maxWidth;
    @Related
    Integer maxHeight;

    public ImageField() {
        type = Constants.IMAGE;
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public Integer getMaxHeight() {
        return maxHeight;
    }

    /**
     * 显示时最大高度
     *
     * @param height
     * @return
     */
    public ImageField maxHeight(Integer height) {
        args.put("maxHeight", height);
        return this;
    }

    /**
     * 显示时最大宽度
     *
     * @param width
     * @return
     */
    public ImageField maxWidth(Integer width) {
        args.put("maxWidth", width);
        return this;
    }
}
