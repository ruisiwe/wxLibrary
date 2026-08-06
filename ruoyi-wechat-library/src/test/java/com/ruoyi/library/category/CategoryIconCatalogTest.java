package com.ruoyi.library.category;

import com.ruoyi.library.dto.CategoryIconOptionDto;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryIconCatalogTest
{
    @Test
    void loadsOrderedCuratedOptionsAndValidatesNames()
    {
        List<CategoryIconOptionDto> options = CategoryIconCatalog.listOptions();

        assertEquals(31, options.size());
        assertEquals("book", options.get(0).getName());
        assertEquals("图书", options.get(0).getLabel());
        assertTrue(CategoryIconCatalog.isSupported("time"));
        assertTrue(CategoryIconCatalog.isSupported(" time "));
        assertFalse(CategoryIconCatalog.isSupported("not-a-tdesign-icon"));
        assertFalse(CategoryIconCatalog.isSupported(null));
        assertEquals("file", CategoryIconCatalog.defaultIcon());
    }

    @Test
    void optionsAreReadOnly()
    {
        assertThrows(UnsupportedOperationException.class,
                () -> CategoryIconCatalog.listOptions().clear());
    }
}
