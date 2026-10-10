package sg.edu.nus.iss.shoppingcart;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.repository.AccountAvatarRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.AccountFormTokens;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AvatarDealsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountAvatarRepository avatars;
    @Autowired ProductRepository products;
    @Autowired EntityManager em;
    User owner, other;
    MockHttpSession session;
    String token;

    @BeforeEach void setup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = users.saveAndFlush(new User("photo" + suffix, "unused", "Photo", suffix + "@test.example"));
        other = users.saveAndFlush(new User("other" + suffix, "unused", "Other", "other" + suffix + "@test.example"));
        session = session(owner); token = AccountFormTokens.get(session);
    }
    @Test void avatarIsPersistedNormalizedAndIsolatedToItsOwner() throws Exception {
        mvc.perform(multipart("/api/account/avatar").file(picture(128, 128, Color.RED)).session(session)
                .param("accountFormToken", token).param("userId", other.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/api/account/avatar"));
        em.flush(); em.clear();
        assertThat(avatars.existsById(other.getId())).isFalse();
        var response = mvc.perform(get("/api/account/avatar").session(session(owner))).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg")).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff")).andReturn().getResponse();
        var decoded = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertThat(decoded.getWidth()).isEqualTo(512); assertThat(decoded.getHeight()).isEqualTo(512);
        assertThat(new Color(decoded.getRGB(250, 250)).getRed()).isGreaterThan(240);
        mvc.perform(get("/api/account/avatar").session(session(other))).andExpect(status().isNotFound());
        mvc.perform(get("/account").session(session(owner))).andExpect(status().isOk())
                .andExpect(model().attribute("hasAvatar", true)).andExpect(content().string(containsString("Frame your profile photo")));
        mvc.perform(get("/api/auth/session").session(session(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/api/account/avatar"));
        for (String page : new String[]{"/account", "/cart", "/orders", "/account/addresses"}) {
            mvc.perform(get(page).session(session(owner))).andExpect(status().isOk())
                    .andExpect(model().attribute("currentAvatarUrl", "/api/account/avatar"))
                    .andExpect(content().string(containsString("data-nav-avatar")));
        }
        mvc.perform(get("/api/auth/session").session(session(other))).andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }
    @Test void aSecondUploadReplacesOnlyTheCurrentAvatar() throws Exception {
        for (Color color : new Color[]{Color.RED, Color.BLUE}) {
            mvc.perform(multipart("/api/account/avatar").file(picture(100, 100, color)).session(session)
                    .param("accountFormToken", token)).andExpect(status().isOk());
            em.flush(); em.clear();
        }
        byte[] saved = avatars.findById(owner.getId()).orElseThrow().getImage();
        assertThat(new Color(ImageIO.read(new ByteArrayInputStream(saved)).getRGB(250, 250)).getBlue()).isGreaterThan(240);
    }
    @Test void uploadsRequireAuthenticationAndTheCurrentSessionsToken() throws Exception {
        mvc.perform(multipart("/api/account/avatar").file(picture(100,100,Color.RED)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/avatar")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/account/avatar").file(picture(100,100,Color.RED)).session(session)
                .param("accountFormToken", "wrong")).andExpect(status().isForbidden());
        assertThat(avatars.existsById(owner.getId())).isFalse();
    }
    @Test void malformedAndOversizedFilesAreRejectedWithLocalizedFeedback() throws Exception {
        for (byte[] data : new byte[][]{"<svg onload='alert(1)'></svg>".getBytes(), new byte[1048577]}) {
            mvc.perform(multipart("/api/account/avatar").file(new MockMultipartFile("avatar","photo.png","image/png",data))
                    .session(session).param("accountFormToken",token)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Please choose an image and save it using the crop tool."));
        }
        mvc.perform(multipart("/api/account/avatar").file(picture(64,128,Color.RED)).session(session)
                .cookie(new Cookie("store_lang","zh")).param("accountFormToken",token)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请使用圆形预览框选头像后再保存。"));
        assertThat(avatars.existsById(owner.getId())).isFalse();
    }
    @Test void invalidReplacementKeepsThePreviouslySavedPhoto() throws Exception {
        mvc.perform(multipart("/api/account/avatar").file(picture(64,64,Color.GREEN)).session(session)
                .param("accountFormToken",token)).andExpect(status().isOk());
        byte[] before = avatars.findById(owner.getId()).orElseThrow().getImage();
        mvc.perform(multipart("/api/account/avatar").file(picture(20,20,Color.RED)).session(session)
                .param("accountFormToken",token)).andExpect(status().isBadRequest());
        assertThat(avatars.findById(owner.getId()).orElseThrow().getImage()).isEqualTo(before);
    }
    @Test void dailyDealsTrackLargestCurrentDiscountAndExcludeUnavailableProducts() throws Exception {
        hideExistingProducts();
        Product small = product("Small discount",10,10,true), biggest = product("Biggest discount",75,10,true);
        product("Full price",0,10,true); product("Hidden",99,10,false); product("Sold out",95,0,true);
        mvc.perform(get("/api/deals")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.products.length()").value(2)).andExpect(jsonPath("$.products[0].id").value(biggest.getId()))
                .andExpect(jsonPath("$.products[0].effectivePrice").value(25));
        biggest.setDiscountPercent(5); products.saveAndFlush(biggest);
        mvc.perform(get("/api/deals")).andExpect(jsonPath("$.products[0].id").value(small.getId()))
                .andExpect(jsonPath("$.products[1].discountPercent").value(5));
    }
    @Test void dailyDealsHaveAnEmptyStateAndADedicatedPage() throws Exception {
        hideExistingProducts();
        mvc.perform(get("/api/deals")).andExpect(status().isOk()).andExpect(jsonPath("$.products").isEmpty());
        mvc.perform(get("/deals")).andExpect(status().isOk()).andExpect(forwardedUrl("/products/index.html"));
    }
    private void hideExistingProducts() { products.findAll().forEach(p -> { p.setActive(false); products.save(p); }); products.flush(); }
    private Product product(String name, int discount, int stock, boolean active) {
        Product p=new Product(); p.setName(name); p.setPrice(new BigDecimal("100.00"));
        p.setDiscountPercent(discount); p.setStockQuantity(stock); p.setActive(active); return products.saveAndFlush(p);
    }
    private MockHttpSession session(User user) { var s=new MockHttpSession(); s.setAttribute(LoginInterceptor.LOGIN_USER_ID,user.getId()); return s; }
    private MockMultipartFile picture(int width,int height,Color color) throws Exception {
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB); var g=image.createGraphics();
        g.setColor(color); g.fillRect(0,0,width,height); g.dispose(); var out=new ByteArrayOutputStream(); ImageIO.write(image,"png",out);
        return new MockMultipartFile("avatar","photo.png","image/png",out.toByteArray());
    }
}
