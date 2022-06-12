package jpabook.jpashoprestapi.repository;

import jpabook.jpashoprestapi.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepositoryDataJpa extends JpaRepository<Member, Long> {
    List<Member> findByName(String name);
}
