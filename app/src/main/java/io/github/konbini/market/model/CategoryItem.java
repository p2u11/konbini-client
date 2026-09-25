package io.github.konbini.market.model;

public class CategoryItem {
    public String code;
    public String label;

    public CategoryItem() {}

    public CategoryItem(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String toString() { return label == null ? "" : label; }
}
