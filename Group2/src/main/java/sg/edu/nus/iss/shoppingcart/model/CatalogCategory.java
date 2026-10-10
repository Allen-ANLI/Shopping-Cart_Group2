package sg.edu.nus.iss.shoppingcart.model;

import java.util.List;

/** Stable category slugs shared by browsing, seeding and administrator forms. */
public record CatalogCategory(String slug, String name, String nameZh) {
    public static final List<CatalogCategory> ALL = List.of(
            new CatalogCategory("computing", "Computing", "电脑与配件"),
            new CatalogCategory("typing", "Keyboards & mice", "键盘与鼠标"),
            new CatalogCategory("workspace", "Workspace", "桌面好物"),
            new CatalogCategory("audio", "Audio", "音频设备"));

    public static boolean isValid(String slug) {
        return ALL.stream().anyMatch(category -> category.slug().equals(slug));
    }
}
