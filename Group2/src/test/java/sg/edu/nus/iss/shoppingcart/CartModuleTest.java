package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.model.SessionCart;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 购物车数量、金额、会话隔离和表单令牌的行为测试。
 * @author Letian Xie
 */
class CartModuleTest {
    ProductRepository products;
    Product keyboard;
    MockHttpSession session;
    CartService cart;

    @BeforeEach void setup() {
        products=mock(ProductRepository.class);
        keyboard=new Product(); keyboard.setId(1L); keyboard.setName("Keyboard");
        keyboard.setPrice(new BigDecimal("50.00"));
        when(products.findById(1L)).thenReturn(Optional.of(keyboard));
        when(products.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(keyboard));
        cart=new CartService(products);
        session=new MockHttpSession(); session.setAttribute("loginUserId",1L);
    }
    @Test void mergesQuantitiesAndCalculatesDisplayAmount() {
        cart.addItem(session,1L,2); cart.addItem(session,1L,1);
        assertThat(cart.readForCheckout(session)).containsExactly(Map.entry(1L,3));
        assertThat(cart.calculateTotal(cart.getCartItems(session))).isEqualByComparingTo("150.00");
        assertThat(cart.countTotalQuantity(cart.getCartItems(session))).isEqualTo(3);
    }
    @Test void quantityZeroRemovesAndExplicitClearWorks() {
        cart.addItem(session,1L,1); cart.updateQuantity(session,1L,0);
        assertThat(cart.getCartItems(session)).isEmpty();
        cart.addItem(session,1L,2); cart.clearCart(session);
        assertThat(cart.getCartItems(session)).isEmpty();
        assertThatThrownBy(()->cart.readForCheckout(session)).isInstanceOf(BusinessException.class);
    }
    @Test void invalidQuantitiesDoNotModifyState() {
        assertThatThrownBy(()->cart.addItem(session,1L,0)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->cart.addItem(session,1L,-1)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->cart.addItem(session,1L,100)).isInstanceOf(BusinessException.class);
        cart.addItem(session,1L,99);
        assertThatThrownBy(()->cart.addItem(session,1L,1)).isInstanceOf(BusinessException.class);
        assertThat(cart.readForCheckout(session)).containsEntry(1L,99);
    }
    @Test void snapshotContainsOnlyIdsAndQuantities() {
        cart.addItem(session,1L,2);
        var snapshot=cart.readForCheckout(session); snapshot.put(1L,5);
        assertThat(cart.readForCheckout(session)).containsEntry(1L,2);
        SessionCart stored=(SessionCart)session.getAttribute("cart");
        assertThat(stored.getQuantities()).containsExactly(Map.entry(1L,2));
    }
    @Test void priceChangeUpdatesOnlyDisplayAndUnavailableItemsCanBeRemoved() {
        cart.addItem(session,1L,1); keyboard.setPrice(new BigDecimal("55.00"));
        assertThat(cart.calculateTotal(cart.getCartItems(session))).isEqualByComparingTo("55.00");
        keyboard.setActive(false);
        when(products.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());
        assertThat(cart.canCheckout(cart.getCartItems(session))).isFalse();
        assertThatThrownBy(()->cart.readForCheckout(session)).isInstanceOf(BusinessException.class);
        cart.removeItem(session,1L); assertThat(cart.getCartItems(session)).isEmpty();
    }
    @Test void differentSessionsAreIsolatedAndAccountChangeResetsState() {
        cart.addItem(session,1L,2); String old=cart.formToken(session);
        session.setAttribute(CartService.CHECKOUT_STATE_ATTRIBUTE,"old D state");
        MockHttpSession other=new MockHttpSession(); other.setAttribute("loginUserId",1L);
        assertThat(cart.getCartItems(other)).isEmpty();
        session.setAttribute("loginUserId",2L);
        assertThat(cart.getCartItems(session)).isEmpty();
        assertThat(session.getAttribute(CartService.CHECKOUT_STATE_ATTRIBUTE)).isNull();
        assertThat(cart.formToken(session)).isNotEqualTo(old);
    }
    @Test void formTokensRequireExactServerValue() {
        String token=cart.formToken(session);
        cart.validateFormToken(session,token);
        assertThatThrownBy(()->cart.validateFormToken(session,null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->cart.validateFormToken(session,"bad")).isInstanceOf(BusinessException.class);
    }
    @Test void loginIdentityMustBeLongAndMissingIdentityClearsOldCart() {
        cart.addItem(session,1L,1); session.removeAttribute("loginUserId");
        assertThatThrownBy(()->cart.getCartItems(session)).isInstanceOf(NotAuthenticatedException.class);
        assertThat(session.getAttribute("cart")).isNull();
        session.setAttribute("loginUserId","1");
        assertThatThrownBy(()->cart.requireUserId(session)).isInstanceOf(NotAuthenticatedException.class);
    }
}
