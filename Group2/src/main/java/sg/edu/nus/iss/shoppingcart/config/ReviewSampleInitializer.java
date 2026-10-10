package sg.edu.nus.iss.shoppingcart.config;

import jakarta.persistence.EntityManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import sg.edu.nus.iss.shoppingcart.entity.*;
import sg.edu.nus.iss.shoppingcart.repository.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Versioned, explicitly labelled simulation data. Customer purchases and reviews are preserved. */
@Component
@Order(20)
public class ReviewSampleInitializer implements CommandLineRunner {
    public static final String VERSION = "sample-purchase-reviews-v3";
    private static final int[] SALES = {5, 20, 65, 180, 550, 1400, 4200, 9600};
    private static final int[] REVIEW_COUNTS = {5, 8, 12, 20, 35, 50, 80, 120};
    private static final int[] RATING_TENTHS = {46, 48, 45, 50, 42, 47, 44, 49, 36, 18};
    private final ProductRepository products;
    private final UserRepository users;
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductReviewRepository reviews;
    private final CatalogSeedVersionRepository versions;
    private final PasswordEncoder passwords;
    private final EntityManager entityManager;

    public ReviewSampleInitializer(ProductRepository products, UserRepository users, OrderRepository orders,
            OrderItemRepository items, ProductReviewRepository reviews, CatalogSeedVersionRepository versions,
            PasswordEncoder passwords, EntityManager entityManager) {
        this.products = products; this.users = users; this.orders = orders; this.items = items;
        this.reviews = reviews; this.versions = versions; this.passwords = passwords; this.entityManager = entityManager;
    }

    @Override @Transactional
    public void run(String... args) {
        if (versions.existsById(VERSION)) return;
        // Replace only records explicitly marked as simulation data by the previous seed.
        // A new version never deletes a customer's unmarked review or payment.
        entityManager.createQuery("delete from ProductReview r where r.sample = true").executeUpdate();
        entityManager.createQuery("delete from OrderItem i where i.order.id in (select o.id from Order o where o.paymentMethod = 'SAMPLE_PURCHASE')").executeUpdate();
        entityManager.createQuery("delete from Order o where o.paymentMethod = 'SAMPLE_PURCHASE'").executeUpdate();
        entityManager.clear();

        User[] reviewers = new User[120];
        String[] names = {"Jamie", "Morgan", "Casey", "Alex", "Jordan", "Taylor", "Robin", "Avery", "Sam", "Riley", "Kai", "Eden", "Drew", "Blake", "Quinn", "Harper", "Noel", "Reese", "Lee", "Sky"};
        // One unpredictable encoded secret is sufficient: no seed identity has shared login credentials.
        String inaccessiblePassword = passwords.encode(UUID.randomUUID().toString());
        for (int n = 0; n < reviewers.length; n++) {
            reviewers[n] = users.save(new User("sample_" + UUID.randomUUID().toString().substring(0, 12),
                    inaccessiblePassword, names[n % names.length] + " " + (char) ('A' + n / names.length) + ".", null));
        }
        var existing = products.findAll().stream().collect(Collectors.toMap(Product::getName, p -> p, (first, second) -> first));
        List<Product> seed = CatalogSeedProducts.all();
        for (int index = 0; index < seed.size(); index++) {
            Product product = existing.get(seed.get(index).getName());
            if (product == null) continue; // Do not restore products removed by an administrator.
            int band = index % SALES.length;
            int count = REVIEW_COUNTS[band];
            int targetRating = RATING_TENTHS[(index * 3 + index / SALES.length) % RATING_TENTHS.length];
            int lowerRating = targetRating / 10;
            int higherCount = (int) Math.round(count * (targetRating % 10) / 10.0);
            for (int n = 0; n < count; n++) {
                // Every review belongs to a distinct confirmed order; quantity is 1..99.
                int quantity = SALES[band] / count + (n < SALES[band] % count ? 1 : 0);
                var order = new sg.edu.nus.iss.shoppingcart.entity.Order(reviewers[n],
                        product.getEffectivePrice().multiply(BigDecimal.valueOf(quantity)), UUID.randomUUID().toString());
                order.recordPayment("SAMPLE_PURCHASE", "SAMPLE-" + UUID.randomUUID());
                order.confirmReceipt();
                orders.save(order); items.save(new OrderItem(order, product, quantity));
                int rating = Math.min(5, lowerRating + (n < higherCount ? 1 : 0));
                ProductReview review = new ProductReview(); review.setUser(reviewers[n]); review.setProduct(product);
                review.setRating(rating); review.setComment(comment(product.getCategory(), rating, n)); review.setSample(true);
                review.setCreatedAt(LocalDateTime.now().minusDays(1L + (n * 7 + index * 3) % 365));
                reviews.save(review);
            }
        }
        // Historical simulations do not consume the current opening stock.
        versions.save(new CatalogSeedVersion(VERSION));
    }

