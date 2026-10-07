package com.enterprise.oms.cart.service;

import com.enterprise.oms.cart.domain.Cart;
import com.enterprise.oms.cart.domain.CartItem;
import com.enterprise.oms.cart.dto.AddCartItemRequest;
import com.enterprise.oms.cart.dto.CartItemDto;
import com.enterprise.oms.cart.dto.CartResponse;
import com.enterprise.oms.cart.repository.CartItemRepository;
import com.enterprise.oms.cart.repository.CartRepository;
import com.enterprise.oms.common.exception.ResourceNotFoundException;
import com.enterprise.oms.product.domain.Product;
import com.enterprise.oms.product.repository.ProductRepository;
import com.enterprise.oms.user.domain.User;
import com.enterprise.oms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    @Transactional
    public CartResponse getOrCreateCart(String userEmail) {
        User user = userService.getUserEntityByEmail(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCart(String userEmail, AddCartItemRequest request) {
        User user = userService.getUserEntityByEmail(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).items(new ArrayList<>()).build()));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + request.getProductId()));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        cart = cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse removeItemFromCart(String userEmail, UUID itemId) {
        User user = userService.getUserEntityByEmail(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        cartItemRepository.deleteById(itemId);

        cart = cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Transactional
    public void clearCart(String userEmail) {
        User user = userService.getUserEntityByEmail(userEmail);
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    public CartResponse mapToResponse(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        List<CartItemDto> itemDtos = new ArrayList<>();

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                BigDecimal itemTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(itemTotal);
                itemDtos.add(CartItemDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .unitPrice(item.getProduct().getPrice())
                        .quantity(item.getQuantity())
                        .itemTotal(itemTotal)
                        .build());
            }
        }

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemDtos)
                .totalAmount(total)
                .build();
    }
}
