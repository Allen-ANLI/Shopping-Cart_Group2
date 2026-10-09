package sg.edu.nus.iss.shoppingcart.dto;

import org.springframework.data.domain.Page;
import sg.edu.nus.iss.shoppingcart.entity.Product;

import java.util.List;

/**
 * 将商品分页结果整理为列表数据、页码、页大小和总数等响应字段。
 * @author 王重一
 */
public class ProductPageResponse {

    private final List<Product> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public ProductPageResponse(Page<Product> result) {
        this.content = result.getContent();
        this.page = result.getNumber();
        this.size = result.getSize();
        this.totalElements = result.getTotalElements();
        this.totalPages = result.getTotalPages();
    }

    public List<Product> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}