    private String comment(String category, int rating, int index) {
        String subject = switch (category) {
            case "audio" -> "Used for calls and daily listening. 用于日常通话和听音。 ";
            case "typing" -> "Used for long typing sessions. 用于长时间输入。 ";
            case "displays" -> "Used as my office display. 用作日常办公屏幕。 ";
            case "storage" -> "Used for documents and project backups. 用于文档存储和项目备份。 ";
            case "charging" -> "Used to charge my everyday devices. 用于日常设备充电。 ";
            case "networking" -> "Used in my home office network. 用于居家办公网络。 ";
            case "printing" -> "Used to organise office paperwork. 用于整理办公文档。 ";
            case "mobile" -> "Used for work on the move. 用于移动办公。 ";
            case "computing" -> "Used for work and online meetings. 用于工作和线上会议。 ";
            default -> "Used at my study desk. 用于日常学习桌面。 ";
        };
        String[] comments = switch (rating) {
            case 1 -> new String[]{"Did not meet my expectations; reliability needs improvement. 未达到预期，稳定性有待改善。", "Difficult to get consistent results. I would not choose it again. 使用表现不稳定，暂时不会再次选购。", "The finish and day-to-day experience were disappointing. 做工和日常使用体验令人失望。", "Too many compromises for my workflow. 对我的工作流程来说，需要妥协的地方太多。"};
            case 2 -> new String[]{"Usable, but the setup took longer than expected. 勉强够用，设置比预期费时。", "Basic functions work; build quality could be better. 基础功能可用，但做工还需改进。", "A few recurring issues make this hard to recommend. 有些问题反复出现，难以推荐。", "Fair for occasional use, but not for demanding work. 偶尔使用尚可，不适合高强度工作。"};
            case 3 -> new String[]{"Does the essentials, with a few limitations. 基本功能够用，但有一些局限。", "Average overall. Useful at this price. 整体中规中矩，这个价格尚可。", "Fine for lighter tasks; the finish could improve. 轻量任务表现尚可，细节还有提升空间。", "Neither outstanding nor disappointing after a month. 用了一个月，表现平稳但没有惊喜。"};
            case 4 -> new String[]{"Reliable in daily use and straightforward to set up. 日常使用可靠，设置简单。", "Good value and a useful addition to my setup. 性价比不错，为工作设备带来实用补充。", "Works well. A little more refinement would make it excellent. 表现良好，细节再完善一些会更出色。", "Comfortable to use and easy to keep organised. 使用舒适，也方便整理。"};
            default -> new String[]{"Excellent experience from setup to everyday use. 从设置到日常使用，体验都很出色。", "A standout upgrade. I would happily buy it again. 升级效果明显，愿意再次选购。", "Consistently reliable and thoughtfully designed. 表现持续可靠，设计也很用心。", "Exactly what I needed; excellent quality for the price. 正好满足需求，同价位品质出色。"};
        };
        // Each sentence in the source pairs has one language. Never mix the two in a displayed review.
        String subjectEn=subject.substring(0,subject.indexOf(". ")+1);
        String subjectZh=subject.substring(subject.indexOf(". ")+2).trim();
        String paired=comments[(index / 4) % comments.length];
        int chinese=0; while(chinese<paired.length() && Character.UnicodeScript.of(paired.charAt(chinese))!=Character.UnicodeScript.HAN) chinese++;
        return index % 4 == 3 ? subjectZh + paired.substring(chinese) : subjectEn + " " + paired.substring(0,chinese).trim();
    }
}
