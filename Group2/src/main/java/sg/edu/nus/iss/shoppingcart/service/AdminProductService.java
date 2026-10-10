package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.dto.ProductForm;
import sg.edu.nus.iss.shoppingcart.model.CatalogCategory;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.ResourceNotFoundException;
import sg.edu.nus.iss.shoppingcart.repository.AdminProductQueryRepository;

import java.math.BigDecimal;

/**
 * 管理员商品管理服务 —— E 模块。
 *
 * <p>对应作业加分项"Create a basic admin panel where administrators can
 * manage products (add, edit, or delete products)"。</p>
 *
 * <p><b>与 A 的分工（分工文档明确要求）：</b>商品浏览和顾客侧的查询由 A 负责，
 * 后台的增删改由 E 单独维护一个 Service。两者职责不重叠，
 * 所以 A 改顾客侧逻辑不会影响后台，后台改上下架规则也不会影响商品浏览页面。</p>
 *
 * <p><b>关键设计：不物理删除被订单引用的商品。</b>
 * 订单明细表上有指向 products 的外键，一旦商品被物理删除，
 * 后续任何涉及该订单的操作都会因外键约束失败。所以被引用过的商品
 * 只能下架——这既保住了历史订单的完整性，也符合分工文档的要求。</p>
 *
 * @author 蔡千一（Module E）
 * @author OpenAI Codex (new product visibility review)
 */
@Service
@Transactional
public class AdminProductService {

    /** 后台列表每页显示的商品数。 */
    public static final int PAGE_SIZE = 10;

    /** 商品查询接口。 */
    private final AdminProductQueryRepository productRepository;

