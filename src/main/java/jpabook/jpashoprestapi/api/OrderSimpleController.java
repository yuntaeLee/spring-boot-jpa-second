package jpabook.jpashoprestapi.api;

import jpabook.jpashoprestapi.domain.Address;
import jpabook.jpashoprestapi.domain.Order;
import jpabook.jpashoprestapi.domain.OrderStatus;
import jpabook.jpashoprestapi.repository.OrderRepository;
import jpabook.jpashoprestapi.repository.OrderSearch;
import jpabook.jpashoprestapi.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashoprestapi.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * XToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OrderSimpleController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress(); //Lazy 강제 초기화
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO로 변환 (fetch join 사용 X)
     * - 단점: 지연로딩으로 쿼리가 총 1 + N + N 실행
     * - order 조회 1번 (order 조회 결과 수가 N이 된다.)
     * - order -> member 지연 로딩 조회 N번
     * - order -> delivery 지연 로딩 조회 N번
     * - 지연 로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 경우는 쿼리 생략
     */
    @GetMapping("/api/v2/simple-orders")
    public Result<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<SimpleOrderDto> result = orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());

        return new Result<SimpleOrderDto>(result.size(), result);
    }

    /**
     * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용 O)
     * - fetch join으로 쿼리 1번 호출
     */
    @GetMapping("/api/v3/simple-orders")
    public Result<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());

        return new Result<SimpleOrderDto>(result.size(), result);
    }

    /**
     * V4. JPA에서 DTO로 바로 조회
     * - 쿼리 1번 호출
     * - 일반적인 SQL을 사용할 때 처럼 select 절에서 원하는 데이터만 선택해서 조회
     * - new 명령어를 사용해서 JPQL의 결과를 DTO로 즉시 반환
     * - SELECT 절에서 원하는 데이터를 직접 선택하므로 DB -> 애플리케이션 네트워크 용량 최적화 (생각보다 미비)
     * - Repository 재사용성 떨어짐, API 스펙에 맞춘 코드가 Repository에 들어가는 단점
     * <p>
     * - 정리
     * - 엔티티를 DTO로 변환하거나, DTO로 바로 조회하는 두가지 방법은 각각 장단점이 있다.
     * 둘중 상황에 따라서 더 나은 방법을 선택하면 된다. Entity로 조회하면 Repository 재사용성도
     * 좋고, 개발도 단순해진다. 따라서 권장하는 방법은 다음과 같다.
     * <p>
     * - 쿼리 방식 선택 권장 순서
     * 1. 우선 엔티티를 DTO로 변환하는 방법을 선택한다.
     * 2. 필요하면 패치 조인으로 성능을 최적화 한다. -> 대부분의 성능 이슈 해결
     * 3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
     * 4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서
     * SQL을 직접 사용한다.
     */
    @GetMapping("api/v4/simple-orders")
    public Result<OrderSimpleQueryDto> ordersV4() {
        List<OrderSimpleQueryDto> result = orderSimpleQueryRepository.findOrderDtos();
        return new Result<OrderSimpleQueryDto>(result.size(), result);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private List<T> data;
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); //LAZY 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); //LAZY 초기화
        }
    }
}
