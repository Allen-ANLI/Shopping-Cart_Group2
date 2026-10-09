package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sg.edu.nus.iss.shoppingcart.dto.ProductForm;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.service.AdminProductService;

/**
 * 管理员商品后台控制器 —— E 模块。
 *
 * <p>对应作业加分项：管理员可以新增、编辑、上下架商品。
 * 路由沿用分工文档表 3 的建议：{@code /admin/products}。</p>
 *
 * <p><b>权限：</b>不在这个类里写任何权限判断，全部交给
 * {@link sg.edu.nus.iss.shoppingcart.interceptor.AdminAccessInterceptor}。
 * 这样"能不能进后台"只有一个决策点，不会出现某个方法忘了判、
 * 或者换个 URL 就绕过的漏洞。</p>
 *
 * @author 蔡千一（Module E）
 * @author OpenAI Codex (new product visibility review)
 */
@Controller
public class AdminProductController {

    /** 管理员商品服务。 */
    private final AdminProductService adminProductService;

    /**
     * 构造方法，注入依赖。
     *
     * @param adminProductService 管理员商品服务
     */
    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    /**
     * 后台商品列表，含已下架商品。
     *
     * @param page 页码，从 1 开始
     * @param model 视图模型
     * @return 后台商品列表视图
     */
    @GetMapping("/admin/products")
    public String listProducts(@RequestParam(defaultValue = "1") int page, Model model) {
        int pageIndex = Math.max(page - 1, 0);
        Page<Product> productPage = adminProductService.findAllForAdmin(pageIndex);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalProducts", adminProductService.countAll());
        model.addAttribute("inactiveProducts", adminProductService.countInactive());

        return "admin/products";
    }

    /**
     * 展示新增商品表单。
     *
     * @param form  商品表单
     * @param model 视图模型
     * @return 商品表单视图
     */
    @GetMapping("/admin/products/new")
    public String showCreateForm(@ModelAttribute("productForm") ProductForm form,
                                 Model model) {
        model.addAttribute("pageTitle", "Add new product");
        model.addAttribute("formAction", "/admin/products");
        return "admin/product-form";
    }

    /**
     * 处理新增商品。
     *
     * <p>{@code @Valid} 触发服务端校验：名称为空、价格为零或负数、
     * 小数位超限都会被拒绝并回显在表单上。这是"页面隐藏按钮不能代替
     * 后端校验"的直接体现——绕过页面直接 POST 也一样被拦。</p>
     *
     * @param form   商品表单
     * @param result 校验结果
     * @param model  视图模型
     * @return 校验失败回表单；成功则重定向到列表
     */
    @PostMapping("/admin/products")
    public String createProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            // 校验失败时补回 action 和标题，否则页面会退化成默认文案。
            model.addAttribute("pageTitle", "Add new product");
            model.addAttribute("formAction", "/admin/products");
            return "admin/product-form";
        }

        adminProductService.create(form.getName(), form.getDescription(),
                form.getPrice(), form.getImageUrl(), form.isActive());

        return "redirect:/admin/products?created";
    }

    /**
     * 展示编辑商品表单。
     *
     * @param id    商品 ID
     * @param form  商品表单
     * @param model 视图模型
     * @return 商品表单视图
     */
    @GetMapping("/admin/products/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               @ModelAttribute("productForm") ProductForm form,
                               Model model) {
        // 商品不存在时 Service 抛异常，全局处理器转成 404。
        Product product = adminProductService.findById(id);

        form.setId(product.getId());
        form.setName(product.getName());
        form.setDescription(product.getDescription());
        form.setPrice(product.getPrice());
        form.setImageUrl(product.getImageUrl());
        form.setActive(product.isActive());

        model.addAttribute("pageTitle", "Edit product");
        model.addAttribute("formAction", "/admin/products/" + id);

        return "admin/product-form";
    }

    /**
     * 处理编辑商品。
     *
     * @param id     商品 ID
     * @param form   商品表单
     * @param result 校验结果
     * @param model  视图模型
     * @return 校验失败回表单；成功则重定向到列表
     */
    @PostMapping("/admin/products/{id}")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("productForm") ProductForm form,
                                BindingResult result,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit product");
            model.addAttribute("formAction", "/admin/products/" + id);
            return "admin/product-form";
        }

        adminProductService.update(id, form.getName(), form.getDescription(),
                form.getPrice(), form.getImageUrl(), form.isActive());

        return "redirect:/admin/products?updated";
    }

    /**
     * 切换商品上下架状态。
     *
     * <p>这是"下架而非删除"规则的入口。被订单引用过的商品也能下架，
     * 只是不能物理删除。</p>
     *
     * @param id 商品 ID
     * @return 重定向回后台列表
     */
    @PostMapping("/admin/products/{id}/toggle")
    public String toggleActive(@PathVariable Long id) {
        adminProductService.toggleActive(id);
        return "redirect:/admin/products?toggled";
    }

    /**
     * 删除从未被下单的商品。
     *
     * <p>被订单引用过的会抛 {@code BusinessException}，
     * 由全局处理器转成提示页，让管理员改用下架。</p>
     *
     * @param id 商品 ID
     * @return 重定向回后台列表
     */
    @PostMapping("/admin/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        adminProductService.deleteIfUnreferenced(id);
        return "redirect:/admin/products?deleted";
    }
}