    /**
     * 构造方法，注入依赖。
     *
     * @param productRepository 商品 Repository
     */
    public AdminProductService(AdminProductQueryRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 分页查询全部商品，<b>包括已下架的</b>。
     *
     * <p>后台和顾客侧看到的东西不一样是刻意的：顾客只看得到上架商品，
     * 管理员需要能看到下架商品才能把它们重新上架。按 ID 倒序，
     * 让新建的商品出现在第一页。</p>
     *
     * @param page 页码，从 0 开始
     * @return 分页商品，永不为 null
     */
    @Transactional(readOnly = true)
    public Page<Product> findAllForAdmin(int page) {
        int safePage = Math.max(page, 0);
        return productRepository.findAll(
                PageRequest.of(safePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));
    }

    /**
     * 按 ID 查商品，不过滤上下架状态。
     *
     * @param id 商品 ID
     * @return 商品
     * @throws ResourceNotFoundException 商品不存在时抛出
     */
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        if (id == null || id <= 0) {
            throw ResourceNotFoundException.of("Product", id);
        }
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    /**
     * 新增商品，默认上架。
     *
     * <p>入库前再做一次名称和价格的业务校验。Bean Validation 已经在
     * Controller 层拦过一遍，但Service 是可能被别处调用的入口，
     * 校验不能只靠上层。这里是纵深防御，不是重复劳动。</p>
     *
     * @param name        商品名称
     * @param description 商品描述，可为 null
     * @param price       商品单价
     * @param imageUrl    图片地址，可为 null
     * @return 保存后的商品
     */
    public Product create(String name, String description,
                          BigDecimal price, String imageUrl) {
        return create(name, description, price, imageUrl, true);
    }

    /** Save the visibility chosen on the new-product form in the same transaction. */
    public Product create(String name, String description,
                          BigDecimal price, String imageUrl, boolean active) {
        requireValidName(name);
        requireValidPrice(price);

        Product product = new Product();
        product.setName(name.trim());
        product.setDescription(trimToNull(description));
        product.setPrice(price);
        product.setImageUrl(trimToNull(imageUrl));
        product.setActive(active);

        return productRepository.save(product);
    }

    public Product create(ProductForm form) {
        requireMetadata(form);
        Product product = create(form.getName(), form.getDescription(), form.getPrice(), form.getImageUrl(), form.isActive());
        applyMetadata(product, form);
        return productRepository.save(product);
    }

    public Product update(Long id, ProductForm form) {
        requireMetadata(form);
        Product product = update(id, form.getName(), form.getDescription(), form.getPrice(), form.getImageUrl(), form.isActive());
        applyMetadata(product, form);
        return productRepository.save(product);
    }

    private void requireMetadata(ProductForm form) {
        if (form.getDiscountPercent() < 0 || form.getDiscountPercent() > 99) throw new BusinessException("Discount must be between 0 and 99 percent");
        if (form.getStockQuantity() < 0 || form.getStockQuantity() > 1000000) throw new BusinessException("Stock must be between 0 and 1000000");
        if (!CatalogCategory.isValid(form.getCategory())) throw new BusinessException("Select a valid product category");
        checkLength(form.getBrand(), 80); checkLength(form.getOrigin(), 100);
        checkLength(form.getOriginZh(), 100); checkLength(form.getNameZh(), 100);
        checkLength(form.getDescriptionZh(), 1000);
    }

    private void checkLength(String value, int limit) {
        if (value != null && value.length() > limit) throw new BusinessException("Product information is too long");
    }

    private void applyMetadata(Product product, ProductForm form) {
        product.setDiscountPercent(form.getDiscountPercent());
        product.setStockQuantity(form.getStockQuantity());
        product.setHideWhenOutOfStock(form.isHideWhenOutOfStock());
        product.setCategory(form.getCategory()); product.setBrand(trimToNull(form.getBrand()));
        product.setOrigin(trimToNull(form.getOrigin())); product.setOriginZh(trimToNull(form.getOriginZh()));
        product.setNameZh(trimToNull(form.getNameZh())); product.setDescriptionZh(trimToNull(form.getDescriptionZh()));
    }

    /**
     * 编辑商品。
     *
     * <p><b>为什么改价不会影响历史订单：</b>历史订单读的是
     * {@link sg.edu.nus.iss.shoppingcart.entity.OrderItem} 里的
     * 名称和单价快照，不是这个表。所以这里改完价，
     * 旧订单显示的金额纹丝不动。</p>
     *
     * @param id          商品 ID
     * @param name        商品名称
     * @param description 商品描述
     * @param price       商品单价
     * @param imageUrl    图片地址
     * @param active      是否上架
     * @return 更新后的商品
     */
    public Product update(Long id, String name, String description,
                          BigDecimal price, String imageUrl, boolean active) {
        requireValidName(name);
        requireValidPrice(price);

        Product product = findForUpdate(id);
        product.setName(name.trim());
        product.setDescription(trimToNull(description));
        product.setPrice(price);
        product.setImageUrl(trimToNull(imageUrl));
        product.setActive(active);

        return productRepository.save(product);
    }

    /**
     * 上架 / 下架切换。
     *
     * <p>这是推荐的"移除商品"方式：下架后顾客侧看不到也买不到，
     * 但历史订单因为用的是成交快照，依然能完整显示。</p>
     *
     * @param id 商品 ID
     * @return 切换后的商品
     */
    public Product toggleActive(Long id) {
        Product product = findForUpdate(id);
        product.setActive(!product.isActive());
        return productRepository.save(product);
    }

    /**
     * 物理删除商品——<b>仅限从未被任何订单引用过的商品</b>。
     *
     * <p>分工文档写的是"优先下架，不物理删除被订单引用的商品"。
     * 所以这里先查引用情况：被引用过就拒绝，让管理员改用下架；
     * 从没被买过的商品（比如误上架的测试数据）可以直接删掉。</p>
     *
     * @param id 商品 ID
     * @throws BusinessException 商品已被订单引用时抛出
     */
    public void deleteIfUnreferenced(Long id) {
        Product product = findForUpdate(id);

        if (productRepository.isReferencedByAnyOrder(id) || productRepository.isReferencedByAnyReview(id)) {
            throw new BusinessException(
                    "'" + product.getName() + "' appears in existing orders or reviews and cannot be deleted. "
                            + "Please hide (deactivate) it instead.");
        }

        productRepository.delete(product);
    }

    /**
     * 统计商品总数，供后台概览。
     *
     * @return 商品数量
     */
    @Transactional(readOnly = true)
    public long countAll() {
        return productRepository.count();
    }

    /**
     * 统计已下架商品数，供后台概览。
     *
     * @return 下架商品数量
     */
    @Transactional(readOnly = true)
    public long countInactive() {
        return productRepository.findAll()
                .stream()
                .filter(product -> !product.isVisible())
                .count();
    }

    // ---------- 内部校验 ----------

    private Product findForUpdate(Long id) {
        return productRepository.findForUpdate(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    /**
     * 商品名称不能为空，也不能超长。
     *
     * <p>trim 之后再判空，这样全空格的输入也会被拒。</p>
     */
    private void requireValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("Product name is required");
        }
        if (name.trim().length() > 100) {
            throw new BusinessException("Product name must not exceed 100 characters");
        }
    }

    /**
     * 价格必须大于零，且小数位不超过两位。
     *
     * <p>上界对应数据库 {@code DECIMAL(12,2)}，避免超大数字入库时
     * 抛 SQL 异常而不是友好的业务提示。</p>
     */
    private void requireValidPrice(BigDecimal price) {
        if (price == null) {
            throw new BusinessException("Price is required");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Price must be greater than 0");
        }
        if (price.scale() > 2) {
            throw new BusinessException("Price must have at most 2 decimal places");
        }
        if (price.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new BusinessException("Price is too large");
        }
    }

    /** 空字符串统一转成 null，避免数据库里存一堆空串。 */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
