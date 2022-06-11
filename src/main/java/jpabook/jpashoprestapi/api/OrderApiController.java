package jpabook.jpashoprestapi.api;

import jpabook.jpashoprestapi.domain.Address;
import jpabook.jpashoprestapi.domain.Order;
import jpabook.jpashoprestapi.domain.OrderItem;
import jpabook.jpashoprestapi.domain.OrderStatus;
import jpabook.jpashoprestapi.repository.OrderRepository;
import jpabook.jpashoprestapi.repository.OrderSearch;
import jpabook.jpashoprestapi.repository.order.query.OrderQueryDto;
import jpabook.jpashoprestapi.repository.order.query.OrderQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 컬렉션 조회 최적화- OneToMany
 *
 * V1. 엔티티 직접 노출
 *  - 엔티티가 변하면 API 스펙이 변한다.
 *  - 트랜잭션 안에서 지연 로딩 필요
 *  - 양방향 연관관계 문제 -> @JsonIgnore
 *  - Hibernate5Module 모델 등록, Lazy=null 처리
 *
 * V2. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 X)
 *  - 트랜잭션 안에서 지연 로딩 필요
 *
 * V3. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 O)
 *  - fetch join으로 SQL이 1번만 실행
 *  - distinct: 1대다 조인으로 DB row가 증가하기 때문에 중복을 제거
 *  - 페이징 불가능
 *    - 페이징 시에는 N 부분을 포기해야 함 (대신 batch fetch size 옵션을 주면 N -> 쿼리로 변경 가능)
 *
 * V4. JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1 + N Query)
 *  - 페이징 가능
 * V5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
 *  - 페이징 가능
 * V6. JPA에서 DTO로 바로 조회, 플랫 데이터 (1 Query)
 *  - 페이징 불가능
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
//            order.getMember().getName(); //Lazy 강제 초기화
//            order.getDelivery().getAddress(); //Lazy 강제 초기화
//            List<OrderItem> orderItems = order.getOrderItems();
//            orderItems.stream().forEach(o -> o.getItem().getName()); //Lazy 강제 초기화

            Hibernate.initialize(order.getMember());
            Hibernate.initialize(order.getDelivery());
            order.getOrderItems()
                    .forEach(o -> Hibernate.initialize(o.getItem()));
        }

        return all;
    }

    @GetMapping("/api/v2/orders")
    public Result ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    @GetMapping("/api/v3/orders")
    public Result ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    /**
     * V3.1  엔티티를 조회해서 DTO로 변환 페이징 고려
     *  - ToOne 관계만 우선 모두 fetch join으로 최적화
     *  - 컬렉션 관계는 hibernate.default_batch_size, @BatchSize로 최적화
     *    - 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한꺼번에 설정한 size 만큼 IN 쿼리로 조
     */
    @GetMapping("/api/v3.1/orders")
    public Result ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    /**
     * V4. JPA에서 DTO로 바로 조회
     *  - Query: 루트 1번, 컬렉션 N번 실행
     *  - ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리
     *    - 이런 방식을 선택한 이유는 다음과 같다.
     *    - ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
     *    - ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
     *  - row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고
     *    ToMany관계는 최적화 하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다.
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @Getter
    @AllArgsConstructor
    static class Result<T> {
        private int orderCount;
        private T data;
    }

    @Getter
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {
        private String itemName; //상품 명
        private int orderPrice; //주문 가격
        private int itemCount; //주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            itemCount = orderItem.getCount();
        }
    }
}
