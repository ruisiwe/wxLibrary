package com.ruoyi.library.category;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.library.dto.CategoryIconOptionDto;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.StreamUtils;

/** 分类可使用的 TDesign 精选图标目录。 */
public final class CategoryIconCatalog
{
    private static final String RESOURCE = "/library/category-icons.json";
    private static final String DEFAULT_ICON = "file";
    private static final List<CategoryIconOptionDto> OPTIONS = loadOptions();
    private static final Set<String> NAMES = loadNames(OPTIONS);

    private CategoryIconCatalog()
    {
    }

    public static List<CategoryIconOptionDto> listOptions()
    {
        return OPTIONS;
    }

    public static boolean isSupported(String name)
    {
        return name != null && NAMES.contains(name.trim());
    }

    public static String defaultIcon()
    {
        return DEFAULT_ICON;
    }

    private static List<CategoryIconOptionDto> loadOptions()
    {
        try (InputStream input = CategoryIconCatalog.class.getResourceAsStream(RESOURCE))
        {
            if (input == null)
                throw new IllegalStateException("分类图标目录不存在");
            String json = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
            JSONArray values = JSON.parseArray(json);
            List<CategoryIconOptionDto> options = new ArrayList<>(values.size());
            for (Object value : values)
            {
                JSONObject item = (JSONObject) value;
                options.add(new CategoryIconOptionDto(
                        item.getString("name"),
                        item.getString("label"),
                        item.getString("keywords")));
            }
            return Collections.unmodifiableList(options);
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("分类图标目录读取失败", exception);
        }
    }

    private static Set<String> loadNames(List<CategoryIconOptionDto> options)
    {
        Set<String> names = new HashSet<>();
        for (CategoryIconOptionDto option : options)
        {
            if (!names.add(option.getName()))
                throw new IllegalStateException("分类图标名称重复：" + option.getName());
        }
        return Collections.unmodifiableSet(names);
    }
}
