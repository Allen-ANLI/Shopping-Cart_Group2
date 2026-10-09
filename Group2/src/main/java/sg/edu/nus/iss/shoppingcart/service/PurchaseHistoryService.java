package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.dto.OrderDetailDto;
import sg.edu.nus.iss.shoppingcart.dto.OrderSummaryDto;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.entity.OrderItem;
import sg.edu.nus.iss.shoppingcart.exception.ResourceNotFoundException;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderQueryRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购买历史服务 —— E 模块。
 *
 * <p>负责 CA 核心功能 5 Purchase History：当前用户的订单列表与订单详情。
 * <b>本类只读，不做任何写入</b>，订单的创建由 D 的结账流程负责。</p>
 *
 * <p><b>安全边界。</b>每一个对外方法都以 {@code userId} 为第一约束，
 * 用户身份来自服务器 Session，客户端无法伪造。这是分工文档里
 * "用户不能通过修改订单 ID 查看别人订单" 的实现方式——
 * 校验下推到 SQL，越权查询返回空而不是返回别人的数据后再过滤。</p>
 *
 * @author 蔡千一（Module E）
 */
@Service
@Transactional(readOnly = true)
public class PurchaseHistoryService {

    /** 历史列表每页显示的订单数。 */
    public static final int PAGE_SIZE = 5;

    /** 订单读取接口。 */
    private final OrderQueryRepository orderRepository;

    /**订单明细读取接口。 */
    private final OrderItemQueryRepository orderItemRepository;

    /**
     * 构造方法，注入依赖。
     *
     * @param orderRepository     订单读取 Repository
     * @param orderItemRepository 明细读取 Repository
     */
    public PurchaseHistoryService(OrderQueryRepository orderRepository,
                                  OrderItemQueryRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * 分页查询指定用户的历史订单摘要。
     *
     * <p>{@code page} 用 0 起始的下标（Spring Data 的约定），
     * 由 Controller 负责把用户看到的 1 起始页码换算成下标。</p>
     *
     * @param userId 用户 ID
     * @param page   页码，从 0 开始；小于 0 时按 0 处理
     * @return 分页订单摘要，永不为 null
     */
    public Page<OrderSummaryDto> findHistory(Long userId, int page) {
        int safePage = Math.max(page, 0);
        return orderRepository.findOrderSummariesByUserId(userId, PageRequest.of(safePage, PAGE_SIZE));
    }

    /**
     * 按 ID 查订单详情（含明细），并校验归属。
     *
     * <p><b>这是防越权的核心。</b>用户把 URL 里的订单号改成别人的，
     * 查询会同时带上 {@code o.user.id = :userId}，查不到就返回"找不到"。
     * 越权访问和订单真的不存在返回<b>同一个</b>提示，
     * 避免通过报错差异探测别人的订单是否存在。</p>
     *
     * <p>明细用独立查询按 {@code order_id} 取，不去碰订单实体上的关联，
     * 这样 E 不需要修改 D 维护的 {@code Order} 类。</p>
     *
     * @param orderId 订单 ID
     * @param userId  当前登录用户 ID
     * @return 订单详情视图数据
     * @throws ResourceNotFoundException 不存在或不属于该用户时抛出
     */
    public OrderDetailDto findOrderDetailForUser(Long orderId, Long userId) {
        if (orderId == null || orderId <= 0) {
            throw ResourceNotFoundException.orderNotAccessible();
        }

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(ResourceNotFoundException::orderNotAccessible);

        List<OrderDetailDto.OrderItemLine> lines = orderItemRepository
                .findByOrder_IdOrderByIdAsc(orderId)
                .stream()
                .map(this::toLine)
                .toList();

        return new OrderDetailDto(order.getId(), order.getCreatedAt(),
                order.getTotalAmount(), lines);
    }

    /**
     * 统计指定用户的历史订单总数，供列表页顶部概览。
     *
     * @param userId 用户 ID
     * @return 订单数
     */
    public long countOrders(Long userId) {
        return orderRepository.countByUser_Id(userId);
    }

    /**
     * 统计指定用户的历史订单累计金额。
     *
     * @param userId 用户 ID
     * @return 累计金额，没有订单时为 0
     */
    public BigDecimal calculateLifetimeSpend(Long userId) {
        BigDecimal total = orderRepository.sumTotalAmountByUserId(userId);
        return total == null ? BigDecimal.ZERO : total;
    }

    /**
     * 把明细实体转成视图行。
     *
     * <p>只取快照字段，不碰 {@code item.getProduct()}——
     * 那是懒加载代理，事务关闭后访问会抛异常，
     * 而且读了也没意义：历史订单要显示的就是成交时的值。</p>
     *
     * @param item 明细实体
     * @return 视图行
     */
    private OrderDetailDto.OrderItemLine toLine(OrderItem item) {
        return new OrderDetailDto.OrderItemLine(
                item.getProductNameSnapshot(),
                item.getUnitPrice(),
                item.getQuantity());
    }
